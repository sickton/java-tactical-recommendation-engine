// Formation-aware display coordinates for each position.
// The backend positions all central mids at y=0.50 (abstract roles).
// These override that with realistic pitch spread per formation.
// x: 0 = own goal, 1 = opponent goal
// y: 0 = left touchline, 1 = right touchline

type Coords = [number, number];

// Fallback when formation is unknown
const BASE_COORDS: Record<string, Coords> = {
  GK:  [0.05, 0.50],
  CB:  [0.18, 0.38],
  CB2: [0.18, 0.62],
  LB:  [0.20, 0.22],
  RB:  [0.20, 0.78],
  LWB: [0.38, 0.14],
  RWB: [0.38, 0.86],
  CDM: [0.38, 0.50],
  CM:  [0.52, 0.32],
  LM:  [0.50, 0.18],
  RM:  [0.50, 0.82],
  CAM: [0.62, 0.68],
  LW:  [0.72, 0.18],
  RW:  [0.72, 0.82],
  CF:  [0.78, 0.62],
  ST:  [0.84, 0.50],
};

// Per-formation overrides — only positions that need to move from the base
const FORMATION_OVERRIDES: Record<string, Partial<Record<string, Coords>>> = {
  F_4_3_3: {
    CB:  [0.18, 0.38],
    CB2: [0.18, 0.62],
    CDM: [0.40, 0.50],
    CM:  [0.54, 0.28],
    CAM: [0.54, 0.72],
    LW:  [0.73, 0.16],
    RW:  [0.73, 0.84],
    ST:  [0.84, 0.50],
  },
  F_4_2_3_1: {
    CB:  [0.18, 0.38],
    CB2: [0.18, 0.62],
    CDM: [0.38, 0.36],
    CM:  [0.38, 0.64],
    CAM: [0.60, 0.50],
    LW:  [0.68, 0.20],
    RW:  [0.68, 0.80],
    ST:  [0.84, 0.50],
  },
  F_3_4_3: {
    // Four midfielders across: CDM left, CM right, LW/RW wide
    CB:  [0.18, 0.50],
    LWB: [0.40, 0.14],
    RWB: [0.40, 0.86],
    CDM: [0.46, 0.36],
    CM:  [0.46, 0.64],
    LW:  [0.73, 0.18],
    RW:  [0.73, 0.82],
    CF:  [0.80, 0.50],
    ST:  [0.84, 0.50],
  },
  F_3_5_2: {
    // Five midfielders: wingbacks wide, CDM center, two CMs inside
    CB:  [0.18, 0.50],
    LWB: [0.40, 0.10],
    RWB: [0.40, 0.90],
    CDM: [0.44, 0.50],
    CM:  [0.54, 0.33],
    LM:  [0.54, 0.18],
    RM:  [0.54, 0.82],
    CF:  [0.78, 0.38],
    ST:  [0.78, 0.62],
  },
  F_4_4_2: {
    // Flat four in midfield
    LM:  [0.50, 0.16],
    CDM: [0.50, 0.38],
    CM:  [0.50, 0.62],
    RM:  [0.50, 0.84],
    CF:  [0.80, 0.36],
    ST:  [0.80, 0.64],
  },
  F_5_3_2: {
    LB:  [0.22, 0.16],
    RB:  [0.22, 0.84],
    LWB: [0.42, 0.12],
    RWB: [0.42, 0.88],
    CB:  [0.18, 0.50],
    CDM: [0.44, 0.50],
    CM:  [0.54, 0.30],
    CAM: [0.54, 0.70],
    CF:  [0.78, 0.38],
    ST:  [0.78, 0.62],
  },
};

export function getDisplayCoords(positionId: string, formation: string): Coords {
  const overrides = FORMATION_OVERRIDES[formation] ?? {};
  if (overrides[positionId]) return overrides[positionId]!;
  if (BASE_COORDS[positionId]) return BASE_COORDS[positionId];
  // If position unknown, fall back to backend coords passed in
  return [0.5, 0.5];
}
