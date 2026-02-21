export interface TeamInfo {
  name: string;
  staminaLevel: string;
  adaptabilityLevel: string;
}

export interface MatchContext {
  title: string;
  homeGoals: number;
  awayGoals: number;
  minute: number;
  home: TeamInfo;
  away: TeamInfo;
}

export interface MatchData {
  teamId: number;
  teamName: string;
  opponentName: string;
  matchId: number;
  minute: number;
  league: string;
  gamePhase: string;
  context: MatchContext;
  tactics: string[];
  isHome: boolean;
}

export interface Recommendation {
  tactic: string;
  confidence: number;
  formation: string;
}

export interface RecommendResult {
  teamName: string;
  opponentName: string;
  minute: number;
  teamGoals: number;
  opponentGoals: number;
  userTactic: string;
  recommendation: Recommendation;
  agrees: boolean;
  explanation: string;
  matchId: number;
  teamId: number;
  league: string;
}

export interface MatchEvent {
  minute: number;
  type: 'GOAL_HOME' | 'GOAL_AWAY' | 'PHASE_CHANGE' | 'TACTIC_NOTE' | 'ATMOSPHERE';
  label: string;
}

export interface SimulationData {
  teamName: string;
  opponentName: string;
  teamId: number;
  matchId: number;
  league: string;
  startMinute: number;
  startTactic: string;
  startHomeGoals: number;
  startAwayGoals: number;
  isHome: boolean;
  tacticFidelityScore: number;
  managerRating: string;
  matchingPhases: number;
  totalPhases: number;
  finalHomeGoals: number;
  finalAwayGoals: number;
  events: MatchEvent[];
}

export interface PhaseResult {
  events: MatchEvent[];
  finalHomeGoals: number;
  finalAwayGoals: number;
  finalTeamGoals: number;
  finalOpponentGoals: number;
  tacticFidelityScore: number;
  managerRating: string;
  matchingPhases: number;
  totalPhases: number;
}

export interface Fixture {
  id: number;
  title: string;
}
