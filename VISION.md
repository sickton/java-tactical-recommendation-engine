# JGaffer - Vision Document
*Last updated: 2026-04-20*

---

## What JGaffer Is

JGaffer is an **expert guide for a casual football fan**.

It is not a prediction engine, a stat dashboard, or a coaching platform for professionals.
It is a product for people who enjoy football, feel the emotion of big moments, but do not always understand the tactical story underneath them.

The goal is simple:

**Help a casual fan experience a real match moment, understand the tactical problem inside it, try to solve that problem, and come away seeing the game more clearly.**

---

## The Core Product Idea

The clearest version of JGaffer is:

**Moment -> Mission -> Puzzle -> Coaching**

1. **Moment**
The user sees a real moment from their team's season.

2. **Mission**
The system explains the tactical problem in one clear sentence.

3. **Puzzle**
The user interacts with the moment by trying to solve that problem on the pitch.

4. **Coaching**
JGaffer explains why the user's choice worked or failed, and what football idea they should learn from it.

This is the heart of the product.

JGaffer should feel less like "reading football analysis" and more like:

**"Let me show you what was really happening here - now you try it."**

---

## The Problem

Casual fans often love football emotionally before they understand it structurally.

They can feel:
- tension
- momentum swings
- danger
- pressure
- relief

But they often cannot yet see:
- why a team looked trapped
- why a pass was not actually on
- why the far side was open
- why a press worked
- why one small movement changed the whole phase

Most football analysis tools fail casual fans because they:
- assume too much tactical vocabulary
- explain too much before the user cares
- rely on reading instead of interaction

JGaffer solves this by making the fan a participant.

---

## The Mission

**Turn football understanding into an interactive experience guided by an expert voice for a casual fan.**

That means JGaffer should:
- start with a real emotional hook
- reduce reading friction
- ask the user to do something quickly
- teach one football concept at a time
- give immediate feedback in plain English

The product should not feel like homework.
It should feel like:

**"I finally understand why that moment mattered."**

---

## The Ideal User Experience

### Step 1 - Pick a team
The user starts with a club they care about.

This is important because interest comes first.
We are not asking them to study football in the abstract.
We are helping them understand *their team*.

### Step 2 - Experience a real moment
The system surfaces a real moment from the season that matches a theme:
- dramatic
- dominant
- comeback
- under pressure
- turning point
- surprise

The moment card should be short and emotionally legible.
It should make the user think:

**"What actually happened here?"**

### Step 3 - Get the tactical problem
Instead of forcing the user into a long explanation page, JGaffer should quickly frame the problem:

- "They were trapped on the left and needed a safe exit."
- "The press had shifted too far. Could they find the switch?"
- "They needed one route through midfield before the defense recovered."
- "They had to protect the lead without simply giving the ball back."

This is the bridge that connects the moment to the puzzle.

This is the most important connective tissue in the product.

### Step 4 - Solve the puzzle
The user then interacts with the moment.

They are not just told what happened.
They try to solve the exact tactical problem inside the moment.

Examples:
- escape the press
- find the free player
- choose the best switch
- trigger the right pressing trap
- protect the lead with the right sequence

The puzzle is where understanding becomes active.

### Step 5 - Learn through coaching
After the attempt, JGaffer acts like an expert guide:
- what the user saw correctly
- what they missed
- why the better option worked
- what football concept the moment teaches

This should be short, visual, and coach-like.

The key outcome is not just "you were right" or "you were wrong."
The key outcome is:

**"Now I understand the football idea behind the moment."**

---

## Product Principles

### 1. Start with emotion, then teach structure
Fans care about moments before they care about theory.

So the product should begin with:
- tension
- drama
- danger
- pressure
- comeback energy

Then reveal the tactical structure underneath.

### 2. Minimize reading before interaction
If the user must read too much before doing anything, energy drops.

So JGaffer should prefer:
- one strong sentence
- one clear mission
- one visible tactical cue

instead of large blocks of explanation upfront.

### 3. Teach one concept at a time
Each moment should have a main lesson:
- Press Resistance
- High Press
- Weak-Side Switch
- Third-Man Run
- Counter Attack
- Game Management
- Cover Shadow

