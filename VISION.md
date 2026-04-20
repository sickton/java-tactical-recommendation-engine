# JGaffer — Vision Document
*Last updated: 2026-04-20 | Branch: v2/layer-7-network-api*

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

A **Dive** button takes the user deeper into a full tactical explanation.

### Step 3 — The Tactical Puzzle
The screen splits into two panels rendered as SVG pitch diagrams:

**Left panel — Opposition's press structure (PressureMap)**
Shows how the pressing team is organised. Nodes = player positions. Players are
role-differentiated:
- **Harasser** (closest to ball): bright, pulsing halo, charge arrow toward ball carrier
- **Shadowers** (next 3): medium, body-pointer toward their nearest mark, patrol ring
- **Anchors** (rest): dim, lock icon if close enough to intercept

The entire block dynamically shifts toward the ball side when play moves wide:
the far-side winger tucks in, the CDM steps to cut the back-pass lane, and the
full defensive line adjusts — all animated with CSS spring transitions (0.48s
cubic-bezier). Ghost dashed circles mark vacated positions.

Cover shadows (blurred red wedges) show which passing lanes each presser deletes.
The **high line** is marked by a dashed orange line; the dark grass zone behind it
is labelled "SPACE IN BEHIND" — the visual win condition for the puzzle.

When the user makes their first pass, a **lure mechanic** activates: the freest
escape player gets a teal dashed run arrow pointing into open space, with a faint
red reaction arrow showing the presser who would follow — communicating "make the
run, the space opens behind."

**Right panel — Escape route builder (PitchGraph)**
The user's team is displayed with position nodes. The user builds a passing
sequence by clicking through nodes. Visual aids:

