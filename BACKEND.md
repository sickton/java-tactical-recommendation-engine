# JGaffer - Backend Reference

Last updated: 2026-04-20

---

## Architecture Overview

The current backend is a two-service system:

```text
React frontend (Vite dev / Spring-served in prod)
       |
Spring Boot API gateway (port 8080)
       |
FastAPI service (port 8000)
```

### Responsibility split

**Spring Boot**
- public `/api` surface for the frontend
- league and club data loading from CSV
- team lookup and formation lookup
- delegates retrieval/explanation/network generation to FastAPI

**FastAPI**
- moment retrieval (`/story`)
- moment explanation (`/explain`)
- graph generation and edge scoring for the puzzle (`/network`)
- ChromaDB vector store
- OpenAI calls
- escape model inference

---

## Active Spring Boot Surface

**Entry point:** `JGafferWebApplication.java`

**Primary controller:** `ApiController.java`

### Active API endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/clubs?league=PL|SA` | Returns clubs for league selection |
| GET | `/api/story?team=&league=&mode=&queryType=` | Proxies to FastAPI `/story` |
| POST | `/api/explain` | Proxies to FastAPI `/explain` |
| GET | `/api/network?escapingTeam=&pressingTeam=&league=&minute=&homeGoals=&awayGoals=` | Looks up formations in Spring and proxies to FastAPI `/network` |

Legacy fixture/match/recommendation endpoints have been removed.

---

## Spring Boot Data Layer

### Classpath resources

```text
JGaffer/src/main/resources/
  PremierLeague/
    SquadInformation.csv
    PremierLeagueMatches.csv
    MatchMinuteContext.csv
  SerieA/
    SquadInformation.csv
    SerieAMatches.csv
    MatchMinuteContext.csv
```

### What is still actively used

For the current product flow, Spring Boot actively uses:
- `SquadInformation.csv` to load clubs and team attributes
- team formations for the puzzle `/api/network` endpoint

The match-minute CSVs still exist in the repo because they are used by the Python retrieval pipeline and ingestion flow, but they are no longer loaded into in-memory Java match contexts.

---

## Active Spring Classes

### `MatchService`
Current purpose:
- load league team lists
- load parsed squad/team data
- return teams for `/api/clubs`
- build `Team` objects for `/api/network`

Current public API:
- `getTeams(league)`
- `getTeam(league, teamName)`

### `LeagueDataFactory`
Current purpose:
- build `Team` domain objects from parsed squad data

### `ApplicationParser`
Current purpose:
- parse `SquadInformation.csv`
- build `teamId -> teamName` maps

### `RagService`
Current purpose:
- proxy requests from Spring Boot to FastAPI

Public proxy methods:
- `getStory(team, league, mode, queryType)`
- `explainMoment(momentData)`
- `getNetwork(escapingTeam, escapingFormation, pressingTeam, pressingFormation, league, minute, homeGoals, awayGoals)`

---

## Active Domain Model

The remaining active Java domain layer is mostly about teams and formations.

| Class | Role |
|---|---|
| `Team` | Full team object used for backend graph requests |
| `Squad` | Team name, manager, style, base formation |
| `TeamIntent` | Attack / defence / control weights |
| `Formation` | Formation enum used in puzzle graph generation |
| `Style` | Team style enum |
| `StaminaLevel` | Team stamina enum |
| `TeamAdaptability` | Team adaptability enum |
| `FileStorage` | Intermediate parsed squad-data holder |

Removed from the active backend:
- `MatchContext`
- `Tactic`
- `TacticRecommendation`
- `GamePhase`
- tactical recommendation engine and phase rule classes
- old OpenAI explanation classes used by the removed recommendation flow

---

## `/api/network` Flow

This is the main Spring-owned backend logic beyond simple proxying.

### Request

`GET /api/network`

Query params:
- `escapingTeam`
- `pressingTeam`
- `league`
- `minute`
- `homeGoals`
- `awayGoals`

