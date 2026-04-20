# JGaffer

An AI-powered football intelligence engine for casual fans — built around the idea that understanding football feels better when you're solving it, not just reading about it.

---

## What It Does

Pick a team. Pick a moment type. JGaffer surfaces real match moments from the 2024/25 season and explains what's happening in plain English.

Then it puts you in the manager's seat.

The **Tactical Puzzle** shows you how the opposition is pressing — a live diagram of their structure with role-differentiated players, cover shadows, and dynamic shift animations. You build your team's escape route by clicking through positions on the pitch. When you submit, the system compares your route to the statistically optimal path and gives you a verdict.

---

## Key Features

**Moment Discovery**
- Pick from 6 themes: Dramatic, Dominant, Comeback, Under Pressure, Turning Points, Surprise Me
- RAG pipeline retrieves semantically matched moments from 68,400 indexed match minutes
- GPT-4o-mini narrates each moment in plain English with no football jargon

**Tactical Puzzle**
- Left panel: opposition pressing structure — harasser closing down, shadowers blocking lanes, anchors holding shape, entire block sliding toward the ball side
- Right panel: passing options — safe/risky/blocked lanes color-coded, predictive arc showing where to pass (to space, not to feet), pulsing gold ring on the optimal target
- Cross-panel communication: hover a passing lane → the presser blocking it lights up
- Lure mechanic: shows the free player's run and the defender who follows, revealing the space left behind
- All passes are allowed — including risky ones through narrow windows

**Scoring**
- Medal system: GOLD / SILVER / BRONZE / MISS based on how close your route is to the optimal path
- Two meters: escape safety + forward progression
- Per-pass risk percentage in the breadcrumb trail

---

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 23, Spring Boot 3, Maven |
| AI / RAG | Python 3, FastAPI, ChromaDB, OpenAI GPT-4o-mini |
| ML Model | scikit-learn HistGradientBoosting (ROC-AUC 0.84) |
| Data pipeline | pandas, StatsBomb open event data (143,349 pass rows) |
| Frontend | React 18, TypeScript, Vite, SVG |

---

## Project Structure

```
JGaffer/           Spring Boot backend + match data CSVs
frontend/          React frontend (builds into JGaffer/src/main/resources/static/)
rag-service/       Python FastAPI service (RAG + GPT)
rag-service/
  ingest.py        One-time ChromaDB ingestion script
  main.py          FastAPI endpoints (/story, /explain, /health)
  extract_passes.py StatsBomb pass extraction pipeline
  build_matrices.py Formation probability matrix builder
  train_escape_model.py Press escape outcome model trainer
```

---

## Running the App

### Prerequisites
- Java 23
- Maven
- Node.js 18+
- Python 3.11+
- OpenAI API key

### 1. Start the Python RAG service

```bash
cd rag-service
pip install -r requirements.txt
uvicorn main:app --port 8000
```

On first run, ingest the knowledge base (one-time, ~$0.07):
```bash
python ingest.py
```

### 2. Start the Spring Boot backend

```bash
cd JGaffer
mvn spring-boot:run
```

App starts on `http://localhost:8080`. Set your OpenAI key:
```bash
export OPENAI_API_KEY=sk-...
```

### 3. Frontend (dev mode)

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` — proxies `/api` to the backend.

### 4. Frontend (production build)

```bash
cd frontend
npm run build
```

Outputs to `JGaffer/src/main/resources/static/` — served directly by Spring Boot.

---

## Data

| Source | Description | Size |
|---|---|---|
| StatsBomb open event data | Pass extraction for formation matrices + outcome model | 143,349 pressured pass rows, 1,162 matches |
| PL SquadInformation.csv | 20 teams, 2024/25 season — style, stamina, formation | 20 rows |
| PL MatchMinuteContext.csv | Minute-by-minute match state | ~38,000 rows |
| SA SquadInformation.csv | 20 teams, 2024/25 season | 20 rows |
| SA MatchMinuteContext.csv | Minute-by-minute match state | ~30,000 rows |

---

## Leagues

| League | Teams | Season |
|---|---|---|
| Premier League | 20 teams | 2024/25 |
| Serie A | 20 teams | 2024/25 |

---

## Status

Active development on branch `v2/layer-7-network-api`. The tactical puzzle is feature-complete. Next planned work: context adjustment layer (team style modifies escape probabilities), formation-level ground truth panel on the result screen.

For architecture details see [VISION.md](VISION.md), [BACKEND.md](BACKEND.md), and [FRONTEND_STATE.txt](FRONTEND_STATE.txt).
