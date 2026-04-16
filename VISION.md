# JGaffer — Vision Document
*Last updated: 2026-04-16 | Branch: dev*

---

## What JGaffer Is

JGaffer is an AI-powered football intelligence system for casual fans. It does not predict match results or produce statistics. It explains **why moments in a football match feel the way they do** — and then lets the user participate in solving the tactical problem.

The core belief: casual fans understand football more deeply when they can *feel* the tactical tension, not just read about it.

---

## The Problem

Football is difficult to understand for casual fans because:
- Tactical patterns are subtle and build gradually
- Broadcasts rarely explain structural shifts
- Most analytics tools assume expert knowledge

Casual fans see players passing randomly.
Experts see patterns of pressure, control, and space creation.

JGaffer bridges that gap — not by lecturing, but by making the fan an active participant.

---

## The Full User Experience

### Step 1 — Pick your team and moment type
User selects a league, a club, and a theme (dramatic, dominant, comeback, under pressure, turning point, surprise). The system surfaces 5–10 real match moments matching that theme.

### Step 2 — Select a moment
Each moment shows:
- A headline (e.g. "Arsenal's relentless press suffocates Liverpool")
- The match, minute, and score
- A short narrative in plain English
- A football concept being illustrated (e.g. "High Press")

Two panels: **What it is** and **What to Notice**.

A **Dive** button takes the user deeper.

### Step 3 — The Tactical Puzzle (new feature)
The screen splits into two panels rendered on a pitch diagram:

**Left panel — Opposition's tactic**
A passing/pressing network showing how the opposition (e.g. Arsenal) is structured. Nodes represent player positions. Edges show pressing connections and passing routes. This is generated from historical data — it shows how teams in this formation and game phase typically apply pressure.

**Right panel — User tries to break it**
The user's team (e.g. Liverpool) is shown with position nodes on the pitch. The user builds a passing sequence by clicking through nodes in order. Each click draws an arrow to the next position. An undo button lets them backtrack.

When satisfied, the user hits **Submit**.

### Step 4 — The Result
Three panels shown side by side:

| User's Sequence | System's Suggested Sequence | What Actually Happened* |
|---|---|---|
| The path the user drew | The path the model thinks is most likely to succeed | Formation-level ground truth: what teams in this exact situation typically do, and how often it worked |
| Explanation of why it works or doesn't | Explanation of why this is the stronger route | Outcome data: success rate of this approach |

*See architectural note below on ground truth.

---

## Technical Pipeline

```
StatsBomb open event data
        ↓
Pass extraction pipeline
(from_position → to_position, labeled by formation and game phase)
        ↓
Formation-based pass probability matrices
(one per formation: F_4_3_3.json, F_4_2_3_1.json, etc.)
        ↓
Press escape outcome model
(trained on: did this sequence successfully escape pressure?)
        ↓
Context adjustment layer
(modifies base matrix using: style, stamina, game phase, score diff)
        ↓
Graph generator
(outputs: nodes with pitch coordinates + weighted edges)
        ↓
React pitch visualization
(SVG pitch, click-to-connect interaction)
        ↓
LLM explanation
(narrates the structural reasoning in plain English)
```

---

## The Data Foundation

### StatsBomb Open Data
Used exclusively to learn **how passing works positionally** — not league-specific, not team-specific. The question being answered: given a formation and game phase, how do positions connect to each other?

Pass events are extracted and labeled:
- `from_position` → `to_position` (abstract roles: CB, CDM, LW, ST, etc.)
- Formation detected from tactics events in the same match
- Outcome labeled: did the sequence escape pressure successfully, or did possession break down?

This produces two artefacts:
1. **Probability matrices** — for each formation, `P(to_position | from_position)`
2. **Labeled training dataset** — for the outcome model

### Why StatsBomb data transfers across leagues
The matrices are formation-conditioned, not league-conditioned. A 4-3-3 POSSESSION team passes with the same structural logic whether it is in La Liga or the Premier League. The formation is the abstraction that makes the data portable.

