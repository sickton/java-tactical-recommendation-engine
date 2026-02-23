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
│   │   ├── service/analytics/        AnalyticsService (Monte Carlo + DB aggregation)
│   │   ├── utility/                  TacticMapper, ApplicationParser, FileStorage
│   │   ├── persistence/
│   │   │   ├── dto/                  TacticAnalytics, LeagueNormalizerResult, PhaseNormalizerResult, PhasePerformance
│   │   │   ├── entity/               MatchDecision, TacticWinStats (projection)
│   │   │   └── repository/           MatchDecisionRepository
│   │   └── web/                      ApiController + AnalyticsController + SpaController
│   └── src/main/resources/
│       ├── tactics.csv               Classpath tactic lookup table
│       ├── application.properties    Default profile — jgaffer DB, ddl-auto=update
│       ├── PremierLeague/            MatchMinuteContext.csv, PremierLeagueMatches.csv, SquadInformation.csv
│       ├── SerieA/                   MatchMinuteContext.csv, SerieAMatches.csv, SquadInformation.csv
│       └── static/                   Built React SPA (index.html + assets/index-*.js + assets/index-*.css + images/)
├── frontend/                         React 18 + TypeScript + Vite source
│   ├── public/
│   │   ├── style.css                 Global stylesheet (glassmorphic dark theme)
│   │   └── images/
│   │       ├── PremierLeague/        20 club crest PNGs
│   │       └── SerieA/               20 club crest PNGs
│   └── src/
│       ├── main.tsx                  Entry point
│       ├── App.tsx                   BrowserRouter + 7 Routes (includes /analytics)
│       ├── types.ts                  All shared TypeScript interfaces
│       ├── components/               Header.tsx, Footer.tsx, TeamCrest.tsx
│       ├── constants/                crests.ts, teamCodes.ts, tacticGuide.ts
│       ├── hooks/                    useLeagueTheme.ts, usePageTitle.ts
│       ├── pages/                    LeagueSelect, ClubSelect, Fixtures, Match, Result, Simulation, Analytics
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
Vite outputs to `JGaffer/src/main/resources/static/`. Produces 3 files: `index.html`, `assets/index-*.css` (hashed), `assets/index-*.js` (hashed). The `public/` directory (style.css + images/) is copied after clearing, so those files survive `emptyOutDir: true`.

**CSS architecture note**: `Analytics.css` is imported directly in `Analytics.tsx` (`import './Analytics.css'`). Vite bundles it into the hashed `assets/index-*.css` file, which bypasses browser cache. Do NOT add Analytics styles to `public/style.css` — that file has a fixed filename and is aggressively cached.

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
Forwards all React Router paths (`/`, `/clubs`, `/fixtures`, `/match`, `/result`, `/simulation`, `/analytics`) to `static/index.html` so direct URL access and browser refresh work.

## 🔬 Analytics & Tactical Research Layer

JGaffer includes an advanced tactical analytics and meta-evaluation layer built on top of the simulation engine.

### Architecture

```
service/analytics/
    AnalyticsService               — DB aggregation + parallel Monte Carlo sweeps

persistence/
    dto/
        TacticAnalytics            — startTactic, total, wins, losses, draws, winRate
        LeagueNormalizerResult     — league, tactic, totalSimulations, wins, losses, draws, winRate
        PhaseNormalizerResult      — league, tactic, phases: PhasePerformance[]
        PhasePerformance           — phase, totalSimulations, wins, losses, draws, winRate
    entity/
        MatchDecision              — persisted simulation record (outcome: WIN/LOSS/DRAW)
        TacticWinStats             — JPQL projection: startTactic, total, wins, losses, draws
    repository/
        MatchDecisionRepository    — JPQL groups by startTactic, counts WIN/LOSS/DRAW outcomes

web/
    AnalyticsController            — /api/analytics/*
```

All analytics endpoints under `/api/analytics/*`.

---

## 1️⃣ Historical Analytics (DB-Based)

**Endpoint**
```
GET /api/analytics/win-rate-by-tactic?league=PL|SA
```

**What it returns**
Per-tactic aggregation from logged `match_decisions`: total simulations, wins, losses, draws, win rate. Filtered by league.

**Data flow**
`MatchDecisionRepository.getWinRateByTactic()` → JPQL aggregation (SUM CASE for WIN, LOSS, DRAW) → `TacticWinStats` projection → `TacticAnalytics` DTO

