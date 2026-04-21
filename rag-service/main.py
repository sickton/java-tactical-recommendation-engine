import json
import math
import os
import random
import re
from collections import defaultdict
from pathlib import Path

import chromadb
import joblib
import numpy as np
from dotenv import load_dotenv
from fastapi import FastAPI
from openai import OpenAI
from pydantic import BaseModel
from sklearn.preprocessing import LabelEncoder

load_dotenv()

# Pipeline artefacts (loaded once at startup)
_PIPELINE_DIR = Path(__file__).parent / "pipeline" / "output"

escape_model = joblib.load(_PIPELINE_DIR / "escape_model.joblib")

with open(_PIPELINE_DIR / "escape_model_meta.json") as f:
    _meta = json.load(f)

THRESHOLD = _meta["threshold"]

encoders: dict[str, LabelEncoder] = {}
for col, classes in _meta["encoders"].items():
    le = LabelEncoder()
    le.classes_ = np.array(classes)
    encoders[col] = le

matrices: dict[str, dict] = {}
for path in (_PIPELINE_DIR / "matrices").glob("*.json"):
    with open(path) as f:
        data = json.load(f)
    matrices[data["formation"]] = data["matrix"]

# Pitch coordinates (normalised 0-1, origin = bottom-left)
# x: 0 = own goal end, 1 = opponent goal end
# y: 0 = left touchline, 1 = right touchline
POSITION_COORDS: dict[str, tuple[float, float]] = {
    "GK":  (0.05, 0.50),
    "CB":  (0.20, 0.50),
    "RB":  (0.22, 0.82),
    "LB":  (0.22, 0.18),
    "RWB": (0.40, 0.88),
    "LWB": (0.40, 0.12),
    "CDM": (0.38, 0.50),
    "CM":  (0.50, 0.50),
    "RM":  (0.50, 0.82),
    "LM":  (0.50, 0.18),
    "CAM": (0.62, 0.50),
    "RW":  (0.72, 0.82),
    "LW":  (0.72, 0.18),
    "CF":  (0.80, 0.65),
    "ST":  (0.85, 0.50),
}

FALLBACK_FORMATION = "F_4_3_3"

app = FastAPI()
openai_client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
chroma_client = chromadb.PersistentClient(path="./chroma_store")
collection = chroma_client.get_or_create_collection(
    name="match_moments",
    metadata={"hnsw:space": "cosine"}
)


class StoryRequest(BaseModel):
    team: str
    league: str
    mode: str
    query_type: str


class ExplainRequest(BaseModel):
    headline: str
    match: str
    minute: int
    score: str
    concept: str
    team: str


QUERY_TEMPLATES = {
    "dramatic": "late tense moments with a close score and emotional swing",
    "dominant": "controlled commanding spells where one team imposed itself",
    "comeback": "moments where a team was behind or recovering momentum",
    "pressure": "moments where a team was under pressure and needed composure",
    "turning_point": "moments where the match suddenly changed direction",
    "surprise": None,
}

CONCEPT_HINTS = {
    "dramatic": "Game Management",
    "dominant": "Control",
    "comeback": "Momentum Shift",
    "pressure": "Press Resistance",
    "turning_point": "Momentum Shift",
    "surprise": "Tactical Awareness",
}


def normalize_query_type(query_type: str) -> str:
    if query_type not in QUERY_TEMPLATES or query_type == "surprise":
        return random.choice([q for q in QUERY_TEMPLATES if q != "surprise"])
    return query_type


def build_query(team: str, query_type: str) -> str:
    template = QUERY_TEMPLATES[query_type]
    return (
        f"{template}. Focus on {team} in the 2024/25 season. "
        f"Prefer moments that are tactically teachable and emotionally clear."
    )


def parse_match_teams(match: str) -> tuple[str | None, str | None]:
    parts = re.split(r"\s+vs\s+", match.strip(), flags=re.IGNORECASE)
    if len(parts) != 2:
        return None, None
    return parts[0].strip(), parts[1].strip()


def get_goal_diff_for_team(meta: dict, team: str) -> int:
    if team == meta.get("home_team"):
        return int(meta.get("home_goals", 0)) - int(meta.get("away_goals", 0))
    return int(meta.get("away_goals", 0)) - int(meta.get("home_goals", 0))


def team_state(meta: dict, team: str) -> str:
    diff = get_goal_diff_for_team(meta, team)
    if diff > 0:
        return "winning"
    if diff < 0:
        return "losing"
    return "drawing"


def score_string(meta: dict) -> str:
    return f"{meta['home_team']} {meta['home_goals']}-{meta['away_goals']} {meta['away_team']}"