### Spring-side work

1. Look up both teams by name from squad data.
2. Extract their formations from the `Team` domain objects.
3. Forward the request to FastAPI `/network` with team names, formations, league, and match state.

### Response shape

```json
{
  "escape_graph": {
    "nodes": [{ "id": "LB", "x": 0.18, "y": 0.12 }],
    "edges": [{ "from": "LB", "to": "CDM", "escape_prob": 0.72 }]
  },
  "pressing_graph": {
    "nodes": [],
    "edges": []
  },
  "game_phase": "BUILD_PHASE",
  "escaping_formation": "F_4_3_3",
  "pressing_formation": "F_4_3_3"
}
```

---

## FastAPI Service

**Location:** `rag-service/main.py`

### Active endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Health check |
| POST | `/story` | Retrieve and generate moment cards |
| POST | `/explain` | Explain a moment with nearby grounded context |
| POST | `/network` | Build puzzle graphs and score escape edges |

---

## Retrieval Pipeline

The retrieval system is no longer just raw minute-level semantic search.

### Ingestion (`rag-service/ingest.py`)

The current ingestion flow:
- reads Premier League and Serie A minute context CSVs
- joins squad metadata
- creates richer match windows around each minute
- stores tactical and narrative retrieval hints in Chroma metadata

Examples of enriched metadata:
- `game_phase`
- `goal_diff_abs`
- `close_score`
- `late_game`
- `turning_point`
- `home_state`
- `away_state`
- `home_themes`
- `away_themes`
- `home_puzzle_score`
- `away_puzzle_score`

### Story retrieval (`/story`)

Current flow:
1. Build a theme-aware query from team + query type.
2. Embed query with `text-embedding-3-small`.
3. Retrieve candidate windows from Chroma filtered by league and team.
4. Rerank candidates using:
   - semantic similarity
   - theme-fit heuristics
   - puzzle usefulness
   - recency interest
5. Cluster nearby minutes to avoid overcounting one event.
6. Enforce diversity across matches and contexts.
7. Ask GPT-4o-mini to turn selected candidates into readable moment cards.
8. Apply a final post-generation constraint pass so one match does not dominate the result set.

### Explanation (`/explain`)

Current flow:
1. Accept selected moment fields from frontend.
2. Retrieve nearby context from the same match window.
3. Ask GPT-4o-mini to explain the moment using both the selected card and nearby retrieved context.

The intention is to keep the explanation grounded in actual match state instead of generating from the card alone.

---

## Puzzle Intelligence Layer

The network pipeline in FastAPI uses:
- formation matrices from `rag-service/pipeline/output/matrices/`
- `escape_model.joblib`
- current minute to determine game phase
- team formations supplied by Spring Boot

### Output responsibilities

FastAPI `/network` returns:
- `escape_graph`
- `pressing_graph`
- `game_phase`
- `escaping_formation`
- `pressing_formation`

The frontend currently still computes some puzzle-specific derived values locally, but the backend owns graph generation and edge scoring.

---

## Pipeline Assets

Offline pipeline scripts live in:

```text
rag-service/pipeline/
  extract_passes.py
  build_matrices.py
  train_escape_model.py
```

Generated artifacts live in:

```text
rag-service/pipeline/output/
  escape_model.joblib
  escape_model_meta.json
  passes.csv
  matrices/
```

These scripts are not part of runtime request handling, but they remain part of the backend data and model pipeline.

---

## Current Backend Direction

The backend is now aligned to the active product vision:

**experience a real moment -> understand the tactical problem -> solve it -> learn from feedback**

What matters most going forward:
- improving retrieval quality and moment diversity
- introducing a structured "moment brief" that connects story to puzzle
- moving more puzzle truth and evaluation into backend-owned logic
- making post-puzzle coaching more explicit

The old recommendation simulator architecture has been intentionally removed to reduce clutter and keep the codebase centered on the current product.