**Characteristic**: Reflects only the matches the user has personally run. Biased by the teams and fixtures they happened to test.

---

## 2️⃣ League Normalizer (Monte Carlo)

**Endpoint**
```
GET /api/analytics/league-normalizer?league=PL|SA&tactic=HIGH_PRESS&samplesPerTeam=15&iterationsPerSample=9
```

**Method**
- Parallelised via `parallelStream()` over all 20 teams (ForkJoinPool common pool)
- Per team: `samplesPerTeam` random matches × `iterationsPerSample` simulations each
- Accumulates with `LongAdder` (no contention); RNG via `ThreadLocalRandom.current()` (per-thread, no locking)

**Statistical quality at default params**
`20 teams × 15 samples × 9 iterations = 2,700 trials → ±1.88% margin of error @ 95% CI`

**Purpose**: Bias-free league-wide win rate — removes team, opponent, and fixture bias.

---

## 3️⃣ Phase Normalizer (Monte Carlo)

**Endpoint**
```
GET /api/analytics/phase-normalizer?league=PL|SA&tactic=HIGH_PRESS&samplesPerTeam=15&iterationsPerSample=9
```

**Method**
- Builds 20 teams × 7 phases = **140 work items**, all processed via `parallelStream()`
- Phase stats stored in `ConcurrentHashMap<String, LongAdder[]>` (index 0=wins, 1=losses, 2=draws)
- Minute sampling is phase-constrained: `minuteForPhase(phase, ThreadLocalRandom.current())`

**Phase minute ranges**
```
EARLY_MINUTES  0–15    CLOSING_HALF  16–44   HALF_TIME     45–50
BUILD_PHASE    51–60   TENSION_TIME  61–70   LATE_GAME     71–87   STOPPAGE_TIME 88–92
```

**Statistical quality at default params**
`20 teams × 15 samples × 9 iterations = 2,700 trials per phase → ±1.88% per phase @ 95% CI`
Total simulations = 2,700 × 7 = 18,900 — runtime kept low by full parallelism across 140 work items.

**Purpose**: Reveals a tactic's timing profile across all 7 game phases.

---

## Monte Carlo — Statistical Reference

| Trials (n) | Margin of Error (±, 95% CI) |
|---|---|
| 180 (old default) | ±7.3% |
| 600 | ±4.0% |
| 1,067 | ±3.0% |
| **2,700 (current default)** | **±1.88% ✓** |
| 2,401 | ±2.0% (threshold) |

Formula: `±1.96 × √(0.25 / n)` (worst-case p=0.5)

---

## Runtime Profiles

**Default Profile** (`application.properties`)
- `spring.datasource.url=jdbc:postgresql://localhost:5432/jgaffer`
- `spring.jpa.hibernate.ddl-auto=update`
- Used for Monte Carlo normalizers and real simulations

**test-analytics Profile** (`application-test-analytics.properties`)
- Uses `jgaffer_test_analytics` DB
- `ddl-auto=create`, seeds 100 simulations
- Used for historical analytics preview/testing

Monte Carlo normalizers do NOT depend on database state.

---

## Design Principles

- Simulation remains pure — no engine modification
- Normalizers do NOT persist results
- Clean layering: `Controller → AnalyticsService → MatchService.simulate()`
- Thread safety: `ThreadLocalRandom` (per-thread RNG), `LongAdder` (concurrent counters), `ConcurrentHashMap` (phase map), `List.copyOf()` (immutable matchIds snapshot)

---

## Frontend