def display_match_label(meta: dict, team: str) -> str:
    if meta.get("home_team") == team:
        opponent = str(meta.get("away_team"))
        return f"{opponent} at home"
    opponent = str(meta.get("home_team"))
    return f"{opponent} away"


def metadata_theme_fit(meta: dict, team: str, query_type: str) -> float:
    minute = int(meta.get("minute", 0))
    goal_diff = abs(int(meta.get("home_goals", 0)) - int(meta.get("away_goals", 0)))
    state = team_state(meta, team)
    turning_point = int(meta.get("turning_point", 0))
    explicit_themes = f"{meta.get('home_themes', '')},{meta.get('away_themes', '')}"

    score = 0.0
    if query_type in explicit_themes:
        score += 0.55

    if query_type == "dramatic":
        if minute >= 70:
            score += 0.25
        if goal_diff <= 1:
            score += 0.2
    elif query_type == "dominant":
        if state == "winning":
            score += 0.35
        if goal_diff >= 2:
            score += 0.3
        if minute >= 45:
            score += 0.1
    elif query_type == "comeback":
        if state != "winning":
            score += 0.25
        if turning_point:
            score += 0.25
        if minute >= 55:
            score += 0.1
    elif query_type == "pressure":
        if state != "winning":
            score += 0.3
        if goal_diff <= 1:
            score += 0.2
        if minute >= 60:
            score += 0.2
    elif query_type == "turning_point":
        if turning_point:
            score += 0.45
        if goal_diff <= 1:
            score += 0.15

    return min(score, 1.0)


def build_where_clause(team: str, league: str):
    return {
        "$and": [
            {"league": league},
            {
                "$or": [
                    {"home_team": team},
                    {"away_team": team}
                ]
            }
        ]
    }


def retrieve_candidates(team: str, league: str, query_type: str, candidate_count: int = 60) -> list[dict]:
    normalized_type = normalize_query_type(query_type)
    query_embedding = openai_client.embeddings.create(
        input=[build_query(team, normalized_type)],
        model="text-embedding-3-small"
    ).data[0].embedding

    results = collection.query(
        query_embeddings=[query_embedding],
        n_results=candidate_count,
        where=build_where_clause(team, league)
    )

    documents = results.get("documents", [[]])[0]
    metadatas = results.get("metadatas", [[]])[0]
    distances = results.get("distances", [[]])[0]

    candidates = []
    for idx, meta in enumerate(metadatas):
        if not meta:
            continue
        distance = distances[idx] if idx < len(distances) else 1.0
        semantic_score = max(0.0, 1.0 - float(distance))
        theme_fit = metadata_theme_fit(meta, team, normalized_type)
        puzzle_score = float(
            meta.get("home_puzzle_score", 0.4) if meta.get("home_team") == team
            else meta.get("away_puzzle_score", 0.4)
        )
        recency_interest = min(int(meta.get("minute", 0)) / 90.0, 1.0)
        final_score = (
            0.45 * semantic_score
            + 0.30 * theme_fit
            + 0.15 * puzzle_score
            + 0.10 * recency_interest
        )
        candidates.append({
            "document": documents[idx],
            "metadata": meta,
            "semantic_score": round(semantic_score, 4),
            "theme_fit": round(theme_fit, 4),
            "puzzle_score": round(puzzle_score, 4),
            "final_score": round(final_score, 4),
        })

    return candidates


def context_bucket(meta: dict, team: str) -> str:
    minute = int(meta.get("minute", 0))
    phase = str(meta.get("game_phase", "unknown"))
    state = team_state(meta, team)

    if minute <= 30:
        time_band = "early"
    elif minute <= 70:
        time_band = "mid"
    else:
        time_band = "late"

    return f"{state}:{time_band}:{phase}"


