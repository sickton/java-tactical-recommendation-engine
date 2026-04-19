import os
import re
import json
import math
import random
import numpy as np
import joblib
import chromadb
from pathlib import Path
from fastapi import FastAPI
from openai import OpenAI
from dotenv import load_dotenv
from pydantic import BaseModel
from sklearn.preprocessing import LabelEncoder

load_dotenv()

# ── Pipeline artefacts (loaded once at startup) ───────────────────────────────
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

# ── Pitch coordinates (normalised 0–1, origin = bottom-left) ─────────────────
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

QUERY_TEMPLATES = {
    "dramatic": "dramatic tense high stakes moments with late goals or close scorelines in the season",
    "dominant": "dominant controlling performances with commanding leads",
    "comeback": "comeback moments where a team was losing and fought back",
    "pressure": "high pressure moments where a team was under intense defensive pressure",
    "turning_point": "turning points where momentum shifted and changed the game",
    "surprise": None
}

def build_query(team: str, query_type: str) -> str:
    if query_type == "surprise" or query_type not in QUERY_TEMPLATES:
        query_type = random.choice([q for q in QUERY_TEMPLATES if q != "surprise"])

    template = QUERY_TEMPLATES[query_type]
    return f"{template} involving {team} in the 2024/25 season"

def retrieve_moments(team: str, league: str, query: str, n_results: int) -> list:
    query_embedding = openai_client.embeddings.create(
        input=[query],
        model="text-embedding-3-small"
    ).data[0].embedding

    results = collection.query(
        query_embeddings=[query_embedding],
        n_results=200,
        where={
            "$or": [
                {"home_team": team},
                {"away_team": team}
            ]
        }
    )

    documents = results["documents"][0]
    metadatas = results["metadatas"][0]
    combined = list(zip(documents, metadatas))

    from collections import defaultdict
    matches = defaultdict(list)
    for doc, meta in combined:
        matches[meta["match_id"]].append((doc, meta))

    diverse_pool = [random.choice(moments) for moments in matches.values()]
    random.shuffle(diverse_pool)

    return diverse_pool[:n_results]

def generate_moments(team: str, sampled: list, n_results: int) -> list:
    context = "\n".join([doc for doc, meta in sampled])

    prompt = f"""
                You are a football storyteller for casual fans who are learning the game.

                Here are {n_results} match moments involving {team} from the 2024/25 season:

                {context}

                For each moment return a JSON array of objects with exactly these fields:
                - headline: a short punchy title for the moment (max 8 words)
                - minute: the minute of the match
                - match: home team vs away team
                - score: the scoreline at that moment
                - narrative: 2-3 sentences describing what is happening and why it matters in plain English, no jargon
                - concept: the football concept a casual fan can learn from this moment (e.g. "High Press", "Counter Attack")

                Return a JSON object with a single key "moments" containing an array of {n_results} objects. Each object must have exactly these keys: headline, minute, match, score, narrative, concept.
            """

    response = openai_client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": prompt}],
        temperature=0.8,
        response_format={"type": "json_object"}
    )

    import json
    content = response.choices[0].message.content
    parsed = json.loads(content)
    return parsed.get("moments", list(parsed.values())[0])

@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/story")
def get_story(request: StoryRequest):
    n_results = 5 if request.mode == "simple" else 10

    query = build_query(request.team, request.query_type)
    sampled = retrieve_moments(request.team, request.league, query, n_results)
    moments = generate_moments(request.team, sampled, n_results)

    return {"team": request.team, "query_type": request.query_type, "moments": moments}

class ExplainRequest(BaseModel):
    headline: str
    match: str
    minute: int
    score: str
    concept: str
    team: str

@app.post("/explain")
def explain_moment(request: ExplainRequest):
    prompt = f"""
You are a football teacher explaining the game to someone who is watching 
for the first time.

A key moment just happened in a match:
- Match: {request.match}
- Minute: {request.minute}
- Score: {request.score}
- Headline: {request.headline}
- Football concept: {request.concept}
- Team being followed: {request.team}

Write a 4-5 sentence explanation of:
1. What is physically happening on the pitch right now
2. Why this moment matters in the context of the match
3. What the "{request.concept}" concept means in plain English
4. What the team should do next and why

No jargon. No stats. Write like you are explaining to a friend 
who loves drama but does not know football.
"""

    response = openai_client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": prompt}],
        temperature=0.7
    )

    explanation = response.choices[0].message.content

    return {
        "headline": request.headline,
        "match": request.match,
        "minute": request.minute,
        "concept": request.concept,
        "explanation": explanation
    }


# ── Network helpers ───────────────────────────────────────────────────────────

def get_game_phase(minute: int) -> str:
    if minute <= 15:  return "EARLY_MINUTES"
    elif minute <= 44: return "CLOSING_HALF"
    elif minute <= 50: return "HALF_TIME"
    elif minute <= 60: return "BUILD_PHASE"
    elif minute <= 70: return "TENSION_TIME"
    elif minute <= 87: return "LATE_GAME"
    else:              return "STOPPAGE_TIME"

def get_pitch_zone(norm_x: float) -> str:
    x = norm_x * 120
    if x < 40:   return "defensive_third"
    elif x < 80: return "middle_third"
    else:        return "attacking_third"

def get_pass_direction(angle: float) -> str:
    if -math.pi / 4 < angle < math.pi / 4:
        return "forward"
    elif angle > 3 * math.pi / 4 or angle < -3 * math.pi / 4:
        return "backward"
    else:
        return "sideways"

def encode_value(col: str, val: str) -> int:
    classes = list(encoders[col].classes_)
    return classes.index(val) if val in classes else 0

# How much a pass direction contributes to escaping a press.
# Backward passes are penalised because completing a pass is not the same
# as escaping pressure — the model was trained on pass completion, not progression.
_DIRECTION_WEIGHT: dict[str, float] = {
    "forward":  1.0,
    "sideways": 0.75,
    "backward": 0.35,
}

# Extra multiplier applied to backward passes depending on where the ball is.
# Going backward from midfield or the attacking third undoes the escape entirely.
_ZONE_BACKWARD_PENALTY: dict[str, float] = {
    "defensive_third":  0.9,
    "middle_third":     0.5,
    "attacking_third":  0.2,
}

def score_edge(from_pos: str, to_pos: str, formation: str, game_phase: str,
               from_coords: tuple, to_coords: tuple) -> float:
    dx = (to_coords[0] - from_coords[0]) * 120
    dy = (to_coords[1] - from_coords[1]) * 80
    pass_length    = math.sqrt(dx ** 2 + dy ** 2)
    pass_angle     = math.atan2(dy, dx)
    pitch_x        = from_coords[0] * 120
    pitch_y        = from_coords[1] * 80
    pitch_zone     = get_pitch_zone(from_coords[0])
    pass_direction = get_pass_direction(pass_angle)

    features = np.array([[
        encode_value("from_pos",       from_pos),
        encode_value("to_pos",         to_pos),
        encode_value("formation",      formation),
        encode_value("game_phase",     game_phase),
        encode_value("pitch_zone",     pitch_zone),
        encode_value("pass_direction", pass_direction),
        pass_length,
        pass_angle,
        pitch_x,
        pitch_y,
    ]])

    proba = escape_model.predict_proba(features)[0][1]

    # Apply progression weighting so the optimal path favours moving the ball
    # forward rather than recycling possession backwards under pressure.
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


# ── /network endpoint ─────────────────────────────────────────────────────────

class NetworkRequest(BaseModel):
    escaping_team:      str
    escaping_formation: str
    pressing_team:      str
    pressing_formation: str
    league:             str
    minute:             int
    home_goals:         int
    away_goals:         int

@app.post("/network")
def get_network(request: NetworkRequest):
    game_phase = get_game_phase(request.minute)

    escape_graph   = build_graph(request.escaping_formation, game_phase, score_edges=True)
    pressing_graph = build_graph(request.pressing_formation, game_phase, score_edges=False)

    return {
        "escape_graph":        escape_graph,
        "pressing_graph":      pressing_graph,
        "game_phase":          game_phase,
        "escaping_formation":  request.escaping_formation,
        "pressing_formation":  request.pressing_formation,
    }