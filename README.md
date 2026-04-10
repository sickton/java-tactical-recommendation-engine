# JGaffer

A rule-driven tactical recommendation engine for football, served as a full-stack web application.

Given a match snapshot — scoreline, minute, team style, stamina, and PCA-derived tactical weights — the engine evaluates phase-specific rules and recommends the most appropriate tactic. An AI explanation (OpenAI) is generated to justify the decision.

---

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 23, Spring Boot, Maven |
| Engine | Rule-based strategy pattern (7 game phases × 7 tactics) |
| Frontend | React, TypeScript, Vite |
| Database | PostgreSQL |
| AI | OpenAI GPT-4.1 (tactical explanations) |

---

## Project Structure

```
JGaffer/          Spring Boot backend + CSV data
frontend/         React frontend (builds into JGaffer/src/main/resources/static/)
```

---

## Running the App

### Prerequisites
- Java 23
- Maven
- Node.js
- PostgreSQL running on `localhost:5432` with database `jgaffer`
- OpenAI API key (optional — explanations fall back gracefully without it)

### Backend

```bash
cd JGaffer
JAVA_HOME="C:\Program Files\Java\jdk-23" mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

### Frontend (dev mode)

```bash
cd frontend
npm install
npm run dev
```

### Frontend (production build)

```bash
cd frontend
npm run build
# Outputs to JGaffer/src/main/resources/static/ — served by Spring Boot
```

---

## Leagues

| League | Teams | IDs |
|---|---|---|
| Premier League | 20 teams, 2024/25 | 1–20 |
| Serie A | 20 teams, 2024/25 | 21–40 |

---

## How the Engine Works

1. Team tactical weights are derived from PCA analysis of season stats (attack / defence / control)
2. Weights are mapped to a `WeightCombination` bucket (LOW / MEDIUM / HIGH per dimension)
3. The engine looks up `Style × WeightCombination × GamePhase → Tactic` via `tactics.csv`
4. Opponent adjustments shift the weights slightly before the lookup
5. Confidence is computed from the strength of the best rule match

### Game Phases

| Phase | Minutes |
|---|---|
| Early Minutes | 0–15 |
| Closing Half | 16–44 |
| Half Time | 45–50 |
| Build Phase | 51–60 |
| Tension Time | 61–70 |
| Late Game | 71–87 |
| Stoppage Time | 88+ |

### Tactics

`GEGENPRESSING` · `HIGH_PRESS` · `TIKI_TAKA` · `CONTROL` · `COUNTER_ATTACK` · `DIRECT_PLAY` · `LOW_BLOCK`

---

## Tests

```bash
cd JGaffer
JAVA_HOME="C:\Program Files\Java\jdk-23" mvn test
```
