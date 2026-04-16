import csv
import os
import sys
import chromadb
from importlib_metadata import metadata
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

def load_squad_info(filepath):
    squad={}
    with open(filepath, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            squad[row["team_name"]] = row
    return squad

def get_game_phase(minute):
    m = int(minute)
    if m <= 15:
        return "early game"
    elif m <= 44:
        return "first half"
    elif m <= 50:
        return "half time"
    elif m <= 60:
        return "build phase"
    elif m <= 70:
        return "tension time"
    elif m <= 87:
        return "late game"
    else:
        return "stoppage time"

def get_score_situation(home, away, home_goals, away_goals):
    if home_goals > away_goals:
        return f"{home} leading {home_goals}-{away_goals}"
    elif away_goals > home_goals:
        return f"{away} leading {away_goals}-{home_goals}"
    else:
        return f"level at {home_goals}-{away_goals}"

def row_to_text(row, squad_info):
    home = row["homeTeam_name"]
    away = row["awayTeam_name"]
    minute = row["minute"]
    home_goals = int(row["homeGoals"])
    away_goals = int(row["awayGoals"])

    h = squad_info.get(home, {})
    a = squad_info.get(away, {})

    return (
        f"{home} vs {away}, minute {minute} ({get_game_phase(minute)}). "
        f"Score: {get_score_situation(home, away, home_goals, away_goals)}. "
        f"{home} managed by {h.get('manager', 'unknown')}, "
        f"playing {h.get('style', 'unknown')} style, "
        f"{h.get('team_stamina', 'unknown')} stamina, "
        f"{h.get('team_adaptability', 'unknown')} adaptability. "
        f"{away} managed by {a.get('manager', 'unknown')}, "
        f"playing {a.get('style', 'unknown')} style, "
        f"{a.get('team_stamina', 'unknown')} stamina, "
        f"{a.get('team_adaptability', 'unknown')} adaptability."
    )

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
        rows = list(csv.DictReader(f))

    print(f"\n[{name}] {len(rows)} rows to ingest...")

    existing_ids = set(collection.get(where={"league": name})["ids"])
    rows = [r for r in rows if f"{name}_{r['match_id']}_{r['minute']}" not in existing_ids]

    if not rows:
        print(f"[{name}] Already fully ingested. Skipping.")
        return

    batch_size = 100
    for i in range(0, len(rows), batch_size):
        batch = rows[i : i + batch_size]
        texts = [row_to_text(r, squad_info) for r in batch]
        ids = [f"{name}_{r['match_id']}_{r['minute']}" for r in batch]
        metadatas = [
            {
                "league": name,
                "match_id": r["match_id"],
                "minute": int(r["minute"]),
                "home_team": r["homeTeam_name"],
                "away_team": r["awayTeam_name"],
                "home_goals": int(r["homeGoals"]),
                "away_goals": int(r["awayGoals"]),
            }
            for r in batch
        ]

        embeddings = embed_batch(texts)
        collection.add(ids=ids, embeddings=embeddings, documents=texts, metadatas=metadatas)
        print(f"[{name}] Ingested rows {i + 1}–{i + len(batch)} / {len(rows)}")

    print(f"[{name}] Done.")

if __name__ == "__main__":
    print("Starting JGaffer knowledge base ingestion...")
    print("NOTE: This embeds ~68,400 rows. Estimated cost: ~$0.07 (one-time).")
    confirm = input("Proceed? [y/N]: ").strip().lower()
    if confirm != "y":
        print("Aborted.")
        sys.exit(0)

    for league in LEAGUES:
        ingest_league(league)

    print("\nIngestion complete. ChromaDB store saved to ./chroma_store")