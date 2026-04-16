# JGaffer — Backend Reference

Last updated: 2026-04-16
Branch: v2/layer-6-statsbomb-pipeline

---

## Architecture Overview

Two services run in parallel:

```
React frontend (port 5173 dev / served by Spring Boot in prod)
       |
Spring Boot — port 8080   ←→   FastAPI RAG service — port 8000
       |
  In-memory CSV data (loaded at startup)
```

Spring Boot owns the HTTP surface. It proxies `/api/story` and `/api/explain`
to the Python FastAPI service via `RestTemplate`. The Python service owns all
AI interactions (embeddings + GPT calls) and the ChromaDB vector store.

---

## Spring Boot Backend

**Entry point:** `JGafferWebApplication.java` — standard `@SpringBootApplication`, starts on port 8080.

**Package root:** `com.sickton.jgaffer`

### Data Files (classpath resources)

```
JGaffer/src/main/resources/
  PremierLeague/
    SquadInformation.csv       — team attributes (one row per team)
    PremierLeagueMatches.csv   — match ID → match title map (e.g. "ARS-AVL")
    MatchMinuteContext.csv     — minute-by-minute match rows
  SerieA/
    SquadInformation.csv
    SerieAMatches.csv
    MatchMinuteContext.csv
```

**SquadInformation.csv columns:**
`team_name, manager, style, team_stamina, team_adaptability, formation, team_code,
atk_weight, def_weight, ctrl_weight`

- `style` → maps to `Style` enum (POSSESSION, COUNTER_ATTACK, HIGH_PRESS, DEFENSIVE, BALANCED)
- `team_stamina` → `StaminaLevel` enum (LOW / MEDIUM / HIGH)
- `team_adaptability` → `TeamAdaptability` enum (LOW / MEDIUM / HIGH)
- `formation` → `Formation` enum (F_4_3_3, F_3_4_3, F_4_2_3_1, F_3_5_2, F_5_3_2)
- `atk_weight / def_weight / ctrl_weight` → PCA-derived floats (optional; -1.0 = not set)

**MatchMinuteContext.csv columns:**
`match_id, minute, homeTeam_name, awayTeam_name, homeGoals, awayGoals`

~68,400 rows total across both leagues (every match at every minute).

---

### Domain Model

| Class | Role |
|---|---|
| `MatchContext` | Immutable snapshot: home Team, away Team, homeGoals, awayGoals, minute, title |
| `Team` | name, Squad, StaminaLevel, TeamAdaptability, Formation, TeamIntent |
| `TeamIntent` | attack/defence/control weights (0.0–1.0). Built from Style or explicit PCA weights |
| `Squad` | team name, manager name, Style enum, base Formation |
| `TacticRecommendation` | recommended Tactic, confidence (int), suggested Formation |
| `Tactic` | enum: 7 tactics (ATTACK, DEFEND, PRESS, COUNTER, POSSESS, PARK_THE_BUS, DIRECT) |
| `GamePhase` | enum: EARLY_MINUTES, CLOSING_HALF, HALF_TIME, BUILD_PHASE, TENSION_TIME, LATE_GAME, STOPPAGE_TIME |
| `Formation` | enum: 5 formations with string labels |
| `StaminaLevel` | LOW / MEDIUM / HIGH |
| `TeamAdaptability` | LOW / MEDIUM / HIGH |

---

### Data Loading

**`ApplicationParser`** (utility) — parses all CSV files from classpath:
- `parseTitles(path)` → `Map<Integer, String>` (matchId → title)
- `parseSquadInformation(path)` → `Map<String, FileStorage>` (teamName → FileStorage)
- `buildTeamMapFromCsv(path)` → `Map<Integer, String>` (teamId → teamName)
- `getTeamCodeMap(path)` → `Map<String, String>` (teamName → 3-letter code)

**`FileStorage`** — intermediate holder from CSV parse:
Bundles Squad, TeamAdaptability, StaminaLevel, Formation, and optional PCA weights.
`hasCustomWeights()` returns true if all three PCA weights are present (not -1.0).

