/**
 * TacticDiagram / FormationDiagram
 *
 * TacticDiagram (default export):
 *   Animated SVG that starts in the team's real-world formation, then
 *   spring-transitions to the tactic shape. Ball + arrows fade in after.
 *
 * FormationDiagram (named export):
 *   Static SVG of the team's formation — no arrows, no opponents, no ball.
 *   Used as the left-hand "before" panel in the two-diagram layout.
 *
 * Coordinate system — viewBox "0 0 160 230":
 *   y = 8   → opponent's goal (top)
 *   y = 222 → our goal (bottom)
 *   x = 80  → centre
 */

import { useState, useEffect } from 'react';

interface PlayerDot { x: number; y: number; ghost?: boolean; }
interface Pos        { x: number; y: number; }
interface ArrowDef  { x1: number; y1: number; x2: number; y2: number; cx?: number; cy?: number; dashed?: boolean; thick?: boolean; dim?: boolean; }
interface ZoneDef   { y: number; h: number; }
interface LineDef   { y: number; label: string; }
interface BallPos   { x: number; y: number; }

interface DiagramData {
  players: PlayerDot[];
  ball?:   BallPos;
  arrows?: ArrowDef[];
  zones?:  ZoneDef[];
  lines?:  LineDef[];
}

/* ── Shared pitch background ─────────────────────────────────────────────── */
function PitchBackground() {
  return (
    <>
      <rect x="0" y="0" width="160" height="230" rx="6" fill="#0a3214" />
      {[8, 64, 120, 176].map(sy => (
        <rect key={sy} x="8" y={sy} width="144" height="28" fill="rgba(255,255,255,0.018)" />
      ))}
      <rect x="8" y="8" width="144" height="214" fill="none"
            stroke="rgba(255,255,255,0.28)" strokeWidth="1" />
      <line x1="8" y1="115" x2="152" y2="115"
            stroke="rgba(255,255,255,0.22)" strokeWidth="0.8" />
      <circle cx="80" cy="115" r="22" fill="none"
              stroke="rgba(255,255,255,0.18)" strokeWidth="0.8" />
      <circle cx="80" cy="115" r="2" fill="rgba(255,255,255,0.38)" />
      {/* Opponent penalty area */}
      <rect x="45" y="8"  width="70" height="42" fill="none"
            stroke="rgba(255,255,255,0.18)" strokeWidth="0.8" />
      <rect x="62" y="8"  width="36" height="16" fill="none"
            stroke="rgba(255,255,255,0.13)" strokeWidth="0.6" />
      <circle cx="80" cy="40" r="1.5" fill="rgba(255,255,255,0.32)" />
      {/* Our penalty area */}
      <rect x="45" y="172" width="70" height="50" fill="none"
            stroke="rgba(255,255,255,0.18)" strokeWidth="0.8" />
      <rect x="62" y="206" width="36" height="16" fill="none"
            stroke="rgba(255,255,255,0.13)" strokeWidth="0.6" />
      <circle cx="80" cy="190" r="1.5" fill="rgba(255,255,255,0.32)" />
      {/* Goals */}
      <rect x="68" y="3"   width="24" height="6" fill="none"
            stroke="rgba(255,255,255,0.28)" strokeWidth="0.8" />
      <rect x="68" y="221" width="24" height="6" fill="none"
            stroke="rgba(255,255,255,0.28)" strokeWidth="0.8" />
    </>
  );
}

/* ── Formation starting positions ────────────────────────────────────────────
 * 11 positions per formation, right→left within each line.
 * Index layout: [0] GK  [1-4] DEF  [5-7] MID  [8-10] ATT
 *
 * Formations with <4 defenders fill the spare DEF slot with the widest
 * midfielder (wing-back / wide mid), so that player flips naturally to
 * a flanking position in any tactic.
 */
