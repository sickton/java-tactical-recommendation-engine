import { useState } from 'react';
import type { NetworkGraph, NetworkNode } from '../types';
import { getDisplayCoords } from '../constants/pitchCoords';

const DISPLAY_LABELS: Record<string, string> = { CB: 'LCB', CB2: 'RCB' };
const BACK_LINE_IDS = new Set(['CB', 'CB2', 'LB', 'RB', 'LWB', 'RWB']);
const SVG_W = 452; // W - 2*PX
const SVG_H = 280; // H - 2*PY

interface Props {
  pressingGraph: NetworkGraph;
  escapeGraph: NetworkGraph;
  ballCarrierId: string;
  escapeFormation: string;
  pressingFormation: string;
  highlightedPresserId?: string | null;
  threatLayerVisible?: boolean;
  pressingTeamLabel?: string;
}

const W = 500;
const H = 320;
const PX = 24;
const PY = 20;

type Role = 'harasser' | 'shadower' | 'anchor';

function toSvg(x: number, y: number): [number, number] {
  return [PX + x * SVG_W, PY + y * SVG_H];
}

function normDist(x1: number, y1: number, x2: number, y2: number): number {
  return Math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2);
}

// Formation-based base position for pressing team (mirrored)
function getBasePos(node: NetworkNode, formation: string): [number, number] {
  const [fx, fy] = getDisplayCoords(node.id, formation);
  return [1 - fx, fy];
}

// Trigger-based dynamic shift — returns normalized (dx, dy) offset from base position.
// Rules:
//   Harasser   — steps 28% toward ball carrier
//   Shadower   — drifts 14% laterally toward ball side, 10% forward
//   Backline anchor — when ball is central, gate closes (compresses y toward 0.5);
//                     when ball is wide, near-side steps out
//   Other anchor  — slight drift toward ball
function computeShift(
  id: string,
  role: Role,
  ballNormX: number,
  ballNormY: number,
  baseX: number,
  baseY: number,
): [number, number] {
  const ballDx = ballNormX - baseX;
  const ballDy = ballNormY - baseY;
  const isCentralBall = ballNormY >= 0.30 && ballNormY <= 0.70;
  const isWideBall    = ballNormY < 0.28 || ballNormY > 0.72;
  const isLeftBall    = ballNormY < 0.40;

  // Every player in the block tracks the ball laterally as a unit.
  // blockSlideY is the baseline shift — roles adjust from here.
  const blockSlideY = ballDy * 0.32;
  const blockSlideX = ballDx * 0.06;

  if (role === 'harasser') {
    // Harasser closes down fast — full lateral + forward push
    return [ballDx * 0.28, ballDy * 0.36];
  }

  if (role === 'shadower') {
    // Far-side winger tucks centrally when ball is at touchline
    const farSideWinger = isLeftBall ? 'RW' : 'LW';
    if (isWideBall && id === farSideWinger) {
      return [blockSlideX, (0.50 - baseY) * 0.42];
    }
    // CDM cuts off back-pass lane
    if (id === 'CDM') {
      return [ballDx * 0.16 + 0.04, ballDy * 0.34];
    }
    return [blockSlideX, ballDy * 0.30];
  }

  // Anchor — backline
  if (BACK_LINE_IDS.has(id)) {
    if (isCentralBall) {
      // Gate closes inward, still tracks ball laterally
      return [0.02, (0.50 - baseY) * 0.15 + blockSlideY * 0.45];
    }
    const nearSide = (isLeftBall && baseY < 0.50) || (!isLeftBall && baseY > 0.50);
    if (nearSide) return [0.03, ballDy * 0.28];
    // Far-side defender squeezes inward but still shifts with block
    return [0.01, (0.50 - baseY) * 0.12 + blockSlideY * 0.35];
  }

  // All other anchors (CM, CAM, etc.) — full block slide
  return [blockSlideX, blockSlideY];
}

function shortenLine(x1: number, y1: number, x2: number, y2: number, amount: number): [number, number] {
  const dx = x2 - x1, dy = y2 - y1;
  const len = Math.sqrt(dx * dx + dy * dy);
  if (len < amount * 2) return [x2, y2];
  return [x2 - (dx / len) * amount, y2 - (dy / len) * amount];
}

