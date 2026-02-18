# JGaffer

### A Rule-Driven Tactical Recommendation Engine for Football

[![Java](https://img.shields.io/badge/Java-23-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-Build-blue?style=flat-square&logo=apachemaven)](https://maven.apache.org/)
[![JUnit](https://img.shields.io/badge/JUnit-5.8.1-green?style=flat-square&logo=junit5)](https://junit.org/junit5/)
[![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4.1-purple?style=flat-square&logo=openai)](https://openai.com/)


---

JGaffer is a Java-based decision engine that recommends tactical changes during a football (soccer) match. Given a snapshot of the match state — scoreline, minute, team stamina, adaptability, and playing style — it evaluates a set of phase-specific rules and recommends the most appropriate tactic.

It acts as an **assistant manager in the dugout**: you suggest a tactic, JGaffer either agrees or proposes an alternative with an AI-powered explanation.

> The current version focuses on modelling real-life coaching decisions through a rule-based approach. Machine learning and match simulation are planned for future iterations.

---

## Quick Start

### Prerequisites

- Java 23
- Maven
- OpenAI API key (optional — for AI-generated explanations)

### Run

```bash
# Set your API key (optional)
set OPENAI_API_KEY=your-key-here          # Windows
export OPENAI_API_KEY=your-key-here       # Mac/Linux

# Build and run
cd JGaffer
mvn compile exec:java -Dexec.mainClass="com.sickton.jgaffer.demoUI.jgafferApplication"
```

Or run `jgafferApplication.java` directly from your IDE.

### Test

```bash
cd JGaffer
mvn test
```

---

## How It Works

```
  Select Team → Select Fixture → Random Minute Generated
                                        │
                                        ▼
                              ┌─────────────────┐
                              │  Match Context  │
                              │  (scoreline,    │
                              │   minute, team  │
                              │   attributes)   │
                              └────────┬────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │  Rule Engine    │
                              │  (7 game phase  │
                              │   rules)        │
                              └────────┬────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │  Recommended    │
                              │  Tactic         │
                              └────────┬────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │  AI Explanation │
                              │  (GPT-4.1)      │
                              └─────────────────┘
```

1. You pick a Premier League team and fixture
2. A random match minute is generated, pulling the scoreline and match state
3. You suggest a tactic for your team
4. The engine evaluates which game phase applies, adjusts tactical intent weights based on scoreline, stamina, and adaptability, then looks up the optimal tactic
5. JGaffer compares your suggestion to its recommendation and provides an AI-generated explanation

---

## Problem Statement

During a football match, managers continuously adjust team tactics based on factors such as:
- Scoreline
- Minutes remaining
- Player fatigue
- Team identity and playing style
- Team adaptability
- Opponent behavior

JGaffer models this decision-making process as a **rule-based engine**.
Given a snapshot of the match state, the system evaluates a predefined set of tactical rules and recommends the most appropriate tactical approach for the team in that context.

---

## Tactics

The engine recommends one of seven tactical strategies based on the current match context:

| Tactic | Strategy |
|--------|----------|
| **Gegenpressing** | Immediate aggressive pressure after losing possession |
| **High Press** | Sustained pressure during opposition build-up |
| **Tiki-Taka** | Short, quick passing with positional rotation |
| **Control** | Structured possession with disciplined positioning |
| **Counter Attack** | Exploiting transitions after regaining possession |
| **Direct Play** | Quick vertical progression with long passes |
| **Low Block** | Deep defensive positioning with compact lines |

### Tactic Details

- **Gegenpressing** — Apply immediate and aggressive pressure after losing possession. The objective is to win the ball back within seconds in advanced areas of the pitch. Relies on coordinated pressing triggers, high stamina, and compact vertical spacing to suffocate opponents and create high-probability scoring opportunities.

- **High Press** — Consistently apply pressure high up the pitch during opposition build-up phases. Unlike situational pressing, this maintains sustained attacking pressure to disrupt structured play, force rushed decisions, and recover possession in dangerous zones.

- **Tiki-Taka** — Prioritize short, quick passing and positional rotation to maintain fluid ball circulation. Creates space through movement, controls tempo, and breaks defensive lines through precision and patience rather than direct vertical play.

- **Control** — Manage the tempo of the game through structured possession and disciplined positioning. Balances defensive stability with measured attacking progression, reducing risk while maintaining territorial dominance.

- **Counter Attack** — Exploit transitional moments immediately after regaining possession. Rapid vertical progression into open spaces before the opposition can reorganize defensively. Emphasizes pace, direct passing, and attacking overloads.

- **Direct Play** — Advance the ball quickly into attacking areas using long passes, aerial distribution, and minimal build-up phases. Reduces midfield circulation and focuses on territorial gain and quick goal-scoring opportunities.

- **Low Block** — Defend deep within the team's defensive third with compact defensive lines. Prioritizes space denial, central protection, and forcing opponents into low-quality wide areas. Typically used when protecting a lead or absorbing sustained pressure.

---

## Game Phases

The engine divides a match into seven tactical phases, each with distinct characteristics that influence the recommendation:

| Phase | Minutes | Focus |
|-------|---------|-------|
| **Early Minutes** | 1–15 | Establishing structure, tempo, and territorial control |
| **Closing Half** | 16–44 | Intensifying adjustments as halftime approaches |
| **Half Time** | 45–50 | Strategic recalibration window |
| **Build Phase** | 51–60 | Implementing and evaluating halftime changes |
| **Tension Time** | 61–70 | Managing fatigue and match volatility |
| **Late Game** | 71–87 | High urgency, scoreline-driven decisions |
| **Stoppage Time** | 88+ | Extreme, outcome-driven tactical shifts |

### Phase Details

- **Early Minutes (1–15)** — Focuses on establishing structure, tempo, and territorial control. Teams avoid high-risk tactical shifts early on, instead assessing opponent shape and match rhythm before committing to aggressive strategies.

- **Closing Half (16–44)** — Tactical adjustments intensify as halftime approaches. Managers may push for a psychological advantage before the break or stabilize the team if under pressure. Decisions here often influence halftime team talks and second-half planning.

- **Half Time (45–50)** — The structured adjustment window. Tactical recalibration is at its highest importance, as managers reflect on first-half performance and deliberately shift intent, formation emphasis, or pressing intensity.

- **Build Phase (51–60)** — The early second-half period where teams implement halftime adjustments. Managers evaluate whether changes are producing the desired effect and may refine strategy without entering high-risk territory.

- **Tension Time (61–70)** — Match volatility increases as fatigue begins to influence structure and decision-making. Tactical shifts aim to regain control, protect energy levels, or prepare for an aggressive final push.

- **Late Game (71–87)** — Urgency significantly increases. Scoreline context heavily influences tactical aggression or conservatism. Risk tolerance rises for teams chasing the game, while defensive solidity becomes critical for teams protecting a lead.

- **Stoppage Time (88+)** — The highest-pressure window of the match. Tactical decisions become extreme and highly outcome-driven. Teams may commit fully to attacking overloads or retreat into deep defensive structures depending on the scoreline.

---

## Project Structure

```
JGaffer/
├── src/main/java/com/sickton/jgaffer/
│   ├── domain/            Core models (MatchContext, Team, Squad, TeamIntent)
│   ├── engine/            Tactical recommendation engine
│   ├── rules/             Abstract rule framework + 7 game phase implementations
│   ├── demoUI/            CLI application and data factory
│   ├── openAIService/     AI explanation integration
│   ├── utility/           Parsing, mapping, and enum definitions
│   └── input/             CSV configuration files
├── src/test/java/         JUnit test suite
└── pom.xml                Maven build configuration
```

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 23 |
| Build | Maven |
| Testing | JUnit Jupiter 5.8.1 |
| AI Integration | OpenAI GPT-4.1 |
| Data | CSV-driven configuration |
| Dependencies | None at runtime (pure Java) |

---

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture](ARCHITECTURE.md) | System design, design patterns, data flow, and component breakdown |
| [CLI Guide](CLI_GUIDE.md) | How to run and use the command-line interface |

---

## Roadmap

This is a preliminary version with a solid architectural foundation. Planned enhancements include:

- [ ] Opponent modelling — factor in the opposing team's current tactic
- [ ] Dynamic stamina — stamina degrades over the course of a match
- [ ] Player-level granularity — individual player attributes influencing recommendations
- [ ] Formation support — tactical recommendations tied to specific formations
- [ ] Machine learning layer — learn from historical match data
- [ ] Web UI — move beyond the CLI to a browser-based interface
- [ ] Expanded test coverage — comprehensive unit and integration tests

---

## Contributing

This project is actively under development. If you're interested in contributing or have ideas for features, feel free to open an issue or reach out.

