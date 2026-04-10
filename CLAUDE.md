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

## ML Module — Player Overall Rating
- **Location**: `ml/` at project root (sits alongside `JGaffer/` and `frontend/`)
- **Notebook**: `ml/research/player_research.ipynb` — full pipeline, run cell by cell
- **Raw data**: `ml/data/raw/player_season_24_25_data.csv` — FBref merged stats, Top 5 leagues, 2024/25 (~267 cols)
- **Output**: `ml/output/player_overalls.csv` — schema: `name, team, league, position, role, overall, minutes, 90s`

### Pipeline (in notebook cell order)
1. Load CSV → strip FBref duplicate metadata cols (all cols containing `_stats_*` suffixes)
2. `df_clean = df_clean.copy()` after strip to defragment
3. Extract `PrimaryPos` = first token of `Pos` (e.g. `"DF,MF"` → `"DF"`)
4. Dedup: keep stint with most `Min` per player (handles mid-season transfers)
5. Filter: `Min >= 300`
6. Per-90 normalize all `COUNT_FEATURES` by dividing by `90s` column → `col_p90`
7. Z-score scale full feature matrix, `fillna(0)` before scaling
8. PCA(n=20) → keep first 10 components (~85% variance) → `X_reduced`
9. K-Means(k=12, random_state=42) on `X_reduced`
10. t-SNE(perplexity=40) for visualization
11. Map clusters → role labels → `df_feat['role']`
12. PCA(n=1) within each role group → flip check via `ROLE_ANCHORS` → MinMaxScaler(0–100) → `df_feat['overall']`

### Dataset stats (after filter)
- **1,896 players** | La Liga: 419 | Serie A: 409 | PL: 379 | Ligue 1: 351 | Bundesliga: 338
- `Comp` values: `"eng Premier League"`, `"es La Liga"`, `"it Serie A"`, `"fr Ligue 1"`, `"de Bundesliga"`

### Feature sets
- **RATE_FEATURES** (already per-90): `Sh/90, SoT/90, SCA90, GCA90, G/Sh, G/SoT`
- **COUNT_FEATURES** (divided by `90s`): goals, assists, xG, xAG, progressive actions, shooting, passing, defensive actions, recoveries, aerials, carries, touches
- **GK_FEATURES** (used as-is): `GA90, Save%, CS%, PSxG+/-, #OPA/90, Stp%, AvgDist`
- **PCT_FEATURES** (no division needed): `Cmp%, Tkl%, Succ%, Won%, SoT%`
- Confirmed missing after strip (removed from lists): `Blocks_stats_defense`, `Lost_stats_misc`

### 12 Player Roles (k=12 K-Means)
| Cluster | Role | Key signal |
|---------|------|------------|
| 0 | Ball-playing Centre-back | Int/90 high, Cmp% ~85 |
| 1 | Goalkeeper | 100% GK (merged with cluster 8) |
| 2 | Clinical Striker | Gls/90 ~0.55, Cmp% ~70 |
| 3 | Creative Attacking Midfielder | Gls+Ast ~0.49, mixed FW/MF |
| 4 | Defensive Midfielder | Tkl/90 ~2.31 (highest), large cluster |
| 5 | Defensive Centre-back | Int/90 ~1.12, Cmp% ~86, 97% DF |
| 6 | Wide Midfielder / Winger | Moderate Gls+Ast, Tkl/90 ~1.48 |
| 7 | Target Man / Physical Striker | Gls/90 ~0.27, Cmp% ~69 |
| 8 | Goalkeeper | 100% GK (merged label with cluster 1) |
| 9 | Fullback / Wing-back | Tkl/90 ~1.79, Ast/90 ~0.13, Cmp% ~75 |
| 10 | Elite Wide Forward | Gls/90 ~0.49, Ast/90 ~0.31 — only 39 players |
| 11 | Box-to-box Midfielder | Tkl/90 ~2.18, Cmp% ~85 |

### Role Anchors (PC1 flip detection)
```
Clinical Striker / Elite Wide Forward / Target Man  → Gls_p90
Creative Attacking Midfielder                        → xAG_p90
Wide Midfielder / Winger & Box-to-box               → SCA90
Defensive Midfielder                                 → Tkl_p90
Ball-playing CB & Defensive CB                      → Int_p90
Fullback / Wing-back                                 → PrgP_p90
Goalkeeper                                           → Save%
```
If `pca_grp.components_[0][anchor_idx] < 0` → negate scores before MinMaxScaler.

## User Preferences
- Edit files in MAIN branch | project root: `C:\Users\sriva\OneDrive\Desktop\java-tactic-recommendation-system\java-tactical-recommendation-engine\`
- Tests must pass before any feature is done | Keep solutions minimal — no extra abstractions, no speculative features