// Soft blurred cover shadow: wedge from ball through presser, extending past presser
function coverShadowPath(defX: number, defY: number, ballX: number, ballY: number, nodeR: number): string {
  const dx = defX - ballX, dy = defY - ballY;
  const dist = Math.sqrt(dx * dx + dy * dy);
  if (dist < 4) return '';
  const ux = dx / dist, uy = dy / dist;
  const perpX = -uy, perpY = ux;
  const s = nodeR + 1, e = nodeR + 65;
  const sw = nodeR * 0.35, ew = nodeR * 4.8;
  return [
    `M ${(defX + ux * s - perpX * sw).toFixed(1)},${(defY + uy * s - perpY * sw).toFixed(1)}`,
    `L ${(defX + ux * e - perpX * ew).toFixed(1)},${(defY + uy * e - perpY * ew).toFixed(1)}`,
    `L ${(defX + ux * e + perpX * ew).toFixed(1)},${(defY + uy * e + perpY * ew).toFixed(1)}`,
    `L ${(defX + ux * s + perpX * sw).toFixed(1)},${(defY + uy * s + perpY * sw).toFixed(1)} Z`,
  ].join(' ');
}

function bodyPointer(cx: number, cy: number, toX: number, toY: number, r: number): string {
  const dx = toX - cx, dy = toY - cy;
  const dist = Math.sqrt(dx * dx + dy * dy);
  if (dist < 1) return '';
  const ux = dx / dist, uy = dy / dist;
  const perpX = -uy, perpY = ux;
  const tip = [cx + ux * (r + 5), cy + uy * (r + 5)];
  const b1  = [cx + ux * (r - 2) - perpX * 3.5, cy + uy * (r - 2) - perpY * 3.5];
  const b2  = [cx + ux * (r - 2) + perpX * 3.5, cy + uy * (r - 2) + perpY * 3.5];
  return `M ${tip[0].toFixed(1)},${tip[1].toFixed(1)} L ${b1[0].toFixed(1)},${b1[1].toFixed(1)} L ${b2[0].toFixed(1)},${b2[1].toFixed(1)} Z`;
}

export default function PressureMap({
  pressingGraph, escapeGraph, ballCarrierId,
  escapeFormation, pressingFormation,
  highlightedPresserId, threatLayerVisible = false,
  pressingTeamLabel = 'Pressing team',
}: Props) {
  const [hoveredPresserId, setHoveredPresserId] = useState<string | null>(null);

  // ── Escape team: formation-based positions ────────────────────────────
  const escapePos: Record<string, [number, number]> = {};
  escapeGraph.nodes.forEach(n => {
    escapePos[n.id] = getDisplayCoords(n.id, escapeFormation);
  });

  const ballPos   = escapePos[ballCarrierId] ?? [0.38, 0.5];
  const ballNormX = ballPos[0];
  const ballNormY = ballPos[1];
  const [ballSvgX, ballSvgY] = toSvg(ballNormX, ballNormY);

  // ── Pressing team: base formation positions ───────────────────────────
  const pressBasePos: Record<string, [number, number]> = {};
  pressingGraph.nodes.forEach(pn => {
    pressBasePos[pn.id] = getBasePos(pn, pressingFormation);
  });

  // Role assignment on base positions
  const sortedByBase = [...pressingGraph.nodes].sort((a, b) => {
    const [ax, ay] = pressBasePos[a.id];
    const [bx, by] = pressBasePos[b.id];
    return normDist(ax, ay, ballNormX, ballNormY) - normDist(bx, by, ballNormX, ballNormY);
  });
  const harasserId  = sortedByBase[0]?.id;
  const shadowerIds = new Set(sortedByBase.slice(1, 4).map(n => n.id));
  const roleOf = (id: string): Role =>
    id === harasserId ? 'harasser' : shadowerIds.has(id) ? 'shadower' : 'anchor';

  // Dynamic shifts based on roles + ball position
  const shiftMap: Record<string, [number, number]> = {};
  pressingGraph.nodes.forEach(pn => {
    const [bx, by] = pressBasePos[pn.id];
    shiftMap[pn.id] = computeShift(pn.id, roleOf(pn.id), ballNormX, ballNormY, bx, by);
  });

  // Visual (post-shift) positions for all calculations
  const pressPos: Record<string, [number, number]> = {};
  pressingGraph.nodes.forEach(pn => {
    const [bx, by] = pressBasePos[pn.id];
    const [sx, sy] = shiftMap[pn.id];
    pressPos[pn.id] = [
      Math.max(0.05, Math.min(0.94, bx + sx)),
      Math.max(0.05, Math.min(0.94, by + sy)),
    ];
  });

  // ── Trap node + Lure runner ───────────────────────────────────────────
  // trapNodeId: escape player most isolated (kill zone target)
  // lureRunnerId: same player — when they make a run, a presser follows, creating vacuum
  const { trapNodeId, lureRunnerId, lurePresserIdForRunner } = (() => {
    let maxMinDist = 0, candidate: string | null = null;
    const minDistMap: Record<string, number> = {};
    escapeGraph.nodes.forEach(en => {
      if (en.id === ballCarrierId) return;
      const [ex, ey] = escapePos[en.id] ?? [en.x, en.y];
      const minD = Math.min(...pressingGraph.nodes.map(pn => {
        const [px, py] = pressPos[pn.id];
        return normDist(px, py, ex, ey);
      }));
      minDistMap[en.id] = minD;
      if (minD > maxMinDist) { maxMinDist = minD; candidate = en.id; }
    });
    const trap = maxMinDist > 0.20 ? candidate : null;
    const lure = maxMinDist > 0.22 ? candidate : null;

    // Nearest presser to the lure runner (they'll "follow" the run)
    let lurePresser: string | null = null;
    if (lure) {
      const [lx, ly] = escapePos[lure] ?? [0.5, 0.5];
      let minD = Infinity;
      pressingGraph.nodes.forEach(pn => {
        const [px, py] = pressPos[pn.id];
        const d = normDist(px, py, lx, ly);
        if (d < minD) { minD = d; lurePresser = pn.id; }
      });
    }
    return { trapNodeId: trap, lureRunnerId: lure, lurePresserIdForRunner: lurePresser };
  })();

  // ── High line ─────────────────────────────────────────────────────────
  const backLinePressers = pressingGraph.nodes.filter(n => BACK_LINE_IDS.has(n.id));
  const highLineX = backLinePressers.length > 0
    ? backLinePressers.reduce((s, n) => s + pressPos[n.id][0], 0) / backLinePressers.length
    : null;
  const highLineSvgX = highLineX !== null ? PX + highLineX * SVG_W : null;

  // ── Hover / link state ────────────────────────────────────────────────
  const activePresserId = hoveredPresserId ?? highlightedPresserId ?? null;
  const markedEscapeId = (() => {
    if (!activePresserId) return null;
    const [px, py] = pressPos[activePresserId] ?? [0, 0];
    let nearest: string | null = null, minD = Infinity;
    escapeGraph.nodes.forEach(en => {
      if (en.id === ballCarrierId) return;
      const [ex, ey] = escapePos[en.id] ?? [en.x, en.y];
      const d = normDist(px, py, ex, ey);
      if (d < minD) { minD = d; nearest = en.id; }
    });
    return nearest;
  })();

  // Cage: harasser + 2 nearest shadowers
  const cageNodes = [harasserId, ...sortedByBase.slice(1, 3).map(n => n.id)].filter(Boolean);

  // Intel bar
  const intelText = (() => {
    if (activePresserId) {
      const role = roleOf(activePresserId);
      const marked = markedEscapeId ? (DISPLAY_LABELS[markedEscapeId] ?? markedEscapeId) : '?';
      if (role === 'harasser') return `Harasser closing down — immediate pressure, release quickly`;
      if (role === 'shadower') return `Shadower blocking lane to ${marked} — cover shadow active`;
      return `Anchor holding structure — prevents counter once press breaks`;
    }
    if (threatLayerVisible) return `Cover shadows active — hover a presser to reveal their mark`;
    return `${pressingTeamLabel} high press engaged — hover any red player for tactical intel`;
  })();

  function shouldShowShadow(id: string): boolean {
    if (!threatLayerVisible) return false;
    const role = roleOf(id);
    if (role === 'harasser') return true;
    if (role === 'shadower') return hoveredPresserId === id || highlightedPresserId === id;
    return false;
  }

  function shouldShowLock(id: string): boolean {
    const [px, py] = pressPos[id] ?? [0, 0];
    return normDist(px, py, ballNormX, ballNormY) < 0.32;
  }

  return (
    <svg width="100%" viewBox={`0 0 ${W} ${H}`} className="pitch-svg">
      <defs>
        <pattern id="pm-grass" x={PX} y={PY} width={SVG_W} height="28" patternUnits="userSpaceOnUse">
          <rect width={SVG_W} height="28" fill="#122012" />
          <rect width={SVG_W} height="14" fill="#1a2e1a" />
        </pattern>
        <pattern id="pm-hatch" patternUnits="userSpaceOnUse" width="10" height="10" patternTransform="rotate(45)">
          <rect width="4" height="10" fill="rgba(227,179,65,0.28)" />
        </pattern>
        <filter id="pm-shadow-blur" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="8" />
        </filter>
        <filter id="pm-web-glow" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation="2.5" result="b" />
          <feMerge><feMergeNode in="b" /><feMergeNode in="SourceGraphic" /></feMerge>
        </filter>
        <marker id="pm-arrow" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto">
          <path d="M 0 0 L 6 3 L 0 6 z" fill="rgba(248,81,73,0.95)" />
        </marker>
        <marker id="pm-lure-arrow" markerWidth="7" markerHeight="7" refX="5" refY="3.5" orient="auto">
          <path d="M 0 0 L 7 3.5 L 0 7 z" fill="rgba(0,230,180,0.85)" />
        </marker>
        <marker id="pm-reaction-arrow" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto">
          <path d="M 0 0 L 6 3 L 0 6 z" fill="rgba(248,81,73,0.55)" />
        </marker>
        <radialGradient id="pm-ball-glow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="rgba(255,210,60,0.45)" />
          <stop offset="100%" stopColor="rgba(255,210,60,0)" />
        </radialGradient>
        <radialGradient id="pm-press-zone" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="rgba(248,81,73,0.18)" />
          <stop offset="100%" stopColor="rgba(248,81,73,0)" />
        </radialGradient>
        <radialGradient id="pm-danger-heat" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="rgba(248,81,73,0.55)" />
          <stop offset="55%" stopColor="rgba(248,81,73,0.15)" />
          <stop offset="100%" stopColor="rgba(248,81,73,0)" />
        </radialGradient>
        <radialGradient id="pm-halo-harasser" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="rgba(248,81,73,0.38)" />
          <stop offset="65%" stopColor="rgba(248,81,73,0.07)" />
          <stop offset="100%" stopColor="rgba(248,81,73,0)" />
        </radialGradient>
        <radialGradient id="pm-trap-zone" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="rgba(227,179,65,0.14)" />
          <stop offset="100%" stopColor="rgba(227,179,65,0)" />
        </radialGradient>
      </defs>

      {/* Pitch */}
      <rect x={PX} y={PY} width={SVG_W} height={SVG_H} fill="url(#pm-grass)" rx="4" />
      <rect x={PX} y={PY} width={SVG_W} height={SVG_H}
        fill="none" stroke="rgba(255,255,255,0.14)" strokeWidth="1.5" rx="4" />
      <line x1={W / 2} y1={PY} x2={W / 2} y2={H - PY} stroke="rgba(255,255,255,0.1)" strokeWidth="1.5" />
      <circle cx={W / 2} cy={H / 2} r={40} fill="none" stroke="rgba(255,255,255,0.1)" strokeWidth="1.5" />
      <circle cx={W / 2} cy={H / 2} r={2} fill="rgba(255,255,255,0.25)" />
      <rect x={PX} y={H / 2 - 66} width={82} height={132} fill="none" stroke="rgba(255,255,255,0.09)" strokeWidth="1" />
      <rect x={PX} y={H / 2 - 30} width={28} height={60} fill="none" stroke="rgba(255,255,255,0.07)" strokeWidth="1" />
      <rect x={W - PX - 82} y={H / 2 - 66} width={82} height={132} fill="none" stroke="rgba(255,255,255,0.09)" strokeWidth="1" />
      <rect x={W - PX - 28} y={H / 2 - 30} width={28} height={60} fill="none" stroke="rgba(255,255,255,0.07)" strokeWidth="1" />

      {/* High line shadow zone — "space in behind" is the win condition */}
      {highLineSvgX !== null && (
        <>
          <rect
            x={highLineSvgX} y={PY}
            width={W - PX - highLineSvgX} height={SVG_H}
            fill="rgba(0,0,0,0.18)" rx="2"
          />
          <line x1={highLineSvgX} y1={PY + 2} x2={highLineSvgX} y2={H - PY - 20}
            stroke="rgba(248,140,60,0.55)" strokeWidth="2" strokeDasharray="6 4" />
          <text x={highLineSvgX + 7} y={PY + 14}
            fill="rgba(248,140,60,0.45)" fontSize="7" fontWeight="600"
            style={{ userSelect: 'none', pointerEvents: 'none' }}>
            SPACE IN BEHIND
          </text>
        </>
      )}

      {/* Kill zone hatch */}
      {threatLayerVisible && trapNodeId && (() => {
        const [tNx, tNy] = escapePos[trapNodeId] ?? [0.5, 0.5];
        const [tx, ty]   = toSvg(tNx, tNy);
        const nearTop    = tNy < 0.5;
        const touchlineY = nearTop ? PY : H - PY;
        const pts = [
          `${tx - 28},${ty}`, `${tx + 28},${ty}`,
          `${Math.min(W - PX, tx + 56)},${touchlineY}`,
          `${Math.max(PX, tx - 56)},${touchlineY}`,
        ].join(' ');
        return (
          <g>
            <polygon points={pts} fill="url(#pm-hatch)" opacity={0.65} />
            <polygon points={pts} fill="none"
              stroke="rgba(227,179,65,0.28)" strokeWidth="1" strokeDasharray="5 3" />
          </g>
        );
      })()}

      {/* High pressure zone */}
      <circle cx={ballSvgX} cy={ballSvgY} r={68} fill="url(#pm-press-zone)" />

      {/* Trap glow */}
      {trapNodeId && (() => {
        const [tNx, tNy] = escapePos[trapNodeId] ?? [0.5, 0.5];
        const [tx, ty]   = toSvg(tNx, tNy);
        return <circle cx={tx} cy={ty} r={42} fill="url(#pm-trap-zone)" />;
      })()}

      {/* ── Ghost circles — vacated positions when shift is significant ── */}
      {pressingGraph.nodes.map(node => {
        const [sx, sy] = shiftMap[node.id] ?? [0, 0];
        const shiftMag = Math.abs(sx) + Math.abs(sy);
        if (shiftMag < 0.045) return null;
        const [bx, by] = pressBasePos[node.id];
        const [gcx, gcy] = toSvg(bx, by);
        return (
          <circle key={`ghost-${node.id}`}
            cx={gcx} cy={gcy} r={9}
            fill="none"
            stroke="rgba(248,81,73,0.20)"
            strokeWidth="1"
            strokeDasharray="3 3"
          />
        );
      })}

      {/* ── Pressing cage web ── */}
      {threatLayerVisible && cageNodes.length >= 2 && (() => {
        const pairs: [string, string][] = [];
        for (let i = 0; i < cageNodes.length; i++)
          for (let j = i + 1; j < cageNodes.length; j++)
            pairs.push([cageNodes[i], cageNodes[j]]);
        return pairs.map(([a, b]) => {
          const pa = pressPos[a], pb = pressPos[b];
          if (!pa || !pb) return null;
          const [ax, ay] = toSvg(pa[0], pa[1]);
          const [bx, by] = toSvg(pb[0], pb[1]);
          return (
            <line key={`web-${a}-${b}`}
              x1={ax} y1={ay} x2={bx} y2={by}
              stroke="rgba(248,81,73,0.28)"
              strokeWidth="1.2"
              strokeDasharray="4 4"
              filter="url(#pm-web-glow)"
            />
          );
        });
      })()}

      {/* ── Cover shadows ── */}
      {pressingGraph.nodes.map(node => {
        if (!shouldShowShadow(node.id)) return null;
        const [px, py] = pressPos[node.id];
        const [cx, cy] = toSvg(px, py);
        const d = coverShadowPath(cx, cy, ballSvgX, ballSvgY, 11);
        if (!d) return null;
        return (
          <path key={`shadow-${node.id}`} d={d}
            fill={roleOf(node.id) === 'harasser' ? 'rgba(248,81,73,0.58)' : 'rgba(248,81,73,0.40)'}
            filter="url(#pm-shadow-blur)" />
        );
      })}

      {/* ── Escape ghost nodes ── */}
      {escapeGraph.nodes.map(node => {
        const [ex, ey] = escapePos[node.id] ?? [node.x, node.y];
        const [cx, cy] = toSvg(ex, ey);
        const isBall    = node.id === ballCarrierId;
        const isTrap    = node.id === trapNodeId;
        const isMarked  = node.id === markedEscapeId;
        const isCovered = activePresserId !== null && !isBall && !isMarked;
        return (
          <g key={`esc-${node.id}`}>
            {isBall && <circle cx={cx} cy={cy} r={28} fill="url(#pm-ball-glow)" />}
            {isMarked && activePresserId && (
              <circle cx={cx} cy={cy} r={26} fill="url(#pm-danger-heat)" />
            )}
            {isTrap && (
              <circle cx={cx} cy={cy} r={20} fill="none"
                stroke="rgba(227,179,65,0.65)" strokeWidth="1.5" strokeDasharray="5 3" />
            )}
            <circle cx={cx} cy={cy}
              r={isBall ? 14 : 11}
              fill={isBall ? 'rgba(88,166,255,0.88)' : isMarked ? 'rgba(248,81,73,0.22)' : 'rgba(88,166,255,0.22)'}
              stroke={isBall ? 'rgba(240,200,48,0.95)' : isMarked ? 'rgba(248,81,73,0.80)' : isTrap ? 'rgba(227,179,65,0.55)' : 'rgba(88,166,255,0.32)'}
              strokeWidth={isBall ? 2.5 : isMarked ? 1.5 : 1}
              opacity={isCovered ? 0.36 : 1}
            />
            {isTrap && (
              <text x={cx} y={cy - 18} textAnchor="middle"
                fill="rgba(227,179,65,0.9)" fontSize="8.5" fontWeight="900"
                style={{ pointerEvents: 'none', userSelect: 'none' }}>!</text>
            )}
            <text x={cx} y={cy + 4} textAnchor="middle"
              fill={isBall ? 'white' : isMarked ? 'rgba(248,81,73,0.75)' : isCovered ? 'rgba(88,166,255,0.22)' : 'rgba(88,166,255,0.62)'}
              fontSize={(DISPLAY_LABELS[node.id] ?? node.id).length > 2 ? '6' : '7'}
              fontWeight={isBall ? '700' : '500'}
              style={{ pointerEvents: 'none', userSelect: 'none' }}>
              {DISPLAY_LABELS[node.id] ?? node.id}
            </text>
          </g>
        );
      })}

      {/* ── Lure: escape player tracking run + presser reaction ── */}
      {threatLayerVisible && lureRunnerId && (() => {
        const [lx, ly] = escapePos[lureRunnerId] ?? [0.5, 0.5];
        const [lsvgX, lsvgY] = toSvg(lx, ly);

        // Run direction: away from the pressing cluster (toward open space)
        const clusterX = pressingGraph.nodes.reduce((s, n) => s + pressPos[n.id][0], 0) / pressingGraph.nodes.length;
        const clusterY = pressingGraph.nodes.reduce((s, n) => s + pressPos[n.id][1], 0) / pressingGraph.nodes.length;
        const rdx = lx - clusterX, rdy = ly - clusterY;
        const rlen = Math.sqrt(rdx * rdx + rdy * rdy) || 1;
        const runEndX = lsvgX + (rdx / rlen) * 38;
        const runEndY = lsvgY + (rdy / rlen) * 38;

        // Presser reaction: nearest presser steps toward where runner is going
        const reactionEl = lurePresserIdForRunner ? (() => {
          const [rpx, rpy] = pressPos[lurePresserIdForRunner];
          const [rsvgX, rsvgY] = toSvg(rpx, rpy);
          const [rx2, ry2] = shortenLine(rsvgX, rsvgY, runEndX, runEndY, 18);
          return (
            <line
              x1={rsvgX} y1={rsvgY} x2={rx2} y2={ry2}
              stroke="rgba(248,81,73,0.42)" strokeWidth={2}
              strokeLinecap="round" strokeDasharray="4 3"
              markerEnd="url(#pm-reaction-arrow)"
            />
          );
        })() : null;

        return (
          <g key="lure-group">
            {/* Glow at run destination */}
            <circle cx={runEndX} cy={runEndY} r={14}
              fill="rgba(0,230,180,0.09)" stroke="rgba(0,230,180,0.25)"
              strokeWidth="1" strokeDasharray="3 3" />
            {/* Runner's movement arrow */}
            <line
              x1={lsvgX} y1={lsvgY} x2={runEndX} y2={runEndY}
              stroke="rgba(0,230,180,0.70)" strokeWidth={2.2}
              strokeLinecap="round" strokeDasharray="5 3"
              markerEnd="url(#pm-lure-arrow)"
            />
            {/* Presser following */}
            {reactionEl}
          </g>
        );
      })()}

      {/* Harasser charge arrow — pulsing opacity */}
      {pressingGraph.nodes.map(node => {
        if (roleOf(node.id) !== 'harasser') return null;
        const [px, py] = pressPos[node.id];
        const [sx, sy] = toSvg(px, py);
        if (normDist(px, py, ballNormX, ballNormY) < 0.06) return null;
        const [ex, ey] = shortenLine(sx, sy, ballSvgX, ballSvgY, 20);
        return (
          <line key={`arr-${node.id}`}
            x1={sx} y1={sy} x2={ex} y2={ey}
            stroke="rgba(248,81,73,0.88)" strokeWidth={3.5}
            strokeLinecap="round" markerEnd="url(#pm-arrow)"
          >
            <animate attributeName="opacity" values="1;0.55;1" dur="0.9s" repeatCount="indefinite" />
            <animate attributeName="stroke-width" values="3.5;2.2;3.5" dur="0.9s" repeatCount="indefinite" />
          </line>
        );
      })}

      {escapeGraph.nodes.some(n => n.id === ballCarrierId) && (
        <circle cx={ballSvgX - 16} cy={ballSvgY - 16} r={6}
          fill="#f0c830" stroke="white" strokeWidth="1.5" />
      )}

      {/* ── Pressing nodes — animated via CSS transform transition ── */}
      {pressingGraph.nodes.map(node => {
        const [bx, by]  = pressBasePos[node.id];
        const [sx, sy]  = shiftMap[node.id];
        const [px, py]  = pressPos[node.id];
        const [bcx, bcy] = toSvg(bx, by);   // base SVG coords (render target)
        // CSS pixel delta = shift in normalized * SVG dims
        const tdx = sx * SVG_W;
        const tdy = sy * SVG_H;

        const dist      = normDist(px, py, ballNormX, ballNormY);
        const proximity = Math.max(0, 1 - dist / 0.55);
        const role      = roleOf(node.id);
        const isHov     = node.id === hoveredPresserId;
        const isLinked  = node.id === highlightedPresserId;

        const nodeR = role === 'harasser' ? 12 + proximity * 2 : 10;
        const fillOpacity =
          role === 'harasser' ? 0.88 + proximity * 0.10
          : role === 'shadower' ? 0.52
          : 0.30;
        const strokeW     = role === 'harasser' ? 2.2 : role === 'shadower' ? 1.5 : 1.0;
        const strokeColor = role === 'harasser' ? 'rgba(255,255,255,0.55)'
          : role === 'shadower' ? 'rgba(255,255,255,0.30)'
          : 'rgba(255,255,255,0.14)';

        // Body pointer direction (using visual positions for nearest escape target)
        let facingSvgX = ballSvgX, facingSvgY = ballSvgY;
        if (role === 'shadower') {
          let nearId: string | null = null, minD = Infinity;
          escapeGraph.nodes.forEach(en => {
            if (en.id === ballCarrierId) return;
            const [ex, ey] = escapePos[en.id] ?? [en.x, en.y];
            const d = normDist(px, py, ex, ey);
            if (d < minD) { minD = d; nearId = en.id; }
          });
          if (nearId) {
            const [ex, ey] = escapePos[nearId] ?? [0.5, 0.5];
            [facingSvgX, facingSvgY] = toSvg(ex, ey);
            // Compensate for group transform so pointer faces correct direction
            facingSvgX -= tdx; facingSvgY -= tdy;
          }
        } else if (role === 'anchor') {
          facingSvgX = bcx - 50; facingSvgY = bcy;
        } else {
          // Harasser faces ball — compensate transform
          facingSvgX = ballSvgX - tdx; facingSvgY = ballSvgY - tdy;
        }

        const pointerD  = bodyPointer(bcx, bcy, facingSvgX + tdx, facingSvgY + tdy, nodeR);
        const showLock  = role === 'anchor' && shouldShowLock(node.id);

        return (
          <g key={`press-${node.id}`}
            style={{
              transform: `translate(${tdx.toFixed(1)}px, ${tdy.toFixed(1)}px)`,
              transition: 'transform 0.48s cubic-bezier(0.34,1.56,0.64,1)',
            }}
            onMouseEnter={() => setHoveredPresserId(node.id)}
            onMouseLeave={() => setHoveredPresserId(null)}
          >
            {/* Cross-map / hover link pulse */}
            {(isLinked || isHov) && (
              <circle cx={bcx} cy={bcy} r={nodeR + 11}
                fill="none" stroke="rgba(255,240,80,0.9)" strokeWidth={2}>
                <animate attributeName="r"
                  values={`${nodeR + 7};${nodeR + 15};${nodeR + 7}`}
                  dur="0.9s" repeatCount="indefinite" />
                <animate attributeName="opacity" values="1;0.45;1" dur="0.9s" repeatCount="indefinite" />
              </circle>
            )}

            {/* Harasser halo + pulse ring */}
            {role === 'harasser' && (
              <>
                <circle cx={bcx} cy={bcy} r={28 + proximity * 18}
                  fill="url(#pm-halo-harasser)" opacity={0.38 + proximity * 0.25} />
                <circle cx={bcx} cy={bcy} r={nodeR + 5} fill="none"
                  stroke="rgba(248,81,73,0.50)" strokeWidth="1.5">
                  <animate attributeName="r" from={nodeR + 5} to={44 + proximity * 8} dur="1.4s" repeatCount="indefinite" />
                  <animate attributeName="opacity" from="0.55" to="0" dur="1.4s" repeatCount="indefinite" />
                </circle>
              </>
            )}

            {/* Shadower patrol zone ring */}
            {role === 'shadower' && (
              <circle cx={bcx} cy={bcy} r={nodeR + 9}
                fill="rgba(248,81,73,0.04)"
                stroke="rgba(248,81,73,0.26)"
                strokeWidth="1"
                strokeDasharray="4 3"
              />
            )}

            {/* Main circle */}
            <circle cx={bcx} cy={bcy} r={nodeR}
              fill={`rgba(220,60,50,${fillOpacity.toFixed(2)})`}
              stroke={strokeColor}
              strokeWidth={strokeW}
            />

            {/* Body orientation pointer */}
            {pointerD && role !== 'anchor' && (
              <path d={pointerD} fill="rgba(255,255,255,0.70)"
                style={{ pointerEvents: 'none' }} />
            )}

            {/* Lock icon — close anchors only */}
            {showLock && (
              <g style={{ pointerEvents: 'none' }}>
                <path d={`M ${bcx - 2.8},${bcy - 0.5} a 2.8 2.8 0 0 1 5.6 0`}
                  fill="none" stroke="rgba(255,255,255,0.50)" strokeWidth="1.2" strokeLinecap="round" />
                <rect x={bcx - 3.2} y={bcy - 0.5} width={6.4} height={4.5} rx={1}
                  fill="rgba(255,255,255,0.40)" />
              </g>
            )}

            <text x={bcx} y={showLock ? bcy - 7 : bcy + 4} textAnchor="middle"
              fill={role === 'anchor' ? 'rgba(255,255,255,0.48)' : 'white'}
              fontSize={(DISPLAY_LABELS[node.id] ?? node.id).length > 2 ? '5.5' : '6.5'}
              fontWeight={role === 'harasser' ? '700' : '500'}
              style={{ pointerEvents: 'none', userSelect: 'none' }}>
              {DISPLAY_LABELS[node.id] ?? node.id}
            </text>
          </g>
        );
      })}

      {/* ── Intel bar ── */}
      <g>
        <rect x={PX} y={H - PY - 18} width={SVG_W} height={16} rx={3}
          fill="rgba(10,14,20,0.72)" />
        <text x={PX + 8} y={H - PY - 6}
          fill="rgba(200,210,220,0.75)" fontSize="7.5" fontWeight="500"
          style={{ userSelect: 'none' }}>
          {intelText}
        </text>
      </g>
    </svg>
  );
}
