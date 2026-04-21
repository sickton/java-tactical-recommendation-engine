# JGaffer

JGaffer is an interactive football learning product for casual fans.

Its purpose is to help a user:
- experience a real moment from their team's season
- understand the tactical problem inside that moment
- try to solve that problem through an interactive puzzle
- learn the football idea behind it through guided feedback

This is the current product direction:

**Moment -> Mission -> Puzzle -> Coaching**

---

## What It Does

Pick a league. Pick a club. Pick a type of moment.

JGaffer retrieves real moments from the 2024/25 season and presents them in a way that is easy for a casual fan to care about. The system then explains the tactical problem underneath the moment and turns that problem into a playable puzzle.

The core product loop is:

1. **Moment**
The user discovers a real moment from their team's season.

2. **Mission**
The system frames the tactical problem in plain English.

3. **Puzzle**
The user tries to solve that exact problem on the pitch.

4. **Coaching**
The product explains what worked, what did not, and what concept the user should remember.

---

## Current User Flow

```text
/                    LeagueSelect
  -> /clubs          ClubSelect
    -> /mode         ModeSelect
      -> /query      QuerySelect
        -> /moments  Moments
          -> /moment MomentDetail
            -> /puzzle Puzzle
```

The older tactical recommendation flow has been removed from the codebase.

---

## Core Features

### Moment Discovery
- 6 moment themes: Dramatic, Dominant, Comeback, Under Pressure, Turning Points, Surprise
- Retrieval pipeline built on enriched match windows from Premier League and Serie A season data
- Diversity-aware story selection so moments are not dominated by one match
- GPT-4o-mini turns retrieved candidate moments into short, readable moment cards

### Guided Explanation
- `POST /api/explain` grounds explanation using nearby retrieved context from the same match
- Explanations are written for a casual fan, not an analyst
- The goal is to bridge the user from story to tactical problem

### Tactical Puzzle
- Left panel: opposition press structure rendered as a dynamic `PressureMap`
- Right panel: escape-route graph rendered as `PitchGraph`
- User builds a passing sequence under pressure
- Puzzle feedback compares the chosen route against the strongest route in the graph

### Football Intelligence Layer
- Formation probability matrices learned from StatsBomb open event data
- HistGradientBoosting escape model used to score press-escape edges
- Spring Boot uses team formation data and delegates graph generation/scoring to FastAPI

---

## Stack

| Layer | Tech |
|---|---|
| Frontend | React 18, TypeScript, Vite |
| API gateway | Java 23, Spring Boot 3 |
| AI / retrieval / graph logic | Python 3, FastAPI, ChromaDB, OpenAI |
| ML model | scikit-learn HistGradientBoosting |
| Data pipeline | pandas, StatsBomb open event data |

---

## Project Structure

```text
JGaffer/            Spring Boot API + squad/match CSV resources
frontend/           React frontend
rag-service/        FastAPI retrieval, explanation, and graph service

rag-service/
  ingest.py         Rebuilds ChromaDB retrieval store with enriched match windows
  main.py           FastAPI app (/story, /explain, /network, /health)
  pipeline/
    extract_passes.py
    build_matrices.py
    train_escape_model.py
    output/
      escape_model.joblib
      matrices/
```

---

## Running the App

## Prerequisites
- Java 23
- Maven
- Node.js 18+
- Python 3.11+
- OpenAI API key

## 1. Start the Python service

```bash
cd rag-service
pip install -r requirements.txt
uvicorn main:app --port 8000
```

If you need to rebuild the retrieval store:

```bash
python ingest.py
```

This refreshes the ChromaDB knowledge base using the current enriched ingestion format.

## 2. Start the Spring Boot backend

```bash
cd JGaffer
mvn spring-boot:run
```

Spring Boot runs on `http://localhost:8080`.

## 3. Start the frontend in dev mode

```bash
cd frontend
npm install
npm run dev
```

Vite runs on `http://localhost:5173`.

## 4. Build frontend for production

```bash
cd frontend
npm run build
```

The production build is emitted into Spring Boot static assets and served by the backend.

---

## Active Backend Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/clubs?league=PL|SA` | Returns clubs for league selection |
| GET | `/api/story?team=&league=&mode=&queryType=` | Returns retrieved moment cards |
| POST | `/api/explain` | Returns grounded explanation for a selected moment |
| GET | `/api/network?escapingTeam=&pressingTeam=&league=&minute=&homeGoals=&awayGoals=` | Returns escape and pressing graphs for puzzle generation |

---

## Data Sources

| Source | Description |
|---|---|
| Premier League `SquadInformation.csv` | Team manager, style, stamina, adaptability, formation, optional PCA weights |
| Serie A `SquadInformation.csv` | Same structure for Serie A |
| Premier League `MatchMinuteContext.csv` | Minute-by-minute match state |
| Serie A `MatchMinuteContext.csv` | Minute-by-minute match state |
| StatsBomb open event data | Used for pass extraction, formation matrices, and escape model training |

---

## Current Direction

The project is now centered on the moments-driven experience, not the older recommendation flow.

The key product question is:

**How do we help a casual fan see what expert fans see, without forcing them to read too much before they care?**

That is why the active roadmap is focused on:
- stronger retrieval
- clearer tactical problem framing
- tighter connection between moment and puzzle
- coach-like feedback after interaction

For product direction, see [VISION.md](VISION.md).
For implementation details, see [BACKEND.md](BACKEND.md) and [FRONTEND_STATE.txt](FRONTEND_STATE.txt).