### Existing Squad/Match Data (PL and Serie A)
Used for the moment discovery layer (RAG) and for the context adjustment layer. Team style, stamina, adaptability, and formation are already ingested. These attributes modify the base probability matrices at query time.

---

## The Press Escape Outcome Model

This is what separates the system's suggested sequence from being "just the most common path."

StatsBomb pass events include an `under_pressure` boolean. By tracking what happens in the 3–5 events following a pressured pass, sequences can be labeled as:
- **Successful escape** — possession continues, ball moves to unpressured area
- **Failed escape** — turnover, clearance, press wins the ball

A gradient boosted classifier is trained on:
```
from_position, to_position, formation,
game_phase, score_diff, pass_length,
pass_angle, pressure_intensity
→ escape_success (true / false)
```

The system's suggested sequence is the path through the graph that maximises this model's predicted success probability. This makes the suggestion defensible: it is not "what teams usually do" but **"what tends to work."**

---

## Architectural Decision: Ground Truth Panel

The result screen ideally shows a third panel — what actually happened in the real match, and whether it worked. This requires linking a moment back to its actual event sequence.

**The constraint:** The current RAG knowledge base is built from Premier League and Serie A CSV data. StatsBomb open data does not include PL or Serie A. Direct match-level lookup is not possible for PL/SA moments.

**Two options being considered:**

**Option A — Formation-level ground truth (recommended short-term)**
Instead of a specific match, show: "In similar situations — same formation, same game phase, similar score — teams escaped press successfully X% of the time. The most common successful sequence was CB → CDM → LW."
This is statistically honest and buildable without changing the ingestion architecture.

**Option B — Pivot to StatsBomb competitions**
Rebuild the moment discovery layer around StatsBomb matches (La Liga, Champions League). Now every moment has a real match ID and the actual event sequence is retrievable. The third panel shows exactly what Barcelona or Real Madrid did, and what happened next.
Tradeoff: loses PL and Serie A moments from the user experience.

The plan is to ship Option A first, and migrate to Option B as a later iteration once the core feature is validated.

---

## Current System State

| Component | Status |
|---|---|
| League + club selection | Done |
| RAG moment retrieval (ChromaDB, 68k moments) | Done |
| Moment explanation (GPT-4o-mini) | Done |
| React frontend (full navigation flow, theming) | Done |
| Tactical recommendation engine | Stubbed — not used |
| StatsBomb extraction pipeline | Not started |
| Formation probability matrices | Not started |
| Press escape outcome model | Not started |
| Context adjustment layer | Not started |
| Graph generator + `/api/network` endpoint | Not started |
| Pitch visualization (SVG) | Not started |
| Click-to-connect interaction | Not started |
| Puzzle result / comparison screen | Not started |

---

## What Makes This Technically Strong

Most AI projects generate text. This one:

1. **Models domain structure** — football as a graph, passing as a probability distribution, pressing as a network
2. **Grounds the model in real outcomes** — the optimal path is trained on labeled success/failure data, not arbitrary weights
3. **Combines multiple layers of intelligence** — data pipeline → probabilistic modeling → learned model → context adjustment → graph → LLM explanation
4. **Makes the user a participant** — the fan doesn't watch, they attempt to solve the same problem the real team faced
5. **Validates against reality** — the result screen compares user intuition, model suggestion, and real-world outcome

The mental model: Google Maps predicts traffic patterns from historical behavior. JGaffer predicts passing structure from historical tactical patterns — and then asks you to navigate it yourself.

---

## Open Questions

1. Should the puzzle use real player names (e.g. Trent Alexander-Arnold) or abstract position roles (RB)? Real names are more engaging for fans but require a player roster data layer.
2. How many passes should the user be allowed to make? A cap of 4–5 keeps it simple and mirrors real pressing escape sequences.
3. Should the system show the pressing overlay and the user's team simultaneously, or reveal the pressing network only after submission?
4. Long-term: can the outcome model be personalised — learning which escape routes a specific user tends to miss?