Users learn better when the lesson is singular and clear.

### 4. Make the user participate
A fan remembers more when they try to solve the same problem themselves.

The puzzle is not a decorative add-on.
It is the core teaching mechanic.

### 5. Use expert guidance, not expert language
JGaffer should sound smart without sounding intimidating.

The tone should be:
- confident
- clear
- plain English
- visually guided

The product should help casual fans feel included, not tested.

---

## What Connects The Dots Cleanly

The project becomes much cleaner when every surface serves the same loop:

### Discovery
Find a real moment worth caring about.

### Briefing
State the tactical problem in one sentence.

### Interaction
Let the user solve the problem.

### Reflection
Explain the result and teach the concept.

This means the detail page is no longer just an explanation page.
It becomes a **mission briefing** page.

The user should not have to infer why a puzzle exists.
The system should tell them directly:

- what the problem is
- why it matters
- what they are being asked to solve

---

## The Role of the Backend

To support this vision, the backend should eventually generate a structured "moment brief" rather than separate disconnected blobs.

A strong moment brief would include:
- headline
- match context
- tactical problem
- mission
- concept
- puzzle type
- explanation hooks

Example:

```json
{
  "headline": "Late Pressure on Inter",
  "concept": "Press Resistance",
  "tactical_problem": "Inter are trapped on the left and need a safe route out.",
  "mission": "Find the best three-pass escape.",
  "puzzle_type": "escape_press",
  "match_context": {
    "minute": 89,
    "score": "Inter Milan 1-2 AC Milan"
  }
}
```

This is the object that connects:
- retrieval
- explanation
- puzzle generation
- coaching feedback

---

## The Role of Retrieval

Retrieval should not exist just to produce themed cards.

Its job is to find:
- real moments
- tactically teachable moments
- emotionally legible moments
- moments that are good seeds for puzzles

The retrieval system is valuable when it helps answer:

**"What real moment would best teach this football idea to this fan?"**

That makes the RAG layer part of the teaching system, not just a search feature.

---

## The Role of the Puzzle

The puzzle is the central mechanic of learning.

It should not feel detached from the moment.
It should feel like the user is stepping into the exact problem the team faced.

The current escape puzzle is a strong foundation because it already teaches:
- how pressure works
- how shape creates or removes options
- how space opens when the press shifts

Over time, the puzzle system can expand to cover:
- find the free man
- break the low block
- trigger the press
- choose the best switch
- protect the lead
- counterattack or control

But every puzzle type should still follow the same structure:

**Here is the moment. Here is the problem. Now solve it.**

---

## The Role of Coaching Feedback

The final teaching moment comes after the puzzle.

This is where JGaffer becomes the expert guide.

The feedback should answer:
- what did the user notice correctly?
- what tactical option was best?
- what made the losing option tempting?
- what concept should the user remember next time?

This is more valuable than long pre-puzzle explanation because the user now has a mental model and a personal attempt to compare against.

---

## What JGaffer Should Feel Like

JGaffer should feel:
- smart, but not academic
- guided, but not lecture-heavy
- visual, not text-heavy
- interactive, not passive
- grounded in real football moments

The ideal user reaction is:

**"I always felt this part of football mattered. Now I finally understand why."**

---

## Current Direction

The project already contains the right ingredients:
- team-based moment discovery
- AI explanation
- tactical puzzle interaction
- graph-based football modeling

What now matters most is not adding disconnected features.

What matters is aligning everything around the same learning loop:

**experience -> understand the problem -> solve -> learn**

That is the clean product direction.

---

## Near-Term Product Priorities

1. Reduce reading before action.
The moment detail experience should become a short tactical briefing, not a long explanation page.

2. Make the mission explicit.
Every moment should clearly state the tactical problem and the user's task.

3. Connect story to puzzle through backend-generated structure.
The system should produce a coherent "moment brief" that powers both explanation and interaction.

4. Make post-puzzle coaching the main teaching layer.
This is where the expert guide voice becomes most valuable.