const FORMATION_POSITIONS: Record<string, Pos[]> = {
  '4-3-3': [
    { x: 80,  y: 213 },
    { x: 130, y: 175 }, { x: 98,  y: 170 }, { x: 62,  y: 170 }, { x: 30,  y: 175 },
    { x: 122, y: 130 }, { x: 80,  y: 122 }, { x: 38,  y: 130 },
    { x: 127, y: 72  }, { x: 80,  y: 62  }, { x: 33,  y: 72  },
  ],
  '4-2-3-1': [
    { x: 80,  y: 213 },
    { x: 130, y: 175 }, { x: 98,  y: 170 }, { x: 62,  y: 170 }, { x: 30,  y: 175 },
    { x: 104, y: 148 }, { x: 56,  y: 148 }, { x: 80,  y: 108 },
    { x: 127, y: 100 }, { x: 80,  y: 65  }, { x: 33,  y: 100 },
  ],
  '3-4-3': [
    { x: 80,  y: 213 },
    { x: 113, y: 175 }, { x: 80,  y: 170 }, { x: 47,  y: 175 }, { x: 22,  y: 132 },
    { x: 138, y: 132 }, { x: 95,  y: 127 }, { x: 65,  y: 127 },
    { x: 127, y: 72  }, { x: 80,  y: 62  }, { x: 33,  y: 72  },
  ],
  '3-5-2': [
    { x: 80,  y: 213 },
    { x: 100, y: 175 }, { x: 80,  y: 170 }, { x: 60,  y: 175 }, { x: 142, y: 138 },
    { x: 18,  y: 138 }, { x: 108, y: 125 }, { x: 80,  y: 118 },
    { x: 52,  y: 125 }, { x: 105, y: 70  }, { x: 55,  y: 70  },
  ],
  '5-3-2': [
    { x: 80,  y: 213 },
    { x: 144, y: 172 }, { x: 112, y: 168 }, { x: 80,  y: 165 }, { x: 48,  y: 168 },
    { x: 16,  y: 172 }, { x: 108, y: 125 }, { x: 80,  y: 120 },
    { x: 52,  y: 125 }, { x: 105, y: 70  }, { x: 55,  y: 70  },
  ],
};

