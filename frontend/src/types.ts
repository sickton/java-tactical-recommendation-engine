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
  homeFormation: string;
  awayFormation: string;
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

export interface Fixture {
  id: number;
  title: string;
}

export interface Moment {
  headline: string;
  minute: number;
  match: string;
  score: string;
  narrative: string;
  concept: string;
}

export interface StoryResponse {
  team: string;
  query_type: string;
  moments: Moment[];
}

export interface ExplainResponse {
  headline: string;
  match: string;
  minute: number;
  concept: string;
  explanation: string;
}