- **Predictive arc**: a marching-dashes teal arc curves to a lead point 28px ahead
  of the optimal target (not to the player's feet — to the space)
- **Crosshair**: pulsing target icon at the lead point ("pass here")
- **Vacated zone glows**: green pulsing circles at positions the pressing block
  has shifted away from — the "holes" opened up
- **Passing window brackets**: perpendicular markers at each edge midpoint; gap
  width scales with escape_prob (open lane = wide gap, closed = narrow)
- **Floating tooltip**: on edge hover, shows Risk%, forward Gain in metres,
  and lane status ("Lane open / Lane narrowing / Lane closed — wait")
- **Optimal node**: pulsing gold ring on the highest-probability target
- **Covered nodes**: dimmed to 45% opacity when escape_prob < 0.28
- **Exposed zone**: green ellipse behind target node when hovered (the space
  they'd run into on receiving)
- **Lead pass extension**: dashed line 22px past target for safe forward passes

All passes are allowed — including risky ones and "no edge" attempts. The click
registers with color feedback (blue/yellow/red flash) regardless of escape_prob.

### Step 4 — The Result
After submitting, the user sees:
- Their sequence vs. the system's optimal path
- A medal (GOLD/SILVER/BRONZE/MISS) based on ratio of user score to optimal score
- A plain English verdict
- Two meters: escape safety + forward progression percentage

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
Context adjustment layer  [not yet built]
(modifies base matrix using: style, stamina, game phase, score diff)
        ↓
Graph generator → /api/network
(outputs: escape_graph + pressing_graph with nodes + weighted edges)
        ↓
React puzzle visualization
(PressureMap SVG + PitchGraph SVG, click-to-connect interaction)
        ↓
LLM explanation
(narrates structural reasoning in plain English via GPT-4o-mini)
```

---

## The Data Foundation

### StatsBomb Open Data
Used exclusively to learn **how passing works positionally** — not league-specific, not team-specific. The question answered: given a formation and game phase, how do positions connect to each other?

Pass events are extracted and labeled:
- `from_position` → `to_position` (abstract roles: CB, CDM, LW, ST, etc.)
- Formation detected from tactics events in the same match
- Outcome labeled: did the sequence escape pressure successfully, or did possession break down?

Two artefacts:
1. **Probability matrices** — for each formation, `P(to_position | from_position)`
2. **Labeled training dataset** — for the outcome model

### Why StatsBomb data transfers across leagues
The matrices are formation-conditioned, not league-conditioned. A 4-3-3 POSSESSION team passes with the same structural logic whether in La Liga or the Premier League. Formation is the abstraction that makes the data portable.

### Existing Squad/Match Data (PL and Serie A)
Used for the moment discovery layer (RAG) and for the context adjustment layer. Team style, stamina, adaptability, and formation are already ingested. These attributes will modify the base probability matrices at query time once the context adjustment layer is built.

---

## The Press Escape Outcome Model

StatsBomb pass events include an `under_pressure` boolean. By tracking what happens in the 3–5 events following a pressured pass, sequences can be labeled as:
- **Successful escape** — possession continues, ball moves to unpressured area
- **Failed escape** — turnover, clearance, press wins the ball

A gradient boosted classifier (HistGradientBoosting) is trained on:
```
from_position, to_position, formation,
game_phase, score_diff, pass_length,
pass_angle, pressure_intensity
→ escape_success (true / false)
```

Metrics: ROC-AUC 0.84, CV 0.8333 ± 0.005, 79% fail recall.

The system's suggested sequence is the greedy path through the graph maximising this model's predicted success probability — "what tends to work" rather than "what teams usually do."

---

## Architectural Decision: Ground Truth Panel

The result screen ideally shows a third panel — what actually happened in the real match. This requires linking a moment back to its actual event sequence.

**The constraint:** The RAG knowledge base uses PL and Serie A CSV data. StatsBomb open data does not include PL or Serie A. Direct match-level lookup is not possible.

**Option A — Formation-level ground truth (current plan)**
Show: "In similar situations — same formation, same game phase, similar score — teams escaped press successfully X% of the time. The most common successful sequence was CB → CDM → LW."
Statistically honest, buildable without changing the ingestion architecture.

**Option B — Pivot to StatsBomb competitions**
Rebuild moment discovery around StatsBomb matches (La Liga, Champions League). Every moment would have a real match ID with retrievable event sequences. Tradeoff: loses PL and Serie A moments.

The plan is to ship Option A first, migrate to Option B if the core feature validates well.

---

## Current System State

| Component | Status |
|---|---|
| League + club selection | Done |
| RAG moment retrieval (ChromaDB, 68k moments) | Done |
| Moment explanation (GPT-4o-mini) | Done |
| React frontend (full navigation flow, theming) | Done |
| StatsBomb extraction pipeline | Done — 143,349 pressured pass rows, 1,162 matches |
| Formation probability matrices | Done — 5 formations, all probabilities verified |
| Press escape outcome model | Done — ROC-AUC 0.84, CV 0.8333 ± 0.005 |
| `/api/network` endpoint (graph generation) | Done |
| Tactical puzzle — PressureMap (left panel) | Done — full dynamic press shift, lure mechanic, cover shadows, intel bar |
| Tactical puzzle — PitchGraph (right panel) | Done — predictive arc, crosshair, vacated zones, passing window, tooltip |
| Tactical puzzle — interaction + scoring | Done — sequence building, undo, submit, medal, verdict |
| Context adjustment layer | Not started |
| Result panel: formation-level ground truth | Not started |
| Tension Score (0–100 model) | Not started |
| "What to Watch" as a distinct API field | Not started |

---

## What Makes This Technically Strong

1. **Models domain structure** — football as a graph, passing as a probability distribution, pressing as a dynamic network with role-differentiated players
2. **Grounds the model in real outcomes** — the optimal path is trained on labeled success/failure data, not arbitrary weights
3. **Combines multiple layers of intelligence** — data pipeline → probabilistic modeling → learned model → context adjustment → graph → LLM explanation
4. **Makes the user a participant** — the fan doesn't watch; they attempt to solve the same problem the real team faced, with real tactical feedback
5. **Validates against reality** — the result screen compares user intuition, model suggestion, and (planned) real-world outcome

The mental model: Google Maps predicts traffic patterns from historical behaviour. JGaffer predicts passing structure from historical tactical patterns — and then asks you to navigate it yourself.

---

## Open Questions

1. Should the puzzle use real player names (e.g. Trent Alexander-Arnold) or abstract roles (RB)? Real names are more engaging but require a player roster data layer per team.
2. Should the pressing network also animate after each user pass — the press shifts to respond — rather than only updating the ball-carrier highlight?
3. Long-term: can the outcome model be personalised — learning which escape routes a specific user tends to miss, and surfacing those as training puzzles?
4. Option B migration: is it worth rebuilding around StatsBomb matches to unlock the third result panel (what actually happened)?
