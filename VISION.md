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

## Long-Term Vision

Long-term, JGaffer can become a library of interactive football lessons hidden inside real match moments.

A user should be able to:
- return daily
- explore moments from their club
- learn recurring tactical patterns
- improve their football eye over time

The ambition is not just to explain football.

The ambition is to help a casual fan **see the game the way a more expert fan sees it** - through participation, guided attention, and repeated exposure to meaningful moments.

That is the vision.