**`LeagueDataFactory`** (static factory):
- `buildAllContexts(csvPath, titles, teamData)` → `Map<String, MatchContext>`
  Key format: `"ARS-AVL_47"` (matchTitle + "_" + minute). Holds entire season in memory.
- `getFixtureList(teamCode, titles)` → all match IDs/titles containing that team code
- `buildTeamFromName(name, teamData)` → constructs Team using PCA weights if present,
  style-bias formula otherwise

**`MatchService`** (`@Service`, `@PostConstruct`):
Loads all CSV data at startup into memory maps for both leagues (PL and SA).
Public API used by the controller:
- `getTeams(league)` → sorted team map
- `getTeamName(league, teamId)` → team name string
- `getFixtures(league, teamName)` → `{home: [...], away: [...]}` fixture lists
- `getMatchContext(league, matchNumber, minute)` → MatchContext or null
- `getTeam(league, teamName)` → Team domain object
- `getRecommendation(context, team)` → TacticRecommendation (delegates to engine)
- `getExplanation(context, team, recommendation)` → String from OpenAI
- `getAllTactics()` → List of Tactic enum values

---

### Tactical Recommendation Engine (Legacy — partially stubbed for v2)

**`TacticalRecommendationEngine`**:
Holds 7 `TacticalRule` instances (one per game phase). On `recommendWithDetails(context, team)`,
finds the single rule whose `applies()` returns true, then calls `recommendWithConfidence()`.
Throws if zero or more than one rule applies.

**`TacticalRule`** (abstract base):
- `applies(context, team)` → boolean (checks minute range via `checkGamePhase()`)
- `recommendWithConfidence(context, team)` → TacticRecommendation

**Game phase rules and minute ranges:**

| Class | Phase | Minutes | Status |
|---|---|---|---|
| `EarlyMinuteTactics` | EARLY_MINUTES | 0–15 | **Stubbed** (throws UnsupportedOperationException) |
| `ClosingHalfTactics` | CLOSING_HALF | 16–44 | **Stubbed** |
| `HalfTimeTactics` | HALF_TIME | 45–50 | **Stubbed** |
| `BuildPhaseTactics` | BUILD_PHASE | 51–60 | **Stubbed** |
| `TensionTimeTactics` | TENSION_TIME | 61–70 | **Stubbed** |
| `LateGameTactics` | LATE_GAME | 71–87 | **Stubbed** |
| `StoppageTimeTactics` | STOPPAGE_TIME | 88+ | **Stubbed** |

All 7 phase rules are registered but throw `UnsupportedOperationException` — the engine
structure is in place but the rule logic was not ported to v2. The `/api/recommend`
endpoint that uses this engine is considered legacy.

---

### OpenAI Integration (Legacy — used by /api/recommend only)

**`OpenAIClient`** — wraps raw HTTP call to OpenAI chat completions.
Reads key from `@Value("${openai.api.key:}")`.

**`TacticalExplanationService`** — takes a pre-built prompt string, calls `OpenAIClient`,
returns explanation text. Called by `MatchService.getExplanation()`.

Prompt format: coach-voice briefing, 5 bullet points, plain ASCII, no markdown.
Inputs: tactic, formation, opponent, styles, stamina, adaptability, phase, minute, score, goal diff.

---

### RAG Proxy (Active — used by new casual fan flow)

**`RagService`** (`@Service`):
Thin proxy using `RestTemplate`. Calls `http://localhost:8000` (Python FastAPI).

- `getStory(team, league, mode, queryType)` → POST `/story` → returns raw Object (JSON passthrough)
- `explainMoment(momentData)` → POST `/explain` → returns raw Object (JSON passthrough)

No transformation — the Python response is forwarded as-is to the frontend.

---

### API Controller

**`ApiController`** (`@RestController`, `/api`):

| Method | Path | Parameters | Handler | Active |
|---|---|---|---|---|
| GET | `/api/clubs` | `league=PL\|SA` | `matchService.getTeams()` | Yes |
| GET | `/api/fixtures` | `teamId, league` | `matchService.getFixtures()` | Legacy |
| GET | `/api/match` | `matchId, teamId, league` | `matchService.getMatchContext()` at random minute | Legacy |
| POST | `/api/recommend` | `teamId, matchId, minute, userTactic, league` | Engine + OpenAI explanation | Legacy |
| GET | `/api/story` | `team, league, mode, queryType` | `ragService.getStory()` | Yes |
| POST | `/api/explain` | JSON body (moment fields) | `ragService.explainMoment()` | Yes |

