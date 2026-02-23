# JGaffer — Tactical Recommendation Engine

## Project Structure
```
java-tactical-recommendation-engine/
├── JGaffer/                          Spring Boot backend (Maven)
│   ├── src/main/java/com/sickton/jgaffer/
│   │   ├── domain/                   Core immutable models
│   │   ├── engine/                   TacticalRecommendationEngine
│   │   ├── rules/                    Abstract TacticalRule + 7 game_phases/
│   │   ├── demoUI/                   CLI app (jgafferApplication) + LeagueDataFactory + PremierLeagueFactory
│   │   ├── openAIService/            OpenAIClient + TacticalExplanationService
│   │   ├── service/                  MatchService (data loading) + GameSimulator
│   │   ├── utility/                  TacticMapper, ApplicationParser, FileStorage
│   │   └── web/                      ApiController (REST JSON) + SpaController (HTML routing)
│   └── src/main/resources/
│       ├── tactics.csv               Classpath tactic lookup table
│       ├── application.properties
│       ├── PremierLeague/            MatchMinuteContext.csv, PremierLeagueMatches.csv, SquadInformation.csv
│       ├── SerieA/                   MatchMinuteContext.csv, SerieAMatches.csv, SquadInformation.csv
│       └── static/                   Built React SPA (index.html + assets/index-*.js + style.css + images/)
├── frontend/                         React 18 + TypeScript + Vite source
│   ├── public/
│   │   ├── style.css                 Single stylesheet (glassmorphic dark theme)
│   │   └── images/
│   │       ├── PremierLeague/        20 club crest PNGs
│   │       └── SerieA/               20 club crest PNGs
│   └── src/
│       ├── main.tsx                  Entry point
│       ├── App.tsx                   BrowserRouter + 6 Routes
│       ├── types.ts                  All shared TypeScript interfaces
│       ├── components/               Header.tsx, Footer.tsx, TeamCrest.tsx
│       ├── constants/                crests.ts, teamCodes.ts, tacticGuide.ts
│       ├── hooks/                    useLeagueTheme.ts, usePageTitle.ts
│       ├── pages/                    LeagueSelect, ClubSelect, Fixtures, Match, Result, Simulation
│       └── utils/                    formatTactic.ts
└── src/test/java/...                 TacticalRecommendationTest (3 tests)
```

## Running Tests
```
JAVA_HOME="C:\Program Files\Java\jdk-23" "C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd" test -f JGaffer/pom.xml
```
Run from: `java-tactical-recommendation-engine/` (project root, parent of JGaffer/)

## Building the Frontend
```
cd frontend && npm run build
```
Vite outputs to `JGaffer/src/main/resources/static/`. The `public/` directory (style.css + images/) is copied after clearing, so those files survive `emptyOutDir: true`.

## Backend

### Engine
- **Pattern**: Strategy — `TacticalRule` (abstract) → 7 concrete phase implementations in `rules/game_phases/`
- **Entry points**: `recommendTactic()` → `Tactic`, `recommendWithDetails()` → `TacticRecommendation` (tactic + int confidence 0–100)
- **Tactic lookup**: Style × WeightCombination × GamePhase → Tactic (CSV-driven via `TacticMapper`)
- **WeightCombination constructor order**: `(attack, defence, control)` — NOT alphabetical
- **Opponent awareness**: `applyOpponentStyleAdjustments()` in `TacticalRule` base class, called before stamina scaling in all 7 rules
- **Confidence**: `computeConfidence(attack, control, defence)` — distance from nearest boundary (0.33, 0.66), scaled 0–100

### Data Loading
- `MatchService` loads both leagues at `@PostConstruct` into `plContextMap` and `saContextMap`
- `ApplicationParser` methods all take a path string (e.g. `parseTitles("/PremierLeague/PremierLeagueMatches.csv")`)
- `LeagueDataFactory` is the generic factory; `PremierLeagueFactory` wraps it for CLI backwards-compat
- `FileStorage` bundles Squad + TeamAdaptability + StaminaLevel + Formation from `SquadInformation.csv`

### REST API — `ApiController` (`/api/*`)
All endpoints return JSON. `league` param is always `"PL"` or `"SA"` (default `"PL"`).

