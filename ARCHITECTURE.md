# Architecture

## Overview

JGaffer is a rule-driven tactical recommendation engine for football (soccer) match management. Built in Java 23 with zero external runtime dependencies, it evaluates live match context — scoreline, minute, team stamina, adaptability, and playing style — to recommend the optimal tactical approach from seven recognized football strategies.

The system is designed around the principle that tactical decisions should be **transparent, deterministic, and explainable** — modelling how real coaching staff think rather than relying on black-box predictions.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLI Interface                            │
│                   (jgafferApplication.java)                     │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PremierLeagueFactory                          │
│          Builds MatchContext & Team objects from CSV            │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              TacticalRecommendationEngine                       │
│      Selects applicable rule → delegates recommendation         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐      │
│   │ EarlyMinute │  │ ClosingHalf  │  │    HalfTime       │      │
│   │  Tactics    │  │   Tactics    │  │    Tactics        │      │
│   └─────────────┘  └──────────────┘  └───────────────────┘      │
│   ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐      │
│   │ BuildPhase  │  │ TensionTime  │  │    LateGame       │      │
│   │  Tactics    │  │   Tactics    │  │    Tactics        │      │
│   └─────────────┘  └──────────────┘  └───────────────────┘      │
│   ┌───────────────────┐                                         │
│   │  StoppageTime     │                                         │
│   │    Tactics        │                                         │
│   └───────────────────┘                                         │
│                                                                 │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Tactic Mapping (CSV)                         │
│       Style × WeightCombination × GamePhase → Tactic            │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                   OpenAI Explanation Service                    │
│         Generates natural language reasoning (never decides)    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Package Structure

```
com.sickton.jgaffer
├── domain/              Core domain models (immutable match state)
├── engine/              Tactical recommendation engine (orchestrator)
├── rules/               Abstract tactical rule framework
│   └── game_phases/     7 concrete rule implementations
├── demoUI/              CLI application and data factory
├── openAIService/       AI-powered explanation generation
├── utility/             CSV parsing, data mapping, and enums
└── input/               CSV data files (tactics, squads, fixtures)
```

| Package | Responsibility |
|---------|---------------|
| `domain` | Defines the core data structures — `MatchContext`, `Team`, `Squad`, `Player`, `TeamIntent`, `TacticKey`, `WeightCombination`. All domain objects are immutable. |
| `engine` | Houses `TacticalRecommendationEngine`, the central orchestrator that selects the correct game phase rule and returns a tactic. |
| `rules` | Contains the abstract `TacticalRule` base class with shared thresholds, utility methods, and the contract that all game phase rules must follow. |
| `rules/game_phases` | Seven concrete implementations, one per game phase, each encoding phase-specific tactical logic and weight adjustments. |
| `demoUI` | The interactive CLI (`jgafferApplication`) and `PremierLeagueFactory` which builds domain objects from CSV data. |
| `openAIService` | Integrates with OpenAI's GPT-4.1 API to generate human-readable explanations for tactical decisions. |
| `utility` | Parsing utilities (`ApplicationParser`, `TacticMapper`), enum definitions (`Tactic`, `GamePhase`, `Style`, `StaminaLevel`, `TeamAdaptability`, `IntentRange`, `Position`). |
| `input` | CSV configuration files that define tactics, squad information, match fixtures, and match-minute contexts. |

---

## Core Components

### MatchContext
Immutable snapshot of a match at a specific moment. Holds the match title, home and away `Team` objects, current scoreline, and the minute of play.

### Team
Represents a football team with its `Squad`, `TeamIntent` (attack/control/defence weights), `StaminaLevel`, and `TeamAdaptability`. The intent is dynamically initialized based on the squad's playing style.

### TeamIntent
Encapsulates three weight values — attack, control, and defence — that represent the team's tactical leaning at any given moment. These weights are adjusted by game phase rules based on match conditions.

### TacticalRecommendationEngine
The central orchestrator. On each call to `recommendTactic()`, it:
1. Iterates through all 7 game phase rules
2. Identifies the single rule whose `applies()` method returns true
3. Validates that exactly one rule matches (defensive check)
4. Delegates to that rule's `recommend()` method