def cluster_candidates(candidates: list[dict]) -> list[dict]:
    clusters: dict[tuple[str, int], dict] = {}

    for candidate in sorted(candidates, key=lambda c: c["final_score"], reverse=True):
        meta = candidate["metadata"]
        match_id = str(meta.get("match_id"))
        minute = int(meta.get("minute", 0))
        cluster_key = (match_id, minute // 4)

        existing = clusters.get(cluster_key)
        if existing is None or candidate["final_score"] > existing["final_score"]:
            clusters[cluster_key] = candidate

    return sorted(clusters.values(), key=lambda c: c["final_score"], reverse=True)


def diversify_candidates(candidates: list[dict], team: str, limit: int) -> list[dict]:
    clustered = cluster_candidates(candidates)
    selected = []
    per_match = defaultdict(int)
    per_bucket = defaultdict(int)

    if limit <= 5:
        max_per_match = 1
    elif limit <= 10:
        max_per_match = 2
    else:
        max_per_match = 3

    for candidate in clustered:
        meta = candidate["metadata"]
        match_id = str(meta.get("match_id"))
        bucket = context_bucket(meta, team)

        if per_match[match_id] >= max_per_match:
            continue

        penalty = 0.22 * per_match[match_id] + 0.10 * per_bucket[bucket]
        adjusted = candidate["final_score"] - penalty
        if adjusted <= 0:
            continue

        candidate = dict(candidate)
        candidate["adjusted_score"] = round(adjusted, 4)
        candidate["context_bucket"] = bucket
        selected.append(candidate)
        per_match[match_id] += 1
        per_bucket[bucket] += 1

        if len(selected) >= limit:
            break

    return selected


def build_reason(candidate: dict, team: str, query_type: str) -> str:
    meta = candidate["metadata"]
    bits = [f"{team} are {team_state(meta, team)}"]
    if abs(int(meta.get("home_goals", 0)) - int(meta.get("away_goals", 0))) <= 1:
        bits.append("the score is tight")
    if int(meta.get("turning_point", 0)) == 1:
        bits.append("the match state is shifting")
    if int(meta.get("minute", 0)) >= 70:
        bits.append("the moment arrives late")
    bits.append(f"it fits the {query_type.replace('_', ' ')} theme")
    return "; ".join(bits)


def build_story_candidates(team: str, league: str, query_type: str, n_results: int) -> list[dict]:
    normalized_type = normalize_query_type(query_type)
    candidates = retrieve_candidates(team, league, normalized_type, candidate_count=80)
    selected = diversify_candidates(candidates, team, max(n_results * 2, 8))

    story_candidates = []
    for candidate in selected:
        meta = candidate["metadata"]
        story_candidates.append({
            "match": display_match_label(meta, team),
            "fixture": f"{meta['home_team']} vs {meta['away_team']}",
            "minute": int(meta["minute"]),
            "score": score_string(meta),
            "game_phase": meta.get("game_phase", "unknown"),
            "reason": build_reason(candidate, team, normalized_type),
            "concept_hint": CONCEPT_HINTS.get(normalized_type, "Tactical Awareness"),
            "document": candidate["document"],
            "retrieval_score": candidate.get("adjusted_score", candidate["final_score"]),
            "match_id": str(meta["match_id"]),
        })

    return story_candidates[:max(n_results * 2, 8)]


def safe_json_loads(content: str) -> dict:
    try:
        return json.loads(content)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", content, re.DOTALL)
        if not match:
            raise
        return json.loads(match.group(0))


def generate_moments(team: str, query_type: str, candidates: list[dict], n_results: int) -> list:
    normalized_type = normalize_query_type(query_type)
    payload = json.dumps(candidates, indent=2)

    prompt = f"""
You are a football storyteller for casual fans who are learning the game.

You are given retrieved candidate moments for {team} under the "{normalized_type}" theme.
Each candidate already includes grounded match data and a short reason for retrieval.

Candidate moments:
{payload}

Select the best {n_results} candidates. Keep the exact match, minute, and score from the candidate data.
Do not invent facts that are not supported by the candidates.

Return JSON with a single key "moments" containing an array of exactly {n_results} objects.
Each object must contain exactly these keys:
- headline: short punchy title, max 8 words
- minute: integer minute from the candidate
- match: exact match string from the candidate
- score: exact score string from the candidate
- narrative: 2-3 sentences in plain English, focused on why the moment matters
- concept: a football concept a casual fan can learn from this moment

Favor diversity across matches and avoid repeating the same storyline.
"""

    response = openai_client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": prompt}],
        temperature=0.45,
        response_format={"type": "json_object"}
    )

    content = response.choices[0].message.content
    parsed = safe_json_loads(content)
    moments = parsed.get("moments", [])

    if not moments:
        fallback = []
        for candidate in candidates[:n_results]:
            fallback.append({
                "headline": f"{team} face a key spell",
                "minute": candidate["minute"],
                "match": candidate["match"],
                "score": candidate["score"],
                "narrative": (
                    f"This phase matters because {candidate['reason']}. "
                    f"It is a useful snapshot for understanding how the match is unfolding."
                ),
                "concept": candidate["concept_hint"],
            })
        return fallback

    return enforce_story_constraints(moments, candidates, n_results)