| Method | Path | Params | Returns |
|--------|------|--------|---------|
| GET | `/api/clubs` | `league` | `{ league, teams: [{id, name}] }` |
| GET | `/api/fixtures` | `teamId`, `league` | `{ teamId, teamName, league, homeFixtures, awayFixtures }` — each fixture: `{id, title}` |
| GET | `/api/match` | `matchId`, `teamId`, `league` | `{ teamId, teamName, opponentName, matchId, minute, league, gamePhase, context, tactics, isHome }` |
| POST | `/api/recommend` | `teamId`, `matchId`, `minute`, `userTactic`, `league` | `{ teamName, opponentName, minute, teamGoals, opponentGoals, userTactic, recommendation{tactic,confidence,formation}, agrees, explanation }` |
| POST | `/api/simulate` | `teamId`, `matchId`, `minute`, `userTactic`, `league` | Full `SimulationData` shape (see `types.ts`) |
| POST | `/api/simulate/phase` | `teamId`, `matchId`, `fromMinute`, `fromHomeGoals`, `fromAwayGoals`, `tacticsJson`, `league` | `PhaseResult` shape (events + goals + fidelity scores) |

### SPA Routing — `SpaController`
Forwards all React Router paths (`/`, `/clubs`, `/fixtures`, `/match`, `/result`, `/simulation`) to `static/index.html` so direct URL access and browser refresh work.

## 🔬 Analytics & Tactical Research Layer (Branch 4+)

JGaffer now includes an advanced tactical analytics and meta-evaluation layer built on top of the simulation engine.

---

### Architecture Additions

```
service/analytics/
    AnalyticsService

persistence/dto/
    TacticAnalytics
    LeagueNormalizerResult
    PhaseNormalizerResult
    PhasePerformance

web/
    AnalyticsController
```

All analytics endpoints are exposed under:

```
/api/analytics/*
```

---

## 1️⃣ Historical Analytics (DB-Based)

**Endpoint**
```
GET /api/analytics/win-rate-by-tactic?league=PL|SA
```

**Purpose**
- Aggregates logged simulations from `match_decisions`
- Returns total simulations, wins, and win rate per tactic
- Filtered by league

**Data Source**
- `MatchDecisionRepository`
- JPQL aggregation grouped by tactic

Reflects historical logged simulation results only.

---

## 2️⃣ League Normalizer (Monte Carlo Meta Sweep)

**Endpoint**
```
GET /api/analytics/league-normalizer
```

**Params**
- `league`
- `tactic`
- `samplesPerTeam` (default: 3)
- `iterationsPerSample` (default: 3)

**Method**
- Iterates across all teams in the league
- Random match selection
- Random minute sampling
- Repeated simulations
- Aggregates wins/losses/draws

**Purpose**
Removes:
- Team strength bias
- Opponent bias
- Single-match bias

Returns league-wide meta win rate for a tactic.

---

## 3️⃣ Phase Normalizer (Phase-Level Meta Strength)

**Endpoint**
```
GET /api/analytics/phase-normalizer
```

**Params**
- `league`
- `tactic`
- `samplesPerTeam`
- `iterationsPerSample`

**Method**
- Evaluates all 7 game phases:
    - EARLY_MINUTES
    - CLOSING_HALF
    - HALF_TIME
    - BUILD_PHASE
    - TENSION_TIME
    - LATE_GAME
    - STOPPAGE_TIME
- Runs Monte Carlo simulations per phase
- Returns win rate per phase

**Purpose**
Identifies:
- Phase strengths
- Tactical volatility
- Late-game effectiveness
- Momentum scaling behavior

---

## Runtime Profiles

**Default Profile**
- Uses `jgaffer` database
- Used for Monte Carlo normalizers and real simulations

**test-analytics Profile**
- Uses `jgaffer_test_analytics`
- Seeds 100 simulations
- Used for historical analytics preview

Monte Carlo normalizers do NOT depend on database state.

---

## Design Principles

- Simulation remains pure
- Normalizers do NOT persist results
- No engine modification
- Clean layering:

```
Controller → AnalyticsService → MatchService.simulate()
```

JGaffer now functions as both:
- Tactical recommendation engine
- Tactical meta-analysis and research platform

## Frontend

### Navigation Flow
```
GET /  →  LeagueSelect  →  GET /clubs?league=PL|SA
       →  ClubSelect    →  GET /fixtures?teamId=X&league=PL|SA
       →  Fixtures      →  GET /match?matchId=X&teamId=Y&league=PL|SA
       →  Match         →  POST /api/recommend  →  Result
                        →  POST /api/simulate   →  Simulation
```

### Pages
| Route | Component | Key features |
|-------|-----------|-------------|
| `/` | `LeagueSelect` | Neutral dark theme; strips league class + clears localStorage on mount |
| `/clubs` | `ClubSelect` | 14-col staggered grid (7-7-6); live search filter; white crest badges |
| `/fixtures` | `Fixtures` | Home/Away split panels; 2-col button cards per panel; opponent name + crest only |
| `/match` | `Match` | Hero scoreboard + white crest badges; game-phase progress bar; tactic picker + hover guide panel |
| `/result` | `Result` | Verdict banner; animated confidence bar; AI tactical briefing |
| `/simulation` | `Simulation` | Live clock ticker; event timeline; per-phase tactic picker; Tactical IQ fidelity bar |

