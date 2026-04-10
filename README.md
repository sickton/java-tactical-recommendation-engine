# JGaffer

A contextual football intelligence engine for casual fans.

Football has a massive casual audience — people who watch but don't fully understand what they're seeing. Every football app is built for people who already know the game. JGaffer is built for everyone else.

---

## What It Does

Pick a match. Pick a moment. JGaffer tells you three things:

- **The Story** — what is happening right now and why, in plain English
- **The Tension Score** — a single number (0–100) showing how much is at stake, driven by the minute, scoreline, game phase, and team styles
- **What to Watch** — one specific thing to look for next on the pitch

No jargon. No tactic names. Just football made human.

---

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 23, Spring Boot 3, Maven |
| Engine | Contextual match state builder + tension scoring model |
| Frontend | React, TypeScript, Vite |
| AI | OpenAI GPT-4.1 (match narration) |
| Data | 2024/25 Premier League + Serie A |

---

## Project Structure

```
JGaffer/      Spring Boot backend + match data
frontend/     React frontend (builds into JGaffer/src/main/resources/static/)
```

---

## Running the App

### Prerequisites
- Java 23
- Maven
- Node.js
- OpenAI API key (optional — narration falls back gracefully without it)

### Backend

```bash
cd JGaffer
mvn spring-boot:run
```

App starts on `http://localhost:8080`.

### Frontend (dev mode)

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` — proxies `/api` to the backend.

### Frontend (production build)

```bash
cd frontend
npm run build
```

Outputs to `JGaffer/src/main/resources/static/` — served by Spring Boot.

---

## Leagues

| League | Teams | Season |
|---|---|---|
| Premier League | 20 teams | 2024/25 |
| Serie A | 20 teams | 2024/25 |

---

## Status

**JGaffer 2.0 is under active development.** The engine is being reworked from the ground up.
