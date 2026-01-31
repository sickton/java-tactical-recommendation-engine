# JGaffer
## Java Tactical Recommendation System

JGaffer is a Java-based, rule-driven realistic decision engine that recommends tactical changes based on a given football (soccer) match context.

The system models how a football manager adapts team tactics during a match by evaluating factors such as the current scoreline, team strengths, opponent style of play, player stamina, and injury considerations.

The current version of the project focuses on modeling real-life decision-making by coaches, rather than match simulation or machine learning (***planned for future iterations***).

-----

## Problem Statement

During a football match, managers continuously adjust team tactics based on factors such as:
- Scoreline
- Minutes remaining
- Player fatigue
- Team identity
- Player adaptability
- Opponent behavior

JGaffer models this decision-making process as a **rule-based engine**.  
Given a snapshot of the match state, the system evaluates a predefined set of tactical rules and recommends the most appropriate tactical approach for the team in that context.

-----

## Tactics

The engine evaluates the match context to recommend one of the following tactics
- Mid Block
  - Implemented to achieve control in the midfield and central sections of the pitch, usually helpful in absorbing pressure and controlling the game for longer periods of time by staying narrow and compact.
- Low Block
  - Implemented to absorb maximum pressure in the team's final defensive box. Usually considered the most defensive style of play. Most advantageous to teams playing a more direct style of football and do not allow space 
    for opponents to score.
- High Press
  - Implemented to suffocate opponents with constant high energy chasing, allowing opponents to make silly mistakes, ultimately creating and scoring from the mistake. Teams with a fluid structure and a possession based playstyle tend to up the ante to make things happen on the pitch.
- Counter
  - Implemented to simulate end to end attacking where, a team benefits from a set piece or a corner and is instantly able to create an overload in opponent's half, giving maximum chances of scoring. Usually teams that play a direct style of football benefit with this tactic. 
- Possession
  - Implemented to play fluid football, usually characterized by constant moving of the ball in spaces, channels and have maximum possession of the ball. A strong fluid ball movement results in dangerous passes and crosses and often result in good passage of play leading to a goal. Possession based football team often are able to establish such kind of gameplay.