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
It acts as an assistant manager in a game, agreeing or suggesting changes to the user's choice of tactics.

-----

## Tactics

The engine evaluates the current MatchContext and recommends one of the following tactical strategies defined in the Tactic enum:
- #### *Gegenpressing*:
  - Implemented to apply immediate and aggressive pressure after losing possession. The objective is to win the ball back within seconds in advanced areas of the pitch. This tactic relies on coordinated pressing triggers, high stamina, and compact vertical spacing between lines to suffocate opponents and create high-probability scoring opportunities.
- #### *High Press*:
  - Implemented to consistently apply pressure high up the pitch during opposition build-up phases. Unlike situational pressing, this tactic maintains sustained attacking pressure to disrupt structured play, force rushed decisions, and recover possession in dangerous zones.
- #### *Tiki Taka*:
  - Implemented to prioritize short, quick passing and positional rotation to maintain fluid ball circulation. The focus is on creating space through movement, controlling tempo, and breaking defensive lines through precision and patience rather than direct vertical play.
- #### *Control*:
  - Implemented to manage the tempo of the game through structured possession and disciplined positioning. This tactic balances defensive stability with measured attacking progression, reducing risk while maintaining territorial dominance.
- #### *Counter Attack*:
  - Implemented to exploit transitional moments immediately after regaining possession. The objective is rapid vertical progression into open spaces before the opposition can reorganize defensively. This tactic emphasizes pace, direct passing, and attacking overloads.
- #### *Direct play*:
  - Implemented to advance the ball quickly into attacking areas using long passes, aerial distribution, and minimal build-up phases. This approach reduces midfield circulation and focuses on territorial gain and quick goal-scoring opportunities.
- #### *Low Block*:
  - Implemented to defend deep within the team’s defensive third with compact defensive lines. The priority is space denial, central protection, and forcing opponents into low-quality wide areas. This tactic is typically used when protecting a lead or absorbing sustained pressure.

-----

## Game Phases

The engine evaluates a given match context based on the phase it is occurring in. Based on that, here are the seven phases, that are considered as important to study the game from the dugout
- #### *Early Minutes*
  - From minutes 1 to 15
  - This phase focuses on establishing structure, tempo, and territorial control. Teams typically avoid high-risk tactical shifts early on, instead assessing opponent shape and match rhythm before committing to aggressive strategies
- #### *Closing Half*
  - From minutes 16 to 44
  - Tactical adjustments begin to intensify as halftime approaches. Managers may push for a psychological advantage before the break or stabilize the team if under pressure. Decisions in this window often influence halftime team talks and second-half planning.
- #### *Half Time*
  - From minutes 45 to 50
  - This represents the structured adjustment window. Tactical recalibration is at its highest importance here, as managers can reflect on first-half performance and deliberately shift intent, formation emphasis, or pressing intensity.
- #### *Build Phase*
  - From minutes 51 to 60
  - The early second-half period where teams implement halftime adjustments. Managers evaluate whether tactical changes are producing the desired effect and may refine strategy without entering high-risk territory.
- #### *Tension Time*
  - From minutes 61 to 70
  - Match volatility increases as fatigue begins to influence structure and decision-making. Tactical shifts during this phase often aim to regain control, protect energy levels, or prepare for an aggressive final push.
- #### *Late Game*
  - From minutes 71 to 87
  - Urgency significantly increases. Scoreline context heavily influences tactical aggression or conservatism. Risk tolerance rises for teams chasing the game, while defensive solidity becomes critical for teams protecting a lead.
- #### *Stoppage Time*
  - From minutes 88+
  - The highest-pressure window of the match. Tactical decisions become extreme and highly outcome-driven. Teams may commit fully to attacking overloads or retreat into deep defensive structures depending on the scoreline.

-----