def enforce_story_constraints(moments: list[dict], candidates: list[dict], n_results: int) -> list[dict]:
    candidate_by_key = {}
    fallback_by_match = {}
    for candidate in candidates:
        key = (candidate["match"], int(candidate["minute"]), candidate["score"])
        candidate_by_key[key] = candidate
        fallback_by_match.setdefault(candidate["match_id"], []).append(candidate)

    if n_results <= 5:
        max_per_match = 1
    elif n_results <= 10:
        max_per_match = 2
    else:
        max_per_match = 3

    selected = []
    seen_keys = set()
    match_counts = defaultdict(int)

    for moment in moments:
        key = (moment.get("match"), int(moment.get("minute", 0)), moment.get("score"))
        candidate = candidate_by_key.get(key)
        if not candidate:
            continue
        match_id = candidate["match_id"]
        if match_counts[match_id] >= max_per_match:
            continue
        if key in seen_keys:
            continue
        selected.append(moment)
        seen_keys.add(key)
        match_counts[match_id] += 1
        if len(selected) >= n_results:
            return selected

    ranked_candidates = sorted(candidates, key=lambda c: c["retrieval_score"], reverse=True)

    def try_fill(allow_match_overflow: bool) -> None:
        for candidate in ranked_candidates:
            if len(selected) >= n_results:
                return
            match_id = candidate["match_id"]
            key = (candidate["match"], int(candidate["minute"]), candidate["score"])
            if key in seen_keys:
                continue
            if not allow_match_overflow and match_counts[match_id] >= max_per_match:
                continue
            selected.append({
                "headline": f"{candidate['fixture']} reaches a key phase",
                "minute": candidate["minute"],
                "match": candidate["match"],
                "score": candidate["score"],
                "narrative": (
                    f"This moment was selected because {candidate['reason']}. "
                    f"It stands out as a strong example of this match situation."
                ),
                "concept": candidate["concept_hint"],
            })
            seen_keys.add(key)
            match_counts[match_id] += 1

    try_fill(allow_match_overflow=False)
    if len(selected) < n_results:
        try_fill(allow_match_overflow=True)

    return selected[:n_results]


def lookup_context(match: str, minute: int) -> list[dict]:
    home_team, away_team = parse_match_teams(match)
    if not home_team or not away_team:
        return []

    rows = collection.get(
        where={
            "$and": [
                {"home_team": home_team},
                {"away_team": away_team}
            ]
        },
        include=["documents", "metadatas"]
    )

    metadatas = rows.get("metadatas", [])
    documents = rows.get("documents", [])
    context_rows = []
    for doc, meta in zip(documents, metadatas):
        if not meta:
            continue
        if abs(int(meta.get("minute", 0)) - minute) <= 3:
            context_rows.append({
                "document": doc,
                "metadata": meta
            })

    context_rows.sort(key=lambda row: abs(int(row["metadata"].get("minute", 0)) - minute))
    return context_rows[:5]


def build_explain_context(request: ExplainRequest) -> str:
    nearby = lookup_context(request.match, request.minute)
    if not nearby:
        return "No nearby retrieved match context was available."

    lines = []
    for row in nearby:
        meta = row["metadata"]
        lines.append(
            f"- {meta['minute']}' {score_string(meta)} ({meta.get('game_phase', 'unknown')}): {row['document']}"
        )
    return "\n".join(lines)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/story")
def get_story(request: StoryRequest):
    n_results = 5 if request.mode == "simple" else 10
    candidates = build_story_candidates(request.team, request.league, request.query_type, n_results)
    moments = generate_moments(request.team, request.query_type, candidates, n_results)

    return {"team": request.team, "query_type": request.query_type, "moments": moments}


@app.post("/explain")
def explain_moment(request: ExplainRequest):
    retrieved_context = build_explain_context(request)

    prompt = f"""
You are a football teacher explaining the game to someone who is watching for the first time.

A key moment just happened in a match:
- Match: {request.match}
- Minute: {request.minute}
- Score: {request.score}
- Headline: {request.headline}
- Football concept: {request.concept}
- Team being followed: {request.team}

Retrieved match context around the same minute:
{retrieved_context}

Write a 4-5 sentence explanation of:
1. What is physically happening on the pitch right now
2. Why this moment matters in the context of the match
3. What the "{request.concept}" concept means in plain English
4. What the team should do next and why

Use the retrieved context when it is relevant.
No jargon. No stats-dumping. Do not invent scorelines or match events that are not supported.
Write like you are explaining to a friend who loves drama but does not know football.
"""

    response = openai_client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": prompt}],
        temperature=0.4
    )

    explanation = response.choices[0].message.content

    return {
        "headline": request.headline,
        "match": request.match,
        "minute": request.minute,
        "concept": request.concept,
        "explanation": explanation
    }


# Network helpers
def get_game_phase(minute: int) -> str:
    if minute <= 15:
        return "EARLY_MINUTES"
    if minute <= 44:
        return "CLOSING_HALF"
    if minute <= 50:
        return "HALF_TIME"
    if minute <= 60:
        return "BUILD_PHASE"
    if minute <= 70:
        return "TENSION_TIME"
    if minute <= 87:
        return "LATE_GAME"
    return "STOPPAGE_TIME"


def get_pitch_zone(norm_x: float) -> str:
    x = norm_x * 120
    if x < 40:
        return "defensive_third"
    if x < 80:
        return "middle_third"
    return "attacking_third"


def get_pass_direction(angle: float) -> str:
    if -math.pi / 4 < angle < math.pi / 4:
        return "forward"
    if angle > 3 * math.pi / 4 or angle < -3 * math.pi / 4:
        return "backward"
    return "sideways"


def encode_value(col: str, val: str) -> int:
    classes = list(encoders[col].classes_)
    return classes.index(val) if val in classes else 0


_DIRECTION_WEIGHT: dict[str, float] = {
    "forward": 1.0,
    "sideways": 0.75,
    "backward": 0.35,
}

_ZONE_BACKWARD_PENALTY: dict[str, float] = {
    "defensive_third": 0.9,
    "middle_third": 0.5,
    "attacking_third": 0.2,
}


def score_edge(from_pos: str, to_pos: str, formation: str, game_phase: str,
               from_coords: tuple, to_coords: tuple) -> float:
    dx = (to_coords[0] - from_coords[0]) * 120
    dy = (to_coords[1] - from_coords[1]) * 80
    pass_length = math.sqrt(dx ** 2 + dy ** 2)
    pass_angle = math.atan2(dy, dx)
    pitch_x = from_coords[0] * 120
    pitch_y = from_coords[1] * 80
    pitch_zone = get_pitch_zone(from_coords[0])
    pass_direction = get_pass_direction(pass_angle)

    features = np.array([[
        encode_value("from_pos", from_pos),
        encode_value("to_pos", to_pos),
        encode_value("formation", formation),
        encode_value("game_phase", game_phase),
        encode_value("pitch_zone", pitch_zone),
        encode_value("pass_direction", pass_direction),
        pass_length,
        pass_angle,
        pitch_x,
        pitch_y,
    ]])

    proba = escape_model.predict_proba(features)[0][1]

    direction_w = _DIRECTION_WEIGHT[pass_direction]
    if pass_direction == "backward":
        direction_w *= _ZONE_BACKWARD_PENALTY[pitch_zone]

    return round(float(proba * direction_w), 4)


def build_graph(formation: str, game_phase: str, score_edges: bool) -> dict:
    matrix = matrices.get(formation) or matrices.get(FALLBACK_FORMATION, {})

    positions_used: set[str] = set()
    for from_pos, targets in matrix.items():
        positions_used.add(from_pos)
        positions_used.update(targets.keys())

    nodes = [
        {"id": pos, "x": POSITION_COORDS[pos][0], "y": POSITION_COORDS[pos][1]}
        for pos in positions_used if pos in POSITION_COORDS
    ]

    edges = []
    for from_pos, targets in matrix.items():
        from_coords = POSITION_COORDS.get(from_pos)
        if not from_coords:
            continue
        for to_pos, weight in targets.items():
            if weight < 0.05:
                continue
            to_coords = POSITION_COORDS.get(to_pos)
            if not to_coords:
                continue
            edge: dict = {"from": from_pos, "to": to_pos, "weight": round(weight, 4)}
            if score_edges:
                edge["escape_prob"] = score_edge(
                    from_pos, to_pos, formation, game_phase, from_coords, to_coords
                )
            edges.append(edge)

    return {"nodes": nodes, "edges": edges}


class NetworkRequest(BaseModel):
    escaping_team: str
    escaping_formation: str
    pressing_team: str
    pressing_formation: str
    league: str
    minute: int
    home_goals: int
    away_goals: int


@app.post("/network")
def get_network(request: NetworkRequest):
    game_phase = get_game_phase(request.minute)

    escape_graph = build_graph(request.escaping_formation, game_phase, score_edges=True)
    pressing_graph = build_graph(request.pressing_formation, game_phase, score_edges=False)

    return {
        "escape_graph": escape_graph,
        "pressing_graph": pressing_graph,
        "game_phase": game_phase,
        "escaping_formation": request.escaping_formation,
        "pressing_formation": request.pressing_formation,
    }
