import os
import random
import chromadb
from fastapi import FastAPI
from openai import OpenAI
from dotenv import load_dotenv
from pydantic import BaseModel

load_dotenv()

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
        n_results=50,
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
    sampled = random.sample(combined, min(n_results, len(combined)))

    return sampled

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
        model="gpt-4o",
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