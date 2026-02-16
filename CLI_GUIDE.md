# Command Line Interface

## Prerequisites

- **Java 23** installed
- **Maven** installed
- **OpenAI API key** set as an environment variable:
  ```
  set OPENAI_API_KEY=your-api-key-here       (Windows)
  export OPENAI_API_KEY=your-api-key-here     (Mac/Linux)
  ```

## Running the Application

```bash
cd JGaffer
mvn compile exec:java -Dexec.mainClass="com.sickton.jgaffer.demoUI.jgafferApplication"
```

Or run `jgafferApplication.java` directly from your IDE.

---

## Usage Flow

### Step 1 — Select Your Team

You'll see a table of all 20 Premier League teams. Enter the **Team ID** (1–20) to pick your club.

```
+------------+-------------------+
|  Team ID   |     Team Name     |
+------------+-------------------+
|      1     |      Arsenal      |
|      2     |     Aston Villa   |
|     ...    |        ...        |
|     20     |     Southampton   |
+------------+-------------------+

Enter the Team ID - 10
```

### Step 2 — Choose a Fixture

Your team's home and away fixtures are displayed. Enter a **Match Number** to select a specific match.

```
+-------------------------------------------+
|                 Fixtures                  |
+-------------------------------------------+
|        Home         |        Away         |
+-------------------------------------------+
|  1 : LIV-ARS       |  2 : MCI-LIV       |
|  ...                |  ...                |
+-------------------------------------------+

Enter the Match Number to retrieve a context - 1
```

### Step 3 — Review the Match Context

A random minute (1–90) is generated, and the system displays the current match state — scoreline, teams, stamina, adaptability, and team intents.

### Step 4 — Enter Your Tactic

Choose from the available tactics:

| Tactic | Description |
|--------|-------------|
| `GEGENPRESSING` | Immediate aggressive pressure after losing possession |
| `HIGH_PRESS` | Sustained pressure during opposition build-up |
| `TIKI_TAKA` | Short, quick passing with positional rotation |
| `CONTROL` | Structured possession with disciplined positioning |
| `COUNTER_ATTACK` | Exploiting transitions after regaining possession |
| `DIRECT_PLAY` | Quick vertical progression with long passes |
| `LOW_BLOCK` | Deep defensive positioning with compact lines |

Type the tactic name exactly as shown (case-insensitive).

### Step 5 — Get the Recommendation

The engine compares your suggestion against its recommendation:

- **If you match** — JGaffer agrees and explains why the tactic works
- **If you differ** — JGaffer suggests its alternative with an AI-generated explanation

The explanation is powered by OpenAI's GPT-4.1 and provides 5 bullet points of tactical reasoning.

---

## Notes

- The OpenAI explanation is optional — if the API key is missing or the service is unavailable, the recommendation still works; only the explanation is skipped.
- Match minutes are randomly generated each run, so the same fixture can produce different tactical recommendations at different points in the match.