### TacticalRule (Abstract)
Defines the contract for all game phase rules and provides shared utilities:
- **Phase thresholds** — minute boundaries for each game phase
- **Intent classification** — converts raw weight values to LOW/MEDIUM/HIGH ranges
- **Match state queries** — winning, losing, drawing, goal difference
- **Weight clamping** — ensures adjusted values stay within 0.0–1.0

### Game Phase Rules (7 Implementations)
Each rule handles a specific phase of the match:

| Rule | Minutes | Tactical Character |
|------|---------|-------------------|
| `EarlyMinuteTactics` | 0–15 | Conservative adjustments, establishing structure |
| `ClosingHalfTactics` | 16–44 | Building toward halftime advantage |
| `HalfTimeTactics` | 45–50 | Strategic recalibration based on scoreline |
| `BuildPhaseTactics` | 51–60 | Implementing halftime adjustments |
| `TensionTimeTactics` | 61–70 | Responding to fatigue, pivotal decisions |
| `LateGameTactics` | 71–87 | High urgency, scoreline-driven shifts |
| `StoppageTimeTactics` | 88+ | Extreme, outcome-driven decisions |

---

## Design Patterns

### Strategy Pattern
`TacticalRule` serves as the strategy interface. Each game phase implementation encapsulates its own tactical logic, and the engine selects the appropriate strategy at runtime based on the current minute of play.

### Factory Pattern
`PremierLeagueFactory` constructs complex domain objects (`MatchContext`, `Team`) from raw CSV data, abstracting the data assembly process away from the engine and UI layers.

### Template Method
The abstract `TacticalRule` class provides shared utility methods (`adjustWeights`, `getIntent`, `clamp`, `isTeamWinning`) that concrete rules reuse, while requiring them to implement `applies()` and `recommend()` — ensuring consistent behavior with phase-specific logic.

---

## Data Flow

A tactical recommendation follows this path:

```
1. User selects a team and fixture via CLI
        │
2. PremierLeagueFactory retrieves the MatchContext
   for the selected match at a given minute
        │
3. TacticalRecommendationEngine.recommendTactic() is called
   with the MatchContext and the user's Team
        │
4. Engine iterates through 7 TacticalRule implementations
   and finds the one where applies() returns true
        │
5. The matched rule's recommend() method:
   a. Reads the team's base TeamIntent weights
   b. Adjusts weights based on:
      - Scoreline (winning / drawing / losing)
      - Goal difference magnitude
      - Team stamina and adaptability
   c. Clamps adjusted values to 0.0–1.0
   d. Classifies weights into IntentRange (LOW/MEDIUM/HIGH)
   e. Constructs a TacticKey (Style + WeightCombination + GamePhase)
   f. Looks up the corresponding Tactic from the CSV-loaded map
        │
6. Recommended Tactic is returned to the CLI
        │
7. OpenAI service generates a natural language explanation
   (explanation layer only — never influences the decision)
```

---

## Data Layer

The system is configured through four CSV files, making tactical tuning possible without code changes:

| File | Purpose |
|------|---------|
| `tactics.csv` | Maps Style × WeightCombination × GamePhase to a specific Tactic. Contains 83 rows covering all valid combinations across 7 tactics. |
| `SquadInformation.csv` | Defines 20 Premier League teams with manager, playing style, adaptability, and stamina attributes. |
| `PremierLeagueMatches.csv` | Lists match fixtures as team code pairings. |
| `MatchMinuteContext.csv` | Provides match state at specific minutes — scorelines, team pairings, and minute values for simulation. |

---

## External Integrations

### OpenAI Explanation Service
- **Model:** GPT-4.1 via the OpenAI API
- **Purpose:** Generates human-readable reasoning for why a tactic was recommended
- **Architectural boundary:** The AI service operates strictly as an explanation layer. It receives the decision *after* it has been made and has zero influence on the recommendation logic. If the API is unavailable, the system continues to function — the explanation is simply omitted.

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 23 |
| Build Tool | Maven |
| Testing | JUnit Jupiter 5.8.1 |
| External API | OpenAI GPT-4.1 (explanation only) |
| Data Format | CSV |
| Runtime Dependencies | None (pure Java) |
