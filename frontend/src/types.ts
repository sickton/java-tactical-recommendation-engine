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
