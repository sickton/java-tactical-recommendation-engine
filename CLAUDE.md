# JGaffer — Tactical Recommendation Engine

## Project Structure
```
java-tactical-recommendation-engine/
├── ARCHITECTURE.md
├── JGaffer/
│   ├── pom.xml
│   ├── src/main/java/com/sickton/jgaffer/
│   │   ├── domain/          Core immutable models
│   │   ├── engine/          TacticalRecommendationEngine
│   │   ├── rules/           Abstract TacticalRule + 7 game_phases/
│   │   ├── demoUI/          CLI (jgafferApplication) + PremierLeagueFactory
│   │   ├── openAIService/   OpenAIClient + TacticalExplanationService
│   │   ├── utility/         TacticMapper, ApplicationParser
│   │   └── input/           CSV data files
│   └── src/main/resources/  tactics.csv (classpath resource)
└── src/test/java/...        TacticalRecommendationTest (3 tests)
```

## Running Tests
```
JAVA_HOME="C:\Program Files\Java\jdk-23" "C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd" test -f JGaffer/pom.xml
```
Run from: `java-tactical-recommendation-engine/` (project root, parent of JGaffer/)

## Key Architecture Facts
- **Pattern**: Strategy — TacticalRule (abstract) → 7 concrete phase implementations
- **Engine entry points**: `recommendTactic()` → Tactic, `recommendWithDetails()` → TacticRecommendation (tactic + confidence 0-100)
- **Tactic lookup**: Style × WeightCombination × GamePhase → Tactic (CSV-driven)
- **WeightCombination constructor order**: `(attack, defence, control)` — NOT alphabetical
- **TacticMapper**: loads `/tactics.csv` via `getResourceAsStream` (classpath, not filesystem path)
- **Opponent awareness**: `applyOpponentStyleAdjustments()` in TacticalRule base class, called in all 7 rules before stamina scaling
- **Confidence**: `computeConfidence(attack, control, defence)` in TacticalRule — distance from nearest boundary (0.33, 0.66), scaled 0-100

## Team Base Weights (TeamIntent.java)
| Style | Attack | Control | Defence |
|-------|--------|---------|---------|
| ATTACKING | 0.43 | 0.28 | 0.28 |
| CONTROLLING | 0.28 | 0.43 | 0.28 |
| DEFENSIVE | 0.28 | 0.28 | 0.43 |

## IntentRange Thresholds
- LOW: 0.0 – 0.33
- MEDIUM: 0.34 – 0.66
- HIGH: 0.67 – 1.0

## Opponent Style Adjustments (applyOpponentStyleAdjustments)
- vs ATTACKING:   attack -=0.03, defence +=0.03
- vs DEFENSIVE:   attack +=0.03, defence -=0.03
- vs CONTROLLING: attack +=0.03, control +=0.03

## Game Phases
| Phase | Minutes |
|-------|---------|
| EARLY_MINUTES | 0–15 |
| CLOSING_HALF | 16–44 |
| HALF_TIME | 45–50 |
| BUILD_PHASE | 51–60 |
| TENSION_TIME | 61–70 |
| LATE_GAME | 71–87 |
| STOPPAGE_TIME | 88+ |

## Key Squad Data (SquadInformation.csv)
- Liverpool: CONTROLLING, HIGH stamina, HIGH adaptability
- Fulham: CONTROLLING, HIGH stamina, MEDIUM adaptability
- Match 161 = FUL-LIV (Fulham home), Match 180 = LIV-FUL (Liverpool home)

## Features Built
1. Opposition-aware tactics — all 7 phases, shared utility in TacticalRule
2. Confidence score — TacticRecommendation wraps Tactic + int confidence
3. Enriched OpenAI prompt — phase, both styles, stamina, score, confidence
4. Classpath CSV loading — TacticMapper uses getResourceAsStream (not file path)

## Next on Roadmap (Feature #2)
Formations as Tactical Context:
- Formation.java and Position.java exist in domain/ but commented out in Team.java
- Plan: activate Formation in Team, add column to SquadInformation.csv, add tactic→formation lookup

## User Preferences
- Edit files in the MAIN branch (`main`), not the worktree branch
- Main project files at: `C:\Users\sriva\OneDrive\Desktop\java-tactic-recommendation-system\java-tactical-recommendation-engine\`
- Tests must pass before any feature is considered done
- Keep solutions minimal — no extra abstractions, no speculative features