/* ── Per-tactic layout data ──────────────────────────────────────────────── */
const DIAGRAMS: Record<string, DiagramData> = {

  GEGENPRESSING: {
    players: [
      { x: 80,  y: 213 },
      { x: 128, y: 148 }, { x: 100, y: 140 }, { x: 60, y: 140 }, { x: 32, y: 148 },
      { x: 120, y: 98  }, { x: 80,  y: 90  }, { x: 40, y: 98  },
      { x: 125, y: 52  }, { x: 80,  y: 42  }, { x: 35, y: 52  },
      { x: 74,  y: 68, ghost: true }, { x: 96,  y: 60, ghost: true },
      { x: 58,  y: 74, ghost: true },
      { x: 55,  y: 30, ghost: true }, { x: 105, y: 30, ghost: true },
    ],
    ball: { x: 82, y: 70 },
    arrows: [
      { x1: 125, y1: 52, x2: 103, y2: 65, cx: 114, cy: 55 },
      { x1: 80,  y1: 42, x2: 80,  y2: 63                  },
      { x1: 35,  y1: 52, x2: 57,  y2: 65, cx: 46,  cy: 55 },
      { x1: 120, y1: 98, x2: 104, y2: 80, cx: 114, cy: 86 },
      { x1: 40,  y1: 98, x2: 56,  y2: 80, cx: 46,  cy: 86 },
    ],
    lines: [{ y: 140, label: 'Very High Line' }],
  },

  HIGH_PRESS: {
    players: [
      { x: 80,  y: 213 },
      { x: 128, y: 155 }, { x: 100, y: 147 }, { x: 60, y: 147 }, { x: 32, y: 155 },
      { x: 120, y: 108 }, { x: 80,  y: 100 }, { x: 40, y: 108 },
      { x: 125, y: 58  }, { x: 80,  y: 48  }, { x: 35, y: 58  },
      { x: 80,  y: 13, ghost: true }, { x: 42,  y: 28, ghost: true },
      { x: 80,  y: 26, ghost: true }, { x: 118, y: 28, ghost: true },
      { x: 30,  y: 20, ghost: true },
    ],
    ball: { x: 80, y: 14 },
    arrows: [
      { x1: 125, y1: 58, x2: 120, y2: 36 },
      { x1: 80,  y1: 48, x2: 80,  y2: 32 },
      { x1: 35,  y1: 58, x2: 40,  y2: 36 },
    ],
    lines: [{ y: 147, label: 'High Line' }],
  },

  TIKI_TAKA: {
    players: [
      { x: 80,  y: 213 },
      { x: 122, y: 170 }, { x: 98, y: 163 }, { x: 62, y: 163 }, { x: 38, y: 170 },
      { x: 118, y: 125 }, { x: 80, y: 115 }, { x: 42, y: 125 },
      { x: 118, y: 80  }, { x: 80, y: 70  }, { x: 42, y: 80  },
      { x: 55,  y: 95,  ghost: true }, { x: 105, y: 95,  ghost: true },
      { x: 65,  y: 50,  ghost: true }, { x: 112, y: 58,  ghost: true },
      { x: 80,  y: 140, ghost: true },
    ],
    ball: { x: 80, y: 70 },
    arrows: [
      { x1: 118, y1: 80,  x2: 83,  y2: 70,  cx: 101, cy: 62  },
      { x1: 80,  y1: 70,  x2: 45,  y2: 80,  cx: 62,  cy: 62  },
      { x1: 42,  y1: 80,  x2: 115, y2: 80,  cx: 80,  cy: 95  },
      { x1: 118, y1: 125, x2: 83,  y2: 115, cx: 101, cy: 107 },
      { x1: 80,  y1: 115, x2: 45,  y2: 125, cx: 62,  cy: 107 },
      { x1: 42,  y1: 125, x2: 115, y2: 125, cx: 80,  cy: 140 },
    ],
  },

  CONTROL: {
    players: [
      { x: 80,  y: 213 },
      { x: 128, y: 170 }, { x: 100, y: 163 }, { x: 60, y: 163 }, { x: 32, y: 170 },
      { x: 120, y: 125 }, { x: 80,  y: 118 }, { x: 40, y: 125 },
      { x: 120, y: 82  }, { x: 80,  y: 72  }, { x: 40, y: 82  },
      { x: 50,  y: 52, ghost: true }, { x: 80,  y: 45, ghost: true },
      { x: 110, y: 52, ghost: true }, { x: 45,  y: 95, ghost: true },
      { x: 115, y: 95, ghost: true },
    ],
    ball: { x: 80, y: 118 },
    arrows: [
      { x1: 120, y1: 125, x2: 83,  y2: 125, dim: true },
      { x1: 80,  y1: 125, x2: 43,  y2: 125, dim: true },
      { x1: 120, y1: 82,  x2: 83,  y2: 82,  dim: true },
      { x1: 80,  y1: 82,  x2: 43,  y2: 82,  dim: true },
    ],
    zones: [{ y: 72, h: 62 }],
  },

  COUNTER_ATTACK: {
    players: [
      { x: 80,  y: 213 },
      { x: 128, y: 180 }, { x: 100, y: 173 }, { x: 60, y: 173 }, { x: 32, y: 180 },
      { x: 120, y: 148 }, { x: 80,  y: 142 }, { x: 40, y: 148 },
      { x: 125, y: 132 }, { x: 80,  y: 128 }, { x: 35, y: 132 },
      { x: 50,  y: 118, ghost: true }, { x: 80,  y: 112, ghost: true },
      { x: 110, y: 118, ghost: true }, { x: 35,  y: 98,  ghost: true },
      { x: 125, y: 98,  ghost: true },
    ],
    ball: { x: 80, y: 142 },
    arrows: [
      { x1: 125, y1: 132, x2: 126, y2: 55, cx: 132, cy: 90, thick: true },
      { x1: 80,  y1: 128, x2: 80,  y2: 48,                   thick: true },
      { x1: 35,  y1: 132, x2: 34,  y2: 55, cx: 28,  cy: 90, thick: true },
    ],
    zones: [{ y: 128, h: 94 }],
  },

  DIRECT_PLAY: {
    players: [
      { x: 80,  y: 213 },
      { x: 128, y: 178 }, { x: 100, y: 172 }, { x: 60, y: 172 }, { x: 32, y: 178 },
      { x: 120, y: 132 }, { x: 80,  y: 125 }, { x: 40, y: 132 },
      { x: 125, y: 80  }, { x: 80,  y: 62  }, { x: 35, y: 80  },
      { x: 88,  y: 58, ghost: true }, { x: 68,  y: 56, ghost: true },
      { x: 80,  y: 38, ghost: true }, { x: 45,  y: 108, ghost: true },
      { x: 115, y: 108, ghost: true },
    ],
    ball: { x: 105, y: 108 },
    arrows: [
      { x1: 80, y1: 172, x2: 80, y2: 70, cx: 118, cy: 118, dashed: true, thick: true },
      { x1: 125, y1: 80, x2: 100, y2: 66, cx: 115, cy: 71, dim: true },
      { x1: 35,  y1: 80, x2: 60,  y2: 66, cx: 45,  cy: 71, dim: true },
    ],
  },

  LOW_BLOCK: {
    players: [
      { x: 80,  y: 213 },
      { x: 130, y: 192 }, { x: 102, y: 185 }, { x: 58, y: 185 }, { x: 30, y: 192 },
      { x: 122, y: 170 }, { x: 80,  y: 165 }, { x: 38, y: 170 },
      { x: 122, y: 152 }, { x: 80,  y: 148 }, { x: 38, y: 152 },
      { x: 80,  y: 105, ghost: true }, { x: 45,  y: 112, ghost: true },
      { x: 115, y: 112, ghost: true }, { x: 60,  y: 82,  ghost: true },
      { x: 100, y: 82,  ghost: true }, { x: 80,  y: 68,  ghost: true },
    ],
    ball: { x: 80, y: 107 },
    arrows: [],
    zones:  [{ y: 148, h: 74 }],
    lines:  [{ y: 148, label: 'Defensive Block' }],
  },
};

