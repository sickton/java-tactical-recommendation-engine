export interface Moment {
  headline: string;
  minute: number;
  match: string;
  score: string;
  narrative: string;
  concept: string;
  tactical_problem?: string;
  mission?: string;
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

export interface NetworkNode {
  id: string;
  x: number;
  y: number;
}

export interface NetworkEdge {
  from: string;
  to: string;
  weight: number;
  escape_prob?: number;
}

export interface NetworkGraph {
  nodes: NetworkNode[];
  edges: NetworkEdge[];
}

export interface NetworkResponse {
  escape_graph: NetworkGraph;
  pressing_graph: NetworkGraph;
  game_phase: string;
  escaping_formation: string;
  pressing_formation: string;
}

// ── Puzzle system ─────────────────────────────────────────────────────────────

export interface PuzzleBrief {
  problem: string;
  mission: string;
}

export interface BreakThePressConfig {
  escape_graph: NetworkGraph;
  pressing_graph: NetworkGraph;
  ball_carrier: string;
  escaping_formation: string;
  pressing_formation: string;
  game_phase: string;
}

export interface PuzzleResponse {
  puzzle_type: 'break_the_press';
  brief: PuzzleBrief;
  config: BreakThePressConfig;
}

export interface EvaluateRequest {
  puzzle_type: string;
  answer: Record<string, unknown>;
  config: Record<string, unknown>;
  moment_context: {
    headline: string;
    minute: number;
    match: string;
    score: string;
    concept: string;
    team: string;
    tactical_problem: string;
    mission: string;
  };
}

export interface EvaluateResult {
  puzzle_type: string;
  user_score: number;
  optimal_path: string[];
  optimal_score: number;
  medal: 'GOLD' | 'SILVER' | 'BRONZE' | 'MISS' | '—';
  playstyle: string;
  playstyle_desc: string;
  coaching: string;
}