5. Build the product around one core loop instead of multiple competing flows.
The moments -> mission -> puzzle -> coaching path should become the center of gravity.

---

## The Puzzle Suite

JGaffer has a suite of 8 interactive puzzles. Each puzzle is triggered by a real match moment and ends with coaching that teaches one football concept. The puzzle shown is selected based on the moment type, game state, and what is most teachable at that point in the match.

All puzzles follow the same structure:

**Here is the moment. Here is the problem. Now solve it. Here is what it teaches.**

---

### 1. Break the Press

**Phase:** In possession under pressure

**What happens:**
The opponent rushes the user's team. The user builds a passing sequence to escape the press — clicking through available players on the pitch. The sequence can be up to 4 passes.

**The payoff:**
The backend evaluates not just whether the sequence worked, but *how* the user passed. The sequence is mapped to a real-life football playstyle archetype and the user is taught what that style means.

**Playstyle archetypes:**
- Short central passes, staying compact → **Tiki-Taka** (patient, positional, suffocate the press)
- Quick vertical passes bypassing the midfield line → **Vertical Pressing Escape** (play through fast before they reset)
- Wide switch using the full width → **Wing Overload** (stretch the press, find space on the far side)
- Few passes, long ball over the top → **Direct Press Escape** (go long, don't play into the trap)
- Backward first, then switch side → **Guardiola Build-Up** (reset, invite the press, then exploit)

**Coaching example:**
*"You played like Pep's City — short, patient triangles that pulled the press apart before going forward. This is called positional play: the idea is to make the press chase shadows until a gap opens."*

**Concept taught:** Press resistance, positional play, passing under pressure

---

### 2. Defend Yourself

**Phase:** Out of possession, defending an attack or counter

**What happens:**
An opponent attack is unfolding. The user repositions one or two defenders to cut off passing lanes or close down the threat. The pitch shows the attacker positions and the available defensive moves.

**The payoff:**
Backend evaluates whether the defensive shape held or was bypassed. Maps the user's approach to a defensive archetype.

**Defensive archetypes:**
- Tracking the runner tightly → **Man-Marking**
- Holding shape and covering the zone → **Zonal Defence**
- Dropping to protect the goal line → **Low Block Recovery**
- Pressing the ball carrier immediately → **High Press Recovery**

**Coaching example:**
*"You held your zone rather than following the runner — that's zonal defending. It protects the space but relies on your teammates tracking the runners you've left."*

**Concept taught:** Defensive shape, zonal vs man-marking, recovery runs

---

### 3. Penalty Kick

**Phase:** Dead ball, penalty situation

**What happens:**
The user is taking a penalty. They pick a direction — left low, left high, centre, right low, right high. The keeper has a modelled save probability per zone based on real tendencies. The backend runs the probability and returns a goal or save scenario.

**The payoff:**
The result is revealed with a visual, followed by coaching on penalty strategy, keeper tendencies, and decision-making under pressure.

**Coaching example:**
*"You picked right-low — the most popular direction in the league. Keepers train hardest for it. Takers who study the keeper's dive tendency first score 23% more. The top-corner remains the lowest save probability in every keeper's profile."*

**Concept taught:** Probability, pressure decision-making, keeper tendencies, penalty psychology

---

### 4. Reshape

**Phase:** Crisis — a red card has just occurred

**What happens:**
An unexpected red card leaves the team with 10 players. The user rearranges the remaining players into a new shape by selecting a formation from a set of options. The backend scores the shape on zone coverage — how well the defensive third, midfield, and attacking third are covered with one fewer player.

**The payoff:**
The backend maps the user's choice to a real formation and explains how it is used to protect a lead or manage a game with 10 men.

**Coaching example:**
*"You went 4-4-1 — the classic shape for a team down to 10. The flat four in midfield blocks the central lanes and the lone forward stays as an outlet. The risk is the wide areas, which the opposition will target."*

**Concept taught:** Formation structure, zone coverage, game management with reduced numbers

---

### 5. Spring the Offside Trap

**Phase:** Attacking transition

**What happens:**
The defensive line is sitting high. The user is the attacker and must time their run to beat the trap — picking the exact moment to move. Too early and the flag goes up. Too late and the defender recovers. The backend calculates based on line position and ball release timing whether the run succeeds.

**The payoff:**
The result is shown with a clear visual of where the line was and where the run was timed. Coaching explains the offside rule and the skill of timing a run.

**Coaching example:**
*"You timed it perfectly — that's a third-man run. The key is to move as the ball leaves the passer's foot, not before. The defender's job is to hold the line until that exact moment."*

**Concept taught:** Offside rule, attacking movement, run timing, exploiting a high line

---

### 6. Set the Press

**Phase:** Out of possession, pressing

**What happens:**
The mirror of Break the Press — now the user is defending. They pick which player triggers the press and which zones to overload against the opponent's formation. The backend evaluates whether the press wins the ball, forces a long ball, or gets bypassed.

**The payoff:**
The result is mapped to a pressing archetype and the user is taught what makes a press work.

**Pressing archetypes:**
- Cutting off the CB's short options → **Passing Lane Press**
- Overloading one side to force a switch → **Half-Space Trap**
- Pressing immediately on loss of possession → **Gegenpressing**
- Holding shape and pressing on trigger cues → **Structured Press**

**Coaching example:**
*"You pressed like Klopp's Liverpool — you cut off the CB's passing lane and forced the long ball. That is gegenpressing: win the ball back high up the pitch before the opponent can reorganise."*

**Concept taught:** Pressing triggers, compactness, coordinated pressure, pressing archetypes

---

### 7. Substitution Gamble

**Phase:** Game management, mid-to-late match

**What happens:**
The match is in progress — the user's team is losing, drawing, or protecting a narrow lead with around 20-25 minutes left. Three substitutes are available, each with a different profile: an extra striker, a defensive midfielder, or a wide player. The backend knows the current match state, how many chances have been created, and how the opposition is sitting. The user picks one.

**The payoff:**
The backend calculates the probability shift each substitute creates and reveals what the choice signals tactically. The coaching explains the trade-offs.

**Coaching example:**
*"You went for the striker at 1-0 down with 22 minutes left — high risk, high reward. Managers who commit to a second striker this early win 31% of the time but concede on the counter in 44%. The defensive midfielder would have secured the draw more often."*

**Concept taught:** Game management, risk vs reward, reading match state, tactical substitutions

---

### 8. Dead Ball

**Phase:** Set piece — free kick near or around the box

**What happens:**
A free kick has been won in a dangerous position. The user picks the delivery type — curled around the wall, driven low under the wall, or lifted to the back post — and the target zone. The backend has wall height probabilities and keeper positioning models and calculates whether the delivery results in a goal, a save, or a block.

**The payoff:**
The result is shown visually with coaching on why certain deliveries work against certain keeper and wall setups.

**Coaching example:**
*"You went low and driven — the hardest delivery for keepers to get down to quickly. But the wall was well-set. The curl over the wall to the far post had a higher expected outcome here. Set piece specialists study keeper positioning before every delivery."*

**Concept taught:** Set piece decision-making, delivery types, spatial awareness, keeper positioning

---

## Puzzle Selection Logic

Puzzles are not shown randomly. The system selects the most appropriate puzzle based on:

- **Moment type** — a press-heavy moment triggers Break the Press or Set the Press
- **Game state** — a red card moment triggers Reshape; a late losing moment triggers Substitution Gamble
- **Minute** — early moments suit pressing puzzles; late moments suit game management or dead ball
- **Score context** — losing moments suit attacking puzzles; winning moments suit defensive or management puzzles

Over time the selection logic improves as more moment metadata is available.

---

## Long-Term Vision

Long-term, JGaffer can become a library of interactive football lessons hidden inside real match moments.

A user should be able to:
- return daily
- explore moments from their club
- play a different puzzle type each session
- learn recurring tactical patterns
- improve their football eye over time

The ambition is not just to explain football.

The ambition is to help a casual fan **see the game the way a more expert fan sees it** — through participation, guided attention, and repeated exposure to meaningful moments across every phase of the game.

That is the vision.