### Navigation Flow
```
GET /  →  LeagueSelect  →  GET /clubs?league=PL|SA
       →  ClubSelect    →  GET /fixtures?teamId=X&league=PL|SA
       →  Fixtures      →  GET /match?matchId=X&teamId=Y&league=PL|SA
       →  Match         →  POST /api/recommend  →  Result
                        →  POST /api/simulate   →  Simulation
Header nav link  →  /analytics
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
| `/analytics` | `Analytics` | 3-panel layout; neutral theme; auto-loads historical on mount; per-panel tactic selectors |

### Analytics Page — 3-Panel Layout

`Analytics.tsx` + `Analytics.css` (component-scoped, `.an-*` class namespace).

**Panel 1 — Historical Win Rates** (auto-loads, no tactic picker)
- Fetches on mount and on league change from `/api/analytics/win-rate-by-tactic`
- Each tactic rendered as a card: name centred, segmented W/D/L bar, `X% W · X% D · X% L` stats
- Animated via double-`requestAnimationFrame` pattern; three separate ref arrays (`winRefs`, `drawRefs`, `lossRefs`)
- Bar segments: green = wins, amber = draws, red = losses (`.an-hist-seg-win/draw/loss`)

**Panel 2 — League Normalizer** (per-panel tactic picker + Run button)
- Independent `leagueTactic` state (default `HIGH_PRESS`)
- Displays big win-rate number + animated fill bar + W/D/L breakdown
- Selecting a new tactic clears the previous result

**Panel 3 — Phase Normalizer** (per-panel tactic picker + Run button)
- Independent `phaseTactic` state (default `HIGH_PRESS`)
- Renders 7 animated phase bars (`.an-bars` / `.an-bar-row` layout, label + track + fill + pct + count)
- Each bar colour-coded by win rate threshold (≥55% green, ≥40% amber, <40% red)

**CSS classes reference**
```
.an-page          flex column, fills viewport height
.an-hero          page title section
.an-top-bar       league toggle row
.an-panels        3-col grid (1fr 1fr 1fr), fills remaining height
.an-panel         individual card (rgba white-tint bg, purple border, ::before top accent)
.an-panel-head    panel header (title + badge)
.an-panel-controls  tactic pill row + run button (panels 2 & 3 only)
.an-panel-pill    per-panel tactic selector pill (.active = purple)
.an-panel-run-btn run button
.an-panel-body    scrollable content area
.an-hist-cards    column of tactic cards (Panel 1)
.an-hist-card     individual tactic card
.an-hist-seg-track  segmented W/D/L bar container
.an-hist-seg-win/draw/loss  colour segments
.an-bars          column of phase/tactic bars (Panels 2 & 3)
.an-bar-row       grid: label + track + pct + meta
```

**Responsive breakpoints**
- `<1000px`: 2-col panels, Panel 1 spans full width (max-height 220px)
- `<640px`: 1-col panels, Panel 1 uncapped

### Components
- **`Header`** — Pitch SVG icon (always `#4ade80`), "JGaffer" logo, league badge, "Analytics" nav link (active state via `useLocation`)
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

### TypeScript Interfaces (types.ts)
```typescript
TacticAnalytics       { startTactic, total, wins, losses, draws, winRate }
LeagueNormalizerResult { league, tactic, totalSimulations, wins, losses, draws, winRate }
PhasePerformance      { phase, totalSimulations, wins, losses, draws, winRate }
PhaseNormalizerResult  { league, tactic, phases: PhasePerformance[] }
```
`winRate` from backend is always **0–100** (already a percentage). Do NOT multiply by 100 on the frontend.

### UI Conventions (style.css — do not break these)
- **Theme**: Dark glassmorphic — `rgba` backgrounds + `backdrop-filter: blur`
- **Neutral landing** (`/`, `/analytics`): Charcoal body `radial-gradient(#1e1e1e → #0d0d0d)`, grey accents, no purple. `document.body.classList.remove('league-pl', 'league-sa')` on mount.
- **League themes**: `body.league-pl` = pink/purple (`#e8668c`, `#37003c`); `body.league-sa` = navy/gold (`#e8b84b`, `#00396e`)
- **Spring easing**: `cubic-bezier(.34, 1.56, .64, 1)` on hover transforms
- **Default accent**: `#7B5EA7` / `#a07dd6` purple family (overridden per league)
- **Selected tactic**: left-accent glow via `box-shadow: -3px 0 0 0 #a07dd6`
- **White crest badge**: `.hero-crest-badge` — 68px white circle wrapping hero crests on Match page; `.crest-badge` — 50px circle on club cards
- **Animated bars**: start at `width:0%`, driven by double-`requestAnimationFrame` pattern
- **Score counter-roll**: `animateCount()` cubic ease-out, fires on page load for non-zero goals
- **Club grid**: `repeat(14, 1fr)`, each card `span 2`, 15th card `grid-column: 2 / span 2` for 7-7-6 stagger
- **Fixture list**: `grid-template-columns: 1fr 1fr` per panel; last odd item `grid-column: 1 / -1; max-width: 50%`
- **Panel cards** (analytics): `rgba(255,255,255,0.035)` white-tint background with `rgba(123,94,167,0.28)` purple border — visible on neutral dark background

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
