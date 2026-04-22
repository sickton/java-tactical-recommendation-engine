// Formation-aware display coordinates for each position node.
// x: 0 = own goal end  →  1 = opponent goal end
// y: 0 = left touchline  →  1 = right touchline
//
// Where a formation uses two nodes with different abstract labels at the same
// tactical depth (e.g. CDM + CM as a double pivot, LM + RM as wide mids),
// they are placed symmetrically on either side of y=0.50.

type Coords = [number, number];

// Fallback used when formation is unknown
const BASE_COORDS: Record<string, Coords> = {
  GK:  [0.05, 0.50],
  CB:  [0.20, 0.36],
  CB2: [0.20, 0.64],
  CB3: [0.20, 0.50],  // centre CB in 3-back formations
  LB:  [0.22, 0.14],
  RB:  [0.22, 0.86],
  LWB: [0.40, 0.10],
  RWB: [0.40, 0.90],
  CDM: [0.42, 0.50],
  CM:  [0.52, 0.32],
  LM:  [0.50, 0.16],
  RM:  [0.50, 0.84],
  CAM: [0.62, 0.50],
  LW:  [0.74, 0.14],
  RW:  [0.74, 0.86],
  CF:  [0.80, 0.38],
  ST:  [0.84, 0.50],
};

const FORMATION_OVERRIDES: Record<string, Partial<Record<string, Coords>>> = {

  // ── 4-3-3 ──────────────────────────────────────────────────────────────────
  // Row 1 (def):    LB  CB  CB  RB
  // Row 2 (mid):    CM  CAM  CM   ← flat three; CDM fills left CM slot
  // Row 3 (att):    LW  ST  RW
  F_4_3_3: {
    LB:  [0.22, 0.13],
    CB:  [0.22, 0.35],
    CB2: [0.22, 0.65],
    RB:  [0.22, 0.87],
    CDM: [0.50, 0.24],   // left CM of flat three
    CAM: [0.52, 0.50],   // centre of flat three
    CM:  [0.50, 0.76],   // right CM of flat three
    LW:  [0.78, 0.13],
    ST:  [0.84, 0.50],
    RW:  [0.78, 0.87],
  },

  // ── 4-2-3-1 ────────────────────────────────────────────────────────────────
  // Row 1 (def):    LB  CB  CB  RB
  // Row 2 (pivot):  CDM  CDM      ← double pivot; CDM + CM side by side
  // Row 3 (AM):     RW  CAM  LW
  // Row 4 (att):    ST
  F_4_2_3_1: {
    LB:  [0.22, 0.13],
    CB:  [0.22, 0.35],
    CB2: [0.22, 0.65],
    RB:  [0.22, 0.87],
    CDM: [0.40, 0.34],   // left of double pivot
    CM:  [0.40, 0.66],   // right of double pivot
    LW:  [0.65, 0.13],
    CAM: [0.67, 0.50],
    RW:  [0.65, 0.87],
    ST:  [0.84, 0.50],
  },

  // ── 4-4-2 ──────────────────────────────────────────────────────────────────
  // Row 1 (def):    LB  CB  CB  RB
  // Row 2 (mid):    RM  CM  CM  LM   ← flat four; CDM fills inner-left slot
  // Row 3 (att):    ST  ST
  F_4_4_2: {
    LB:  [0.22, 0.13],
    CB:  [0.22, 0.35],
    CB2: [0.22, 0.65],
    RB:  [0.22, 0.87],
    LM:  [0.50, 0.12],   // wide left mid
    CDM: [0.50, 0.38],   // inner left mid
    CM:  [0.50, 0.62],   // inner right mid
    RM:  [0.50, 0.88],   // wide right mid
    CF:  [0.80, 0.36],
    ST:  [0.80, 0.64],
  },

  // ── 3-4-3 ──────────────────────────────────────────────────────────────────
  // Row 1 (def):    CB(L)  CB3(C)  CB2(R)
  // Row 2 (mid):    LM  CDM  CM  RM
  // Row 3 (att):    LW  ST  RW
  F_3_4_3: {
    CB:  [0.20, 0.24],   // left CB
    CB3: [0.20, 0.50],   // centre CB
    CB2: [0.20, 0.76],   // right CB
    LM:  [0.48, 0.12],   // wide left mid
    CDM: [0.48, 0.38],   // inner left mid
    CM:  [0.48, 0.62],   // inner right mid
    RM:  [0.48, 0.88],   // wide right mid
    LW:  [0.78, 0.13],
    ST:  [0.84, 0.50],
    RW:  [0.78, 0.87],
  },

  // ── 3-5-2 ──────────────────────────────────────────────────────────────────
  // Row 1 (def):    CB(L)  CB3(C)  CB2(R)
  // Row 2 (mid):    LWB  LM  CDM  RM  RWB
  // Row 3 (att):    CF  ST
  F_3_5_2: {
    CB:  [0.20, 0.24],   // left CB
    CB3: [0.20, 0.50],   // centre CB
    CB2: [0.20, 0.76],   // right CB
    LWB: [0.38, 0.08],
    LM:  [0.52, 0.28],
    CDM: [0.54, 0.50],
    RM:  [0.52, 0.72],
    RWB: [0.38, 0.92],
    CF:  [0.80, 0.36],
    ST:  [0.80, 0.64],
  },

  // ── 5-3-2 ──────────────────────────────────────────────────────────────────
  // Row 1 (def):    RWB  CB  CB  CB  LWB  ← LB + CB + RB fill the three CB slots
  // Row 2 (mid):    CM  CAM  CM           ← CDM fills left CM slot
  // Row 3 (att):    ST  ST
  F_5_3_2: {
    LWB: [0.28, 0.08],   // left wingback (attacking)
    LB:  [0.22, 0.26],   // left CB
    CB:  [0.20, 0.50],   // centre CB
    RB:  [0.22, 0.74],   // right CB
    RWB: [0.28, 0.92],   // right wingback (attacking)
    CDM: [0.54, 0.27],   // left of midfield three
    CAM: [0.56, 0.50],   // centre of midfield three
    CM:  [0.54, 0.73],   // right of midfield three
    CF:  [0.80, 0.36],
    ST:  [0.80, 0.64],
  },
};

export function getDisplayCoords(positionId: string, formation: string): Coords {
  const overrides = FORMATION_OVERRIDES[formation] ?? {};
  if (overrides[positionId]) return overrides[positionId]!;
  if (BASE_COORDS[positionId]) return BASE_COORDS[positionId];
  return [0.5, 0.5];
}
