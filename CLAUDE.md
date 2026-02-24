# JGaffer — Tactical Recommendation Engine

## Key File Locations
- **Backend**: `JGaffer/src/main/java/com/sickton/jgaffer/`
  - `engine/` TacticalRecommendationEngine | `rules/game_phases/` 7 phase rules
  - `domain/` Team, TeamIntent, Squad | `utility/` TacticMapper, ApplicationParser, FileStorage
  - `service/` MatchService, GameSimulator | `service/analytics/` AnalyticsService
  - `web/` ApiController, AnalyticsController, SpaController
  - `demoUI/` LeagueDataFactory, PremierLeagueFactory | `research/` PCAResearch
- **Resources**: `JGaffer/src/main/resources/` — `PremierLeague/`, `SerieA/`, `tactics.csv`, `static/`
- **Frontend**: `frontend/src/` — pages/, components/, hooks/, constants/, types.ts, App.tsx
- **Stylesheet**: `frontend/public/style.css` (glassmorphic dark theme — do NOT break)

## Commands
```
# Tests — run from java-tactical-recommendation-engine/
JAVA_HOME="C:\Program Files\Java\jdk-23" "C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd" test -f JGaffer/pom.xml

# Frontend build — outputs to JGaffer/src/main/resources/static/
cd frontend && npm run build
```
`Analytics.css` imports directly in `Analytics.tsx` — do NOT move to `public/style.css` (fixed filename = aggressively cached).

## REST API (`/api/*`) — `league` always `"PL"` or `"SA"`
| Method | Path | Key params | Returns |
|--------|------|-----------|---------|
| GET | `/api/clubs` | `league` | `{teams:[{id,name}]}` |
| GET | `/api/fixtures` | `teamId, league` | home/away fixture lists |
| GET | `/api/match` | `matchId, teamId, league` | match context + tactics |
| POST | `/api/recommend` | `teamId, matchId, minute, userTactic, league` | recommendation + explanation |
| POST | `/api/simulate` | same | full SimulationData |
| POST | `/api/simulate/phase` | `+fromMinute, fromHomeGoals, fromAwayGoals, tacticsJson` | PhaseResult |
| GET | `/api/analytics/win-rate-by-tactic` | `league` | DB-aggregated TacticAnalytics[] |
| GET | `/api/analytics/league-normalizer` | `league, tactic, samplesPerTeam=15, iterationsPerSample=9` | LeagueNormalizerResult |
| GET | `/api/analytics/phase-normalizer` | same | PhaseNormalizerResult (7 phases) |

`SpaController` forwards all React routes (`/, /clubs, /fixtures, /match, /result, /simulation, /analytics`) → `index.html`.

## Backend Architecture
- **Engine pattern**: Strategy — `TacticalRule` → 7 phase impls. Lookup: Style × WeightCombination × GamePhase → Tactic (CSV)
- **WeightCombination** constructor order: `(attack, defence, control)` — NOT alphabetical
- **TeamIntent**: `TeamIntent(Squad)` style-bias fallback; `TeamIntent(double atk, double def, double ctrl)` PCA-direct bypass
- **Team**: 3-arg standard; 6-arg `(Squad, StaminaLevel, Adaptability, atkW, defW, ctrlW)` for PCA weights
- **FileStorage**: `NO_WEIGHT=-1.0` sentinel; `hasCustomWeights()` → true when all 3 weight cols present in CSV
- **ApplicationParser**: `split(",", -1)` — cols 8-10 optional; empty = NO_WEIGHT fallback
- **LeagueDataFactory**: `hasCustomWeights() ? new Team(6-arg) : new Team(3-arg)` for both home + away teams

## PCA Research Integration
- `research/PCAResearch.java` — standalone `main()`, NOT Spring; run directly from IntelliJ
- CSVs: `resources/research/PremierLeague_PCA.csv`, `resources/research/SerieA_PCA.csv` (17 cols, 20 teams each)
- Pipeline: standardise (z-score) → `PCA.fit(Z)` → `getProjection(3)` → `varianceProportion()` / `loadings()` → team weights
- Flip flags (confirmed for both leagues): `flipAtk=true, flipDef=true, flipCtrl=false`
- **Both leagues fully populated**: `SquadInformation.csv` cols 8-10 (`atk_weight,def_weight,ctrl_weight`) written for PL + SA
- PCA weights used as starting `TeamIntent` values, overriding style-bias; affect `WeightCombination` bucket → tactic recommendations

## Engine Data
- **Style-bias fallback** (when no PCA weights): ATTACKING `(0.43,0.28,0.28)` | CONTROLLING `(0.28,0.43,0.28)` | DEFENSIVE `(0.28,0.28,0.43)`
- **Opponent adjustments**: vs ATTACKING atk−0.03,def+0.03 | vs DEFENSIVE atk+0.03,def−0.03 | vs CONTROLLING atk+0.03,ctrl+0.03
- **IntentRange**: LOW 0–0.33 | MEDIUM 0.34–0.66 | HIGH 0.67–1.0
- **Phases**: EARLY_MINUTES 0–15 | CLOSING_HALF 16–44 | HALF_TIME 45–50 | BUILD_PHASE 51–60 | TENSION_TIME 61–70 | LATE_GAME 71–87 | STOPPAGE_TIME 88+
- **7 Tactics**: GEGENPRESSING, HIGH_PRESS, TIKI_TAKA, CONTROL, COUNTER_ATTACK, DIRECT_PLAY, LOW_BLOCK

## League & Team Data
- PL: IDs 1–20 | SA: IDs 21–40 | both 2024/25 | crests in `frontend/public/images/{PremierLeague,SerieA}/`
- DB: `jdbc:postgresql://localhost:5432/jgaffer` (default) | `jgaffer_test_analytics` (test-analytics profile)
- Monte Carlo default: 20 teams × 15 samples × 9 iters = 2,700 trials → ±1.88% @ 95% CI

## Frontend Conventions (do not break)
- Neutral pages (`/, /analytics`): remove `league-pl`/`league-sa` body class on mount; charcoal radial gradient
- League themes: `.league-pl` pink/purple `#e8668c` | `.league-sa` navy/gold `#e8b84b`
- `winRate` from backend = 0–100 already — do NOT multiply by 100 on the frontend
- Always use `formatTactic()` when displaying tactic enums | Analytics CSS namespace: `.an-*`
- Animated bars: double-`requestAnimationFrame` pattern; start `width:0%` | Spring easing: `cubic-bezier(.34,1.56,.64,1)`

## User Preferences
- Edit files in MAIN branch | project root: `C:\Users\sriva\OneDrive\Desktop\java-tactic-recommendation-system\java-tactical-recommendation-engine\`
- Tests must pass before any feature is done | Keep solutions minimal — no extra abstractions, no speculative features