**`SpaController`** — catch-all for React Router; forwards unmapped paths to `index.html`.

---

## Python RAG Service

**Location:** `rag-service/`
**Runtime:** FastAPI on port 8000
**Dependencies:** fastapi, openai, chromadb, python-dotenv, pydantic

### Knowledge Base (ChromaDB)

**`ingest.py`** — one-time ingestion script (~$0.07 cost, ~68,400 rows):

Reads both `MatchMinuteContext.csv` files, joins with `SquadInformation.csv` per league,
converts each row to a natural language string, then embeds and stores in ChromaDB.

**Text format per row:**
```
"{home} vs {away}, minute {m} ({phase}). Score: {situation}.
{home} managed by {manager}, playing {style} style, {stamina} stamina, {adaptability} adaptability.
{away} managed by {manager}, playing {style} style, {stamina} stamina, {adaptability} adaptability."
```

**ChromaDB collection:** `match_moments` (cosine similarity, persisted to `./chroma_store`)

**Document ID format:** `{LEAGUE}_{match_id}_{minute}` (e.g. `PL_14_67`)

**Stored metadata per document:**
`league, match_id, minute, home_team, away_team, home_goals, away_goals`

---

### FastAPI Endpoints

**`GET /health`** → `{"status": "ok"}`

---

**`POST /story`**

Request body:
```json
{ "team": "Arsenal", "league": "PL", "mode": "simple|indepth", "query_type": "dramatic|dominant|comeback|pressure|turning_point|surprise" }
```

Flow:
1. `n_results` = 5 (simple) or 10 (indepth)
2. Build semantic query from `QUERY_TEMPLATES[query_type]` + team name.
   `surprise` picks a random non-surprise query type.
3. Embed query with `text-embedding-3-small`
4. Query ChromaDB: top 200 results filtered to `home_team == team OR away_team == team`
5. Group by `match_id`, pick one random moment per match → diverse pool
6. Shuffle and take `n_results`
7. Send sampled context to GPT-4o-mini with a storyteller prompt

GPT output format (JSON object):
```json
{
  "moments": [
    {
      "headline": "string (max 8 words)",
      "minute": int,
      "match": "Home vs Away",
      "score": "string",
      "narrative": "2-3 sentences, plain English, no jargon",
      "concept": "football concept name (e.g. High Press)"
    }
  ]
}
```

Response: `{ "team", "query_type", "moments": [...] }`

---

**`POST /explain`**

Request body:
```json
{ "headline": "", "match": "", "minute": int, "score": "", "concept": "", "team": "" }
```

Flow: Builds a teacher-voice prompt, calls GPT-4o-mini (temp 0.7).

Prompt instructs GPT to write 4-5 sentences covering:
1. What is physically happening on the pitch
2. Why this moment matters in the match
3. What the concept means in plain English
4. What the team should do next and why

Rules: no jargon, no stats, explain to a friend who loves drama but not football.

Response: `{ "headline", "match", "minute", "concept", "explanation": "string" }`

---

## What Is and Isn't Built

| Feature | Status |
|---|---|
| League + club selection (`/api/clubs`) | Done |
| RAG story retrieval (`/api/story`) | Done |
| Moment explanation (`/api/explain`) | Done |
| ChromaDB knowledge base (68k moments, both leagues) | Done |
| Tactical recommendation engine (`/api/recommend`) | Stubbed — all 7 phase rules throw |
| OpenAI tactical explanation (legacy flow) | Done but only reachable via legacy route |
| StatsBomb pass extraction (`extract_passes.py`) | Done — 143,349 rows, 1,162 matches |
| Formation probability matrices (`build_matrices.py`) | Not started |
| Press escape outcome model (`train_escape_model.py`) | Not started |
| `/api/network` endpoint (graph generation) | Not started |
| Tension Score (0–100 model) | Not started |
| "What to Watch" as a distinct API field | Not started |