### Components
- **`Header`** — Pitch SVG icon (always `#4ade80`), "JGaffer" logo, league badge (`body.league-pl` / `body.league-sa` controls which badge shows via CSS)
- **`Footer`** — Static footer
- **`TeamCrest`** — Renders `<img>` from `CRESTS` map; takes `teamName`, optional `className` + `size`

### Hooks
- **`useLeagueTheme(league)`** — Sets `body.league-pl` or `body.league-sa` class + saves to `localStorage.jgaffer_league`; FOUC prevention inline script in `index.html` restores class on hard refresh
- **`usePageTitle(title)`** — Sets `document.title` to `"<title> | JGaffer"`; restores `"JGaffer"` on unmount

### Utils & Constants
- **`formatTactic(tactic)`** — Converts `HIGH_PRESS` → `"High Press"`, `GEGENPRESSING` → `"Gegenpressing"` etc. Use everywhere a tactic enum is displayed
- **`CRESTS`** — `{ [teamName]: '/images/League/File.png' }` — all local paths in `public/images/`
- **`TEAM_CODES`** — `{ [3-letter-code]: fullName }` for PL + SA (used in Fixtures page)
- **`TACTIC_GUIDE`** — Static guide data (emoji, tagline, whatIsIt, realLife, whenItWorks, etc.) keyed by tactic name

### UI Conventions (style.css — do not break these)
- **Theme**: Dark glassmorphic — `rgba` backgrounds + `backdrop-filter: blur`
- **Neutral landing** (`/`): Charcoal body `radial-gradient(#1e1e1e → #0d0d0d)`, grey accents, no purple
- **League themes**: `body.league-pl` = pink/purple (`#e8668c`, `#37003c`); `body.league-sa` = navy/gold (`#e8b84b`, `#00396e`)
- **Spring easing**: `cubic-bezier(.34, 1.56, .64, 1)` on hover transforms
- **Default accent**: `#7B5EA7` / `#a07dd6` purple family (overridden per league)
- **Selected tactic**: left-accent glow via `box-shadow: -3px 0 0 0 #a07dd6`
- **White crest badge**: `.hero-crest-badge` — 68px white circle wrapping hero crests on Match page; `.crest-badge` — 50px circle on club cards
- **Animated bars**: start at `width:0%`, driven by `requestAnimationFrame` double-rAF pattern
- **Score counter-roll**: `animateCount()` cubic ease-out, fires on page load for non-zero goals
- **Club grid**: `repeat(14, 1fr)`, each card `span 2`, 15th card `grid-column: 2 / span 2` for 7-7-6 stagger
- **Fixture list**: `grid-template-columns: 1fr 1fr` per panel; last odd item `grid-column: 1 / -1; max-width: 50%`

## Engine Data
**Team Base Weights** — ATTACKING `(0.43, 0.28, 0.28)`, CONTROLLING `(0.28, 0.43, 0.28)`, DEFENSIVE `(0.28, 0.28, 0.43)`
**Opponent adjustments** — vs ATTACKING: attack−0.03, defence+0.03 | vs DEFENSIVE: attack+0.03, defence−0.03 | vs CONTROLLING: attack+0.03, control+0.03
**Game phases** — EARLY_MINUTES 0–15 | CLOSING_HALF 16–44 | HALF_TIME 45–50 | BUILD_PHASE 51–60 | TENSION_TIME 61–70 | LATE_GAME 71–87 | STOPPAGE_TIME 88+
**IntentRange** — LOW 0.0–0.33 | MEDIUM 0.34–0.66 | HIGH 0.67–1.0
**7 Tactics** — GEGENPRESSING, HIGH_PRESS, TIKI_TAKA, CONTROL, COUNTER_ATTACK, DIRECT_PLAY, LOW_BLOCK

## League & Team Data
- **Premier League**: team IDs 1–20, 20 clubs, 2024/25 season data
- **Serie A**: team IDs 21–40, 20 clubs, 2024/25 season data
- Club crest images in `frontend/public/images/PremierLeague/` and `frontend/public/images/SerieA/`
- Key squads — Liverpool: CONTROLLING, HIGH stamina, HIGH adaptability | Fulham: CONTROLLING, HIGH stamina, MEDIUM adaptability
- Match 161 = FUL-LIV (Fulham home), Match 180 = LIV-FUL (Liverpool home)

## User Preferences
- Edit files in the MAIN branch (`main`), not the worktree branch
- Main project files at: `C:\Users\sriva\OneDrive\Desktop\java-tactic-recommendation-system\java-tactical-recommendation-engine\`
- Tests must pass before any feature is considered done
- Keep solutions minimal — no extra abstractions, no speculative features