/* ── FormationDiagram (named export) ─────────────────────────────────────── */
/** Static bird's-eye view of the team's formation — no tactics, no overlays. */
export function FormationDiagram({ formation }: { formation?: string }) {
  const positions = formation ? (FORMATION_POSITIONS[formation] ?? null) : null;

  return (
    <svg viewBox="0 0 160 230" className="tac-diagram" aria-hidden="true"
         xmlns="http://www.w3.org/2000/svg">
      <PitchBackground />
      {positions?.map((p, i) => (
        <circle key={i} cx={p.x} cy={p.y} r={5.5}
                fill="rgba(255,255,255,0.92)"
                stroke="rgba(200,200,200,0.3)" strokeWidth="0.5" />
      ))}
    </svg>
  );
}

/* ── TacticDiagram (default export) ─────────────────────────────────────── */
/**
 * Animated: players spring from `formation` positions → tactic positions.
 * Arrows/ball/overlays fade in once movement is mostly done.
 * Parent should pass key={tactic} to re-mount (and re-animate) on tactic change.
 */
export default function TacticDiagram({
  tactic,
  formation,
}: {
  tactic: string;
  formation?: string;
}) {
  const data       = DIAGRAMS[tactic] ?? { players: [] };
  const ourPlayers = data.players.filter(p => !p.ghost);
  const tacticPos  = ourPlayers.map(p => ({ x: p.x, y: p.y }));
  const fromPos    = formation ? (FORMATION_POSITIONS[formation] ?? null) : null;

  const [positions, setPositions]         = useState<Pos[]>(() => fromPos ?? tacticPos);
  const [overlaysVisible, setOverlays]    = useState<boolean>(!fromPos);

  useEffect(() => {
    if (!fromPos) return;
    let raf2: number;
    const raf1 = requestAnimationFrame(() => {
      raf2 = requestAnimationFrame(() => setPositions(tacticPos));
    });
    const t = setTimeout(() => setOverlays(true), 600);
    return () => { cancelAnimationFrame(raf1); cancelAnimationFrame(raf2); clearTimeout(t); };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const playerTx = fromPos
    ? 'cx 0.7s cubic-bezier(0.34,1.56,0.64,1), cy 0.7s cubic-bezier(0.34,1.56,0.64,1)'
    : undefined;
  const overlayStyle: React.CSSProperties = {
    opacity: overlaysVisible ? 1 : 0,
    transition: 'opacity 0.4s ease',
  };

  return (
    <svg viewBox="0 0 160 230" className="tac-diagram" aria-hidden="true"
         xmlns="http://www.w3.org/2000/svg">
      <defs>
        <marker id="td-arr"       markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto">
          <polygon points="0 0, 5 2.5, 0 5" fill="rgba(255,255,255,0.82)" />
        </marker>
        <marker id="td-arr-dim"   markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto">
          <polygon points="0 0, 5 2.5, 0 5" fill="rgba(255,255,255,0.32)" />
        </marker>
        <marker id="td-arr-thick" markerWidth="6" markerHeight="6" refX="5.5" refY="3" orient="auto">
          <polygon points="0 0, 6 3, 0 6"   fill="rgba(255,255,255,0.9)"  />
        </marker>
      </defs>

      <PitchBackground />

      {/* Zones & Lines */}
      <g style={overlayStyle}>
        {data.zones?.map((z, i) => (
          <rect key={i} x="8" y={z.y} width="144" height={z.h}
                fill="rgba(255,255,255,0.06)" />
        ))}
        {data.lines?.map((l, i) => (
          <g key={i}>
            <line x1="8" y1={l.y} x2="152" y2={l.y}
                  stroke="rgba(255,210,80,0.65)" strokeWidth="1" strokeDasharray="4 3" />
            <text x="11" y={l.y - 3} fontSize="6" fontFamily="sans-serif"
                  fill="rgba(255,210,80,0.7)">{l.label}</text>
          </g>
        ))}
      </g>

      {/* Ghost opponents — always visible */}
      {data.players.filter(p => p.ghost).map((p, i) => (
        <circle key={`g${i}`} cx={p.x} cy={p.y} r={5}
                fill="rgba(220,80,60,0.18)"
                stroke="rgba(255,100,80,0.7)" strokeWidth="1.4" />
      ))}

      {/* Our players — CSS-transitioned */}
      {positions.map((p, i) => (
        <circle key={`p${i}`} cx={p.x} cy={p.y} r={5.5}
                fill="rgba(255,255,255,0.92)"
                stroke="rgba(200,200,200,0.3)" strokeWidth="0.5"
                style={{ transition: playerTx }} />
      ))}

      {/* Arrows */}
      <g style={overlayStyle}>
        {data.arrows?.map((a, i) => {
          const d = a.cx !== undefined
            ? `M ${a.x1} ${a.y1} Q ${a.cx} ${a.cy} ${a.x2} ${a.y2}`
            : `M ${a.x1} ${a.y1} L ${a.x2} ${a.y2}`;
          const mid = a.dim ? 'td-arr-dim' : a.thick ? 'td-arr-thick' : 'td-arr';
          return (
            <path key={i} d={d} fill="none"
                  stroke={a.dim ? 'rgba(255,255,255,0.32)' : 'rgba(255,255,255,0.8)'}
                  strokeWidth={a.thick ? 2.2 : 1.4}
                  strokeDasharray={a.dashed ? '5 3' : undefined}
                  strokeLinecap="round" markerEnd={`url(#${mid})`} />
          );
        })}
      </g>

      {/* Ball */}
      {data.ball && (
        <g style={overlayStyle}>
          <circle cx={data.ball.x} cy={data.ball.y} r={3.8}
                  fill="white" stroke="#061a0a" strokeWidth="1.4" />
          <line x1={data.ball.x - 2} y1={data.ball.y - 1.2}
                x2={data.ball.x + 2} y2={data.ball.y - 1.2}
                stroke="#061a0a" strokeWidth="0.6" strokeLinecap="round" />
          <line x1={data.ball.x} y1={data.ball.y - 3}
                x2={data.ball.x} y2={data.ball.y + 2.2}
                stroke="#061a0a" strokeWidth="0.6" strokeLinecap="round" />
        </g>
      )}
    </svg>
  );
}
