# JGaffer
## Java Tactical Recommendation System

JGaffer is a Java-based, rule-driven decision engine that recommends tactical changes based on a given football (soccer) match context.

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