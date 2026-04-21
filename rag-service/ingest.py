import csv
import os
import sys
from collections import defaultdict

import chromadb
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    print("ERROR: OPENAI API KEY not set in the env")
    sys.exit(1)

openai_client = OpenAI(api_key=OPENAI_API_KEY)
chroma_client = chromadb.PersistentClient(path="./chroma_store")
collection = chroma_client.get_or_create_collection(
    name="match_moments",
    metadata={"hnsw:space": "cosine"}
)

BASE_DIR = os.path.join(
    os.path.dirname(__file__),
    "..", "JGaffer", "src", "main", "resources"
)

LEAGUES = [
    {
        "name": "PL",
        "match_csv": os.path.join(BASE_DIR, "PremierLeague", "MatchMinuteContext.csv"),
        "squad_csv": os.path.join(BASE_DIR, "PremierLeague", "SquadInformation.csv"),
    },
    {
        "name": "SA",
        "match_csv": os.path.join(BASE_DIR, "SerieA", "MatchMinuteContext.csv"),
        "squad_csv": os.path.join(BASE_DIR, "SerieA", "SquadInformation.csv"),
    },
]

WINDOW_RADIUS = 2


def load_squad_info(filepath):
    squad = {}
    with open(filepath, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            squad[row["team_name"]] = row
    return squad


def get_game_phase(minute):
    m = int(minute)
    if m <= 15:
        return "early game"
    if m <= 44:
        return "first half"
    if m <= 50:
        return "half time"
    if m <= 60:
        return "build phase"
    if m <= 70:
        return "tension time"
    if m <= 87:
        return "late game"
    return "stoppage time"


def scoreline_text(home, away, home_goals, away_goals):
    return f"{home} {home_goals}-{away_goals} {away}"


def score_state_for_team(team, home, away, home_goals, away_goals):
    if team == home:
        diff = home_goals - away_goals
    else:
        diff = away_goals - home_goals

    if diff > 0:
        return "winning"
    if diff < 0:
        return "losing"
    return "drawing"


def describe_recent_change(prev_row, row, team, home, away):
    if prev_row is None:
        return "no immediate score change"

    prev_home = int(prev_row["homeGoals"])
    prev_away = int(prev_row["awayGoals"])
    home_goals = int(row["homeGoals"])
    away_goals = int(row["awayGoals"])

    if prev_home == home_goals and prev_away == away_goals:
        return "no immediate score change"

    if team == home:
        if home_goals > prev_home:
            return f"{team} just scored"
        if away_goals > prev_away:
            return f"{team} just conceded"
    else:
        if away_goals > prev_away:
            return f"{team} just scored"
        if home_goals > prev_home:
            return f"{team} just conceded"

    return "score changed"


def get_team_snapshot(team, squad_info):
    return squad_info.get(team, {})


def get_theme_candidates(team, row, prev_row):
    home = row["homeTeam_name"]
    away = row["awayTeam_name"]
    minute = int(row["minute"])
    home_goals = int(row["homeGoals"])
    away_goals = int(row["awayGoals"])
    state = score_state_for_team(team, home, away, home_goals, away_goals)

    themes = []

    if minute >= 70 and abs(home_goals - away_goals) <= 1:
        themes.append("dramatic")

    if state == "losing" or describe_recent_change(prev_row, row, team, home, away) == f"{team} just scored":
        themes.append("comeback")

    if state == "winning" and abs(home_goals - away_goals) >= 2:
        themes.append("dominant")

    if minute >= 60 and state != "winning" and abs(home_goals - away_goals) <= 1:
        themes.append("pressure")

    if prev_row is not None:
        prev_home = int(prev_row["homeGoals"])
        prev_away = int(prev_row["awayGoals"])
        if prev_home != home_goals or prev_away != away_goals:
            themes.append("turning_point")

    if not themes:
        themes.append("surprise")

    return ",".join(dict.fromkeys(themes))


def compute_puzzle_candidate_score(team, row):
    home = row["homeTeam_name"]
    away = row["awayTeam_name"]
    minute = int(row["minute"])
    home_goals = int(row["homeGoals"])
    away_goals = int(row["awayGoals"])
    goal_diff = abs(home_goals - away_goals)
    team_state = score_state_for_team(team, home, away, home_goals, away_goals)

    score = 0.25
    if minute >= 55:
        score += 0.2
    if goal_diff <= 1:
        score += 0.25
    if minute >= 70 and goal_diff <= 1:
        score += 0.2
    if team_state != "winning":
        score += 0.1

    return round(min(score, 1.0), 3)


def build_window_text(rows, idx, squad_info):
    row = rows[idx]
    home = row["homeTeam_name"]
    away = row["awayTeam_name"]
    minute = int(row["minute"])
    home_goals = int(row["homeGoals"])
    away_goals = int(row["awayGoals"])
    prev_row = rows[idx - 1] if idx > 0 else None

    home_snapshot = get_team_snapshot(home, squad_info)
    away_snapshot = get_team_snapshot(away, squad_info)

    start = max(0, idx - WINDOW_RADIUS)
    end = min(len(rows), idx + WINDOW_RADIUS + 1)
    window_rows = rows[start:end]

    timeline_bits = []
    for window_row in window_rows:
        timeline_bits.append(
            f"{window_row['minute']}' {scoreline_text(home, away, int(window_row['homeGoals']), int(window_row['awayGoals']))}"
        )

    team_focus_lines = []
    for team in (home, away):
        snapshot = home_snapshot if team == home else away_snapshot
        team_focus_lines.append(
            f"{team}: manager {snapshot.get('manager', 'unknown')}, "
            f"style {snapshot.get('style', 'unknown')}, "
            f"stamina {snapshot.get('team_stamina', 'unknown')}, "
            f"adaptability {snapshot.get('team_adaptability', 'unknown')}, "
            f"formation {snapshot.get('formation', 'unknown')}."
        )

    theme_candidates = get_theme_candidates(home, row, prev_row)
    puzzle_score = compute_puzzle_candidate_score(home, row)

    return (
        f"Match window: {home} vs {away}. Focus minute {minute} in the {get_game_phase(minute)}. "
        f"Current scoreline: {scoreline_text(home, away, home_goals, away_goals)}. "
        f"Recent timeline: {' | '.join(timeline_bits)}. "
        f"Home team state: {home} are {score_state_for_team(home, home, away, home_goals, away_goals)}; "
        f"away team state: {away} are {score_state_for_team(away, home, away, home_goals, away_goals)}. "
        f"Recent swing: {describe_recent_change(prev_row, row, home, home, away)} for {home}; "
        f"{describe_recent_change(prev_row, row, away, home, away)} for {away}. "
        f"{team_focus_lines[0]} {team_focus_lines[1]} "
        f"Theme candidates for retrieval: {theme_candidates}. "
        f"Puzzle candidate score: {puzzle_score}."
    )


def build_metadata(league_name, rows, idx, squad_info):
    row = rows[idx]
    prev_row = rows[idx - 1] if idx > 0 else None
    home = row["homeTeam_name"]
    away = row["awayTeam_name"]
    minute = int(row["minute"])
    home_goals = int(row["homeGoals"])
    away_goals = int(row["awayGoals"])

    home_state = score_state_for_team(home, home, away, home_goals, away_goals)
    away_state = score_state_for_team(away, home, away, home_goals, away_goals)
    home_themes = get_theme_candidates(home, row, prev_row)
    away_themes = get_theme_candidates(away, row, prev_row)

    home_snapshot = get_team_snapshot(home, squad_info)
    away_snapshot = get_team_snapshot(away, squad_info)

    return {
        "league": league_name,
        "match_id": str(row["match_id"]),
        "minute": minute,
        "home_team": home,
        "away_team": away,
        "home_goals": home_goals,
        "away_goals": away_goals,
        "game_phase": get_game_phase(minute),
        "goal_diff_abs": abs(home_goals - away_goals),
        "close_score": 1 if abs(home_goals - away_goals) <= 1 else 0,
        "late_game": 1 if minute >= 70 else 0,
        "turning_point": 1 if "turning_point" in home_themes or "turning_point" in away_themes else 0,
        "home_state": home_state,
        "away_state": away_state,
        "home_themes": home_themes,
        "away_themes": away_themes,
        "home_style": home_snapshot.get("style", "unknown"),
        "away_style": away_snapshot.get("style", "unknown"),
        "home_manager": home_snapshot.get("manager", "unknown"),
        "away_manager": away_snapshot.get("manager", "unknown"),
        "home_puzzle_score": compute_puzzle_candidate_score(home, row),
        "away_puzzle_score": compute_puzzle_candidate_score(away, row),
        "doc_version": "v2_windowed",
    }


def embed_batch(texts):
    response = openai_client.embeddings.create(
        input=texts,
        model="text-embedding-3-small"
    )
    return [e.embedding for e in response.data]


def ingest_league(league):
    name = league["name"]
    squad_info = load_squad_info(league["squad_csv"])

    with open(league["match_csv"], newline="", encoding="utf-8") as f:
        grouped_rows = defaultdict(list)
        for row in csv.DictReader(f):
            grouped_rows[row["match_id"]].append(row)

    existing_ids = collection.get(where={"league": name}).get("ids", [])
    if existing_ids:
        collection.delete(ids=existing_ids)
        print(f"[{name}] Removed {len(existing_ids)} existing rows for re-ingestion.")

    documents = []
    ids = []
    metadatas = []

    for match_id, rows in grouped_rows.items():
        rows.sort(key=lambda r: int(r["minute"]))
        for idx, row in enumerate(rows):
            documents.append(build_window_text(rows, idx, squad_info))
            ids.append(f"{name}_{match_id}_{row['minute']}")
            metadatas.append(build_metadata(name, rows, idx, squad_info))

    print(f"\n[{name}] {len(documents)} enriched windows to ingest...")

    batch_size = 100
    for i in range(0, len(documents), batch_size):
        batch_docs = documents[i:i + batch_size]
        batch_ids = ids[i:i + batch_size]
        batch_meta = metadatas[i:i + batch_size]

        embeddings = embed_batch(batch_docs)
        collection.add(
            ids=batch_ids,
            embeddings=embeddings,
            documents=batch_docs,
            metadatas=batch_meta
        )
        print(f"[{name}] Ingested rows {i + 1}-{i + len(batch_docs)} / {len(documents)}")

    print(f"[{name}] Done.")


if __name__ == "__main__":
    print("Starting JGaffer knowledge base ingestion...")
    print("NOTE: This rebuilds the retrieval store with richer moment windows.")
    confirm = input("Proceed? [y/N]: ").strip().lower()
    if confirm != "y":
        print("Aborted.")
        sys.exit(0)

    for league in LEAGUES:
        ingest_league(league)

    print("\nIngestion complete. ChromaDB store saved to ./chroma_store")
