import { useState } from 'react';
import type { NetworkGraph, NetworkEdge } from '../types';
import { getDisplayCoords } from '../constants/pitchCoords';

const DISPLAY_LABELS: Record<string, string> = {
  CB:  'LCB',
  CB2: 'RCB',
};

interface Props {
  graph: NetworkGraph;
  formation: string;
  interactive: boolean;
  highlightSequence: string[];
  onNodeClick: (nodeId: string) => void;
  ballNodeId?: string;
  onEdgeHover?: (edge: { from: string; to: string } | null) => void;
  optimalNodeId?: string | null;
  vacatedZones?: Array<{ x: number; y: number }>;
}

const W = 500;
const H = 320;
const PX = 24;
const PY = 20;

function toSvg(normX: number, normY: number): [number, number] {
  return [PX + normX * (W - 2 * PX), PY + normY * (H - 2 * PY)];
}

// Quadratic bezier arc — curves longer passes above the straight line
function curvedPath(x1: number, y1: number, x2: number, y2: number): string {
  const mx = (x1 + x2) / 2;
  const my = (y1 + y2) / 2;
  const dx = x2 - x1, dy = y2 - y1;
  const len = Math.sqrt(dx * dx + dy * dy);
  if (len < 1) return `M ${x1},${y1} L ${x2},${y2}`;
  const offset = len * 0.16;
  const cpx = mx - (dy / len) * offset;
  const cpy = my + (dx / len) * offset;
  return `M ${x1},${y1} Q ${cpx},${cpy} ${x2},${y2}`;
}

// Midpoint of quadratic bezier at t=0.5
function curveMid(x1: number, y1: number, x2: number, y2: number): [number, number] {
  const mx = (x1 + x2) / 2;
  const my = (y1 + y2) / 2;
  const dx = x2 - x1, dy = y2 - y1;
  const len = Math.sqrt(dx * dx + dy * dy);
  if (len < 1) return [mx, my];
  const offset = len * 0.16;
  const cpx = mx - (dy / len) * offset;
  const cpy = my + (dx / len) * offset;
  // t=0.5: 0.25*P1 + 0.5*CP + 0.25*P2
  return [0.25 * x1 + 0.5 * cpx + 0.25 * x2, 0.25 * y1 + 0.5 * cpy + 0.25 * y2];
}

function edgeStroke(edge: NetworkEdge): { color: string; width: number; dashed: boolean } {
  const p = edge.escape_prob ?? 0;
  if (p >= 0.65) return { color: 'rgba(88,166,255,0.95)', width: 2.2, dashed: false };  // blue — safe
  if (p >= 0.35) return { color: 'rgba(227,179,65,0.90)', width: 1.8, dashed: false };  // yellow — risky
  return           { color: 'rgba(248,81,73,0.85)', width: 1.4, dashed: true  };        // red — blocked lane
}

function isSeqEdge(seq: string[], from: string, to: string): boolean {
  for (let i = 0; i < seq.length - 1; i++) {
    if (seq[i] === from && seq[i + 1] === to) return true;
  }
  return false;
}

// Max outgoing escape_prob per node — used to scale node size
function buildNodeInfluence(graph: NetworkGraph): Record<string, number> {
  const inf: Record<string, number> = {};
  graph.nodes.forEach(n => { inf[n.id] = 0; });
  graph.edges.forEach(e => {
    const p = e.escape_prob ?? 0;
    if ((inf[e.from] ?? 0) < p) inf[e.from] = p;
  });
  return inf;
}

export default function PitchGraph({
  graph, formation, interactive, highlightSequence, onNodeClick, ballNodeId, onEdgeHover,
  optimalNodeId, vacatedZones = [],
}: Props) {
  const [hoveredNode, setHoveredNode] = useState<string | null>(null);
  const [hoveredEdgeKey, setHoveredEdgeKey] = useState<string | null>(null);
  const [hoveredEdgeFull, setHoveredEdgeFull] = useState<NetworkEdge | null>(null);
  const nodeMap: Record<string, { id: string; x: number; y: number }> = {};
  graph.nodes.forEach(n => {
    const [dx, dy] = getDisplayCoords(n.id, formation);
    nodeMap[n.id] = { id: n.id, x: dx, y: dy };
  });
  const nodeInfluence = buildNodeInfluence(graph);

  // The player currently holding the ball (last in sequence)
  const currentCarrier = highlightSequence[highlightSequence.length - 1] ?? ballNodeId;

  // Escape probability FROM the current carrier TO each node — drives opacity/dimming
  const nodeEscapeProb: Record<string, number> = {};
  graph.nodes.forEach(n => {
    const edge = graph.edges.find(e => e.from === currentCarrier && e.to === n.id);
    nodeEscapeProb[n.id] = edge?.escape_prob ?? 0;
  });

  return (
    <svg
      width="100%"
      viewBox={`0 0 ${W} ${H}`}
      className={`pitch-svg${interactive ? ' pitch-interactive' : ''}`}
    >
      <defs>
        <radialGradient id="ball-glow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="rgba(255,210,60,0.5)" />
          <stop offset="100%" stopColor="rgba(255,210,60,0)" />
        </radialGradient>
        <radialGradient id="pg-exposed-zone" cx="40%" cy="50%" r="60%">
          <stop offset="0%" stopColor="rgba(0,230,100,0.32)" />
          <stop offset="100%" stopColor="rgba(0,230,100,0)" />
        </radialGradient>
        <radialGradient id="pg-vacated-glow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="rgba(0,230,100,0.28)" />
          <stop offset="100%" stopColor="rgba(0,230,100,0)" />
        </radialGradient>
        <pattern id="pg-grass" x={PX} y={PY} width={W - 2 * PX} height="28" patternUnits="userSpaceOnUse">
          <rect width={W - 2 * PX} height="28" fill="#122012" />
          <rect width={W - 2 * PX} height="14" fill="#1a2e1a" />
        </pattern>
      </defs>

      {/* Pitch surface */}
      <rect x={PX} y={PY} width={W - 2 * PX} height={H - 2 * PY} fill="url(#pg-grass)" rx="4" />
      <rect x={PX} y={PY} width={W - 2 * PX} height={H - 2 * PY}
        fill="none" stroke="rgba(255,255,255,0.14)" strokeWidth="1.5" rx="4" />
      <line x1={W / 2} y1={PY} x2={W / 2} y2={H - PY}
        stroke="rgba(255,255,255,0.1)" strokeWidth="1.5" />
      <circle cx={W / 2} cy={H / 2} r={40}
        fill="none" stroke="rgba(255,255,255,0.1)" strokeWidth="1.5" />
      <circle cx={W / 2} cy={H / 2} r={2} fill="rgba(255,255,255,0.25)" />
      <rect x={PX} y={H / 2 - 66} width={82} height={132}
        fill="none" stroke="rgba(255,255,255,0.09)" strokeWidth="1" />
      <rect x={PX} y={H / 2 - 30} width={28} height={60}
        fill="none" stroke="rgba(255,255,255,0.07)" strokeWidth="1" />
      <rect x={W - PX - 82} y={H / 2 - 66} width={82} height={132}
        fill="none" stroke="rgba(255,255,255,0.09)" strokeWidth="1" />
      <rect x={W - PX - 28} y={H / 2 - 30} width={28} height={60}
        fill="none" stroke="rgba(255,255,255,0.07)" strokeWidth="1" />

      {/* ── Edges: ghost everything except options from current carrier ── */}
      {graph.edges.map((edge, i) => {
        const fn = nodeMap[edge.from];
        const tn = nodeMap[edge.to];
        if (!fn || !tn) return null;
        const [x1, y1] = toSvg(fn.x, fn.y);
        const [x2, y2] = toSvg(tn.x, tn.y);
        const inSeq       = isSeqEdge(highlightSequence, edge.from, edge.to);
        const isActive    = edge.from === currentCarrier;
        const edgeKey     = `${edge.from}-${edge.to}`;
        const isEdgeHover = hoveredEdgeKey === edgeKey;
        const isHovered   = isEdgeHover || (hoveredNode !== null && edge.from === hoveredNode && !hoveredEdgeKey);
        const ghost       = !isActive && !inSeq && !isHovered;
        const { color, width, dashed } = edgeStroke(edge);
        const d = curvedPath(x1, y1, x2, y2);
        const activeDashArray = dashed ? '5 4' : undefined;

        const visPath = (
          <path
            d={d}
            stroke={
              inSeq       ? 'rgba(0,230,180,0.95)'
              : isEdgeHover ? color
              : isHovered   ? color
              : isActive    ? color
              : 'rgba(255,255,255,0.08)'
            }
            strokeWidth={inSeq ? 3 : (isActive || isHovered) ? width + 0.8 : 0.7}
            strokeLinecap="round"
            strokeDasharray={(isActive || isHovered) ? activeDashArray : undefined}
            fill="none"
            opacity={ghost ? 0.30 : 1}
            style={{ pointerEvents: 'none' }}
          />
        );

        if (!isActive || !interactive) return <g key={i}>{visPath}</g>;

        return (
          <g key={i}
            onMouseEnter={() => {
              setHoveredNode(edge.from);
              setHoveredEdgeKey(edgeKey);
              setHoveredEdgeFull(edge);
              onEdgeHover?.({ from: edge.from, to: edge.to });
            }}
            onMouseLeave={() => {
              setHoveredNode(null);
              setHoveredEdgeKey(null);
              setHoveredEdgeFull(null);
              onEdgeHover?.(null);
            }}
            style={{ cursor: 'crosshair' }}
          >
            {visPath}
            {/* Wide invisible hit area */}
            <path d={d} stroke="transparent" strokeWidth={16} fill="none" />
          </g>
        );
      })}

      {/* ── Vacated zones — green wakes where pressers have shifted away from ── */}
      {vacatedZones.map((z, i) => {
        const [zx, zy] = toSvg(z.x, z.y);
        return (
          <g key={`vac-${i}`} style={{ pointerEvents: 'none' }}>
            <circle cx={zx} cy={zy} r={26} fill="url(#pg-vacated-glow)">
              <animate attributeName="opacity" values="0.9;0.35;0.9" dur="2.4s"
                begin={`${i * 0.4}s`} repeatCount="indefinite" />
            </circle>
            <circle cx={zx} cy={zy} r={14} fill="none"
              stroke="rgba(0,230,100,0.28)" strokeWidth="1" strokeDasharray="3 3">
              <animate attributeName="opacity" values="0.7;0.15;0.7" dur="2.4s"
                begin={`${i * 0.4}s`} repeatCount="indefinite" />
            </circle>
          </g>
        );
      })}

      {/* ── Predictive arc for optimal pass target ── */}
      {interactive && optimalNodeId && currentCarrier && (() => {
        const fn = nodeMap[currentCarrier];
        const tn = nodeMap[optimalNodeId];
        if (!fn || !tn) return null;
        const [x1, y1] = toSvg(fn.x, fn.y);
        const [x2, y2] = toSvg(tn.x, tn.y);
        const dx = x2 - x1, dy = y2 - y1;
        const len = Math.sqrt(dx * dx + dy * dy) || 1;
        // Lead point — 28px ahead of target in direction of travel
        const leadX = x2 + (dx / len) * 28;
        const leadY = y2 + (dy / len) * 28;
        // Curved path going to lead point instead of node center
        const mx = (x1 + leadX) / 2, my = (y1 + leadY) / 2;
        const offset = len * 0.14;
        const cpx = mx - (dy / len) * offset;
        const cpy = my + (dx / len) * offset;
        const arcD = `M ${x1},${y1} Q ${cpx},${cpy} ${leadX},${leadY}`;
        const cr = 7; // crosshair radius
        return (
          <g style={{ pointerEvents: 'none' }}>
            {/* Predictive arc */}
            <path d={arcD} fill="none"
              stroke="rgba(0,230,180,0.50)" strokeWidth={2}
              strokeDasharray="6 4" strokeLinecap="round">
              <animate attributeName="stroke-dashoffset" from="0" to="-20"
                dur="0.8s" repeatCount="indefinite" />
            </path>
            {/* Target crosshair at lead position */}
            <circle cx={leadX} cy={leadY} r={cr + 4}
              fill="rgba(0,230,180,0.10)" stroke="rgba(0,230,180,0.45)"
              strokeWidth="1.2">
              <animate attributeName="r" values={`${cr + 3};${cr + 7};${cr + 3}`}
                dur="1.6s" repeatCount="indefinite" />
              <animate attributeName="opacity" values="0.9;0.4;0.9"
                dur="1.6s" repeatCount="indefinite" />
            </circle>
            <line x1={leadX - cr} y1={leadY} x2={leadX + cr} y2={leadY}
              stroke="rgba(0,230,180,0.80)" strokeWidth="1.5" />
            <line x1={leadX} y1={leadY - cr} x2={leadX} y2={leadY + cr}
              stroke="rgba(0,230,180,0.80)" strokeWidth="1.5" />
            <circle cx={leadX} cy={leadY} r={2.5} fill="rgba(0,230,180,0.90)" />
          </g>
        );
      })()}

      {/* Faint "attempt" lines to nodes with no graph edge from current carrier */}
      {interactive && currentCarrier && graph.nodes.map(node => {
        if (node.id === currentCarrier) return null;
        const hasEdge = graph.edges.some(e => e.from === currentCarrier && e.to === node.id);
        if (hasEdge) return null;
        const fn = nodeMap[currentCarrier];
        const tn = nodeMap[node.id];
        if (!fn || !tn) return null;
        const [x1, y1] = toSvg(fn.x, fn.y);
        const [x2, y2] = toSvg(tn.x, tn.y);
        return (
          <line key={`attempt-${node.id}`}
            x1={x1} y1={y1} x2={x2} y2={y2}
            stroke="rgba(255,255,255,0.06)"
            strokeWidth={1} strokeDasharray="3 5"
            style={{ pointerEvents: 'none' }}
          />
        );
      })}

      {/* Pass number badges along user sequence */}
      {highlightSequence.length >= 2 && highlightSequence.map((id, i) => {
        if (i >= highlightSequence.length - 1) return null;
        const fn = nodeMap[id];
        const tn = nodeMap[highlightSequence[i + 1]];
        if (!fn || !tn) return null;
        const [x1, y1] = toSvg(fn.x, fn.y);
        const [x2, y2] = toSvg(tn.x, tn.y);
        const [mx, my] = curveMid(x1, y1, x2, y2);
        return (
          <g key={`badge-${i}`}>
            <circle cx={mx} cy={my} r={8} fill="rgba(0,230,180,0.9)" />
            <text x={mx} y={my + 4} textAnchor="middle"
              fill="white" fontSize="9" fontWeight="bold"
              style={{ pointerEvents: 'none', userSelect: 'none' }}>
              {i + 1}
            </text>
          </g>
        );
      })}

      {/* ── Hovered edge overlays ── */}
      {hoveredEdgeFull && (() => {
        const fn = nodeMap[hoveredEdgeFull.from];
        const tn = nodeMap[hoveredEdgeFull.to];
        if (!fn || !tn) return null;
        const [x1, y1] = toSvg(fn.x, fn.y);
        const [x2, y2] = toSvg(tn.x, tn.y);
        const [mx, my] = curveMid(x1, y1, x2, y2);
        const p = hoveredEdgeFull.escape_prob ?? 0;
        const { color } = edgeStroke(hoveredEdgeFull);

        // Forward gain in metres (pitch ~105m)
        const gainM = Math.round((tn.x - fn.x) * 105);
        const gainLabel = gainM >= 0 ? `+${gainM}m` : `${gainM}m`;
        const gainColor = gainM > 0 ? 'rgba(0,230,180,0.95)' : 'rgba(200,200,200,0.7)';

        // Passing window: perpendicular brackets at midpoint
        // Gap width scales with escape_prob — shrinks when lane is closing
        const gapHalf = 6 + p * 18;
        const dx = x2 - x1, dy = y2 - y1;
        const len = Math.sqrt(dx * dx + dy * dy) || 1;
        const px = -dy / len, py = dx / len; // perpendicular unit vector
        const bLeft  = [mx + px * gapHalf, my + py * gapHalf];
        const bRight = [mx - px * gapHalf, my - py * gapHalf];
        const bDepth = 7;

        // Lead pass: extend line endpoint past target node for forward passes
        const isForward = tn.x > fn.x;
        const leadDx = isForward ? (dx / len) * 22 : 0;
        const leadDy = isForward ? (dy / len) * 22 : 0;

        // Tooltip dimensions
        const riskPct = Math.round((1 - p) * 100);
        const tooltipW = 108, tooltipH = 28;
        // Keep tooltip inside SVG bounds
        const tx = Math.min(Math.max(mx - tooltipW / 2, PX + 4), W - PX - tooltipW - 4);
        const ty = my - tooltipH - 10 < PY ? my + 12 : my - tooltipH - 10;

        return (
          <g style={{ pointerEvents: 'none' }}>
            {/* Exposed zone glow behind target node (space they'll run into) */}
            {isForward && p >= 0.35 && (
              <ellipse
                cx={x2 + (dx / len) * 26} cy={y2}
                rx={38} ry={22}
                fill="url(#pg-exposed-zone)"
              />
            )}

            {/* Lead pass — dashed extension past target showing where ball goes */}
            {isForward && p >= 0.50 && (
              <>
                <line
                  x1={x2} y1={y2} x2={x2 + leadDx} y2={y2 + leadDy}
                  stroke="rgba(0,230,180,0.55)" strokeWidth={2}
                  strokeDasharray="3 3" strokeLinecap="round"
                />
                <circle cx={x2 + leadDx} cy={y2 + leadDy} r={3}
                  fill="rgba(0,230,180,0.55)" />
              </>
            )}

            {/* Passing window — brackets showing how open the lane is */}
            <line
              x1={bLeft[0] - px * bDepth} y1={bLeft[1] - py * bDepth}
              x2={bLeft[0] + px * bDepth} y2={bLeft[1] + py * bDepth}
              stroke={color} strokeWidth={1.8} strokeLinecap="round" opacity={0.75}
            />
            <line
              x1={bRight[0] - px * bDepth} y1={bRight[1] - py * bDepth}
              x2={bRight[0] + px * bDepth} y2={bRight[1] + py * bDepth}
              stroke={color} strokeWidth={1.8} strokeLinecap="round" opacity={0.75}
            />
            {/* Bracket ends */}
            <line x1={bLeft[0] - px * bDepth} y1={bLeft[1] - py * bDepth}
              x2={bRight[0] - px * bDepth} y2={bRight[1] - py * bDepth}
              stroke={color} strokeWidth={1} opacity={0.30} />
            <line x1={bLeft[0] + px * bDepth} y1={bLeft[1] + py * bDepth}
              x2={bRight[0] + px * bDepth} y2={bRight[1] + py * bDepth}
              stroke={color} strokeWidth={1} opacity={0.30} />

            {/* Floating tooltip */}
            <rect x={tx} y={ty} width={tooltipW} height={tooltipH} rx={5}
              fill="rgba(8,12,20,0.88)" stroke={color} strokeWidth={0.8} />
            <text x={tx + 10} y={ty + 11} fill="rgba(180,190,200,0.80)" fontSize="7.5" fontWeight="500">
              Risk
            </text>
            <text x={tx + 30} y={ty + 11} fill={p >= 0.65 ? 'rgba(88,166,255,0.95)' : p >= 0.35 ? 'rgba(227,179,65,0.95)' : 'rgba(248,81,73,0.95)'} fontSize="7.5" fontWeight="700">
              {riskPct}%
            </text>
            <text x={tx + 60} y={ty + 11} fill="rgba(180,190,200,0.80)" fontSize="7.5" fontWeight="500">
              Gain
            </text>
            <text x={tx + 83} y={ty + 11} fill={gainColor} fontSize="7.5" fontWeight="700">
              {gainLabel}
            </text>
            {/* Second row: window status */}
            <text x={tx + 10} y={ty + 22} fill="rgba(140,150,160,0.65)" fontSize="6.5">
              {p >= 0.65 ? 'Lane open' : p >= 0.35 ? 'Lane narrowing' : 'Lane closed — wait'}
            </text>
          </g>
        );
      })()}

      {/* Ball glow + pulse at ball carrier */}
      {ballNodeId && nodeMap[ballNodeId] && (() => {
        const [bx, by] = toSvg(nodeMap[ballNodeId].x, nodeMap[ballNodeId].y);
        return (
          <g>
            <circle cx={bx} cy={by} r={26} fill="url(#ball-glow)" />
            {/* Pulse ring — emanates outward from ball carrier */}
            <circle cx={bx} cy={by} r={22} fill="none"
              stroke="rgba(240,200,48,0.55)" strokeWidth="2">
              <animate attributeName="r" from="18" to="52" dur="1.8s" repeatCount="indefinite" />
              <animate attributeName="opacity" from="0.6" to="0" dur="1.8s" repeatCount="indefinite" />
            </circle>
            <circle cx={bx - 16} cy={by - 16} r={6}
              fill="#f0c830" stroke="white" strokeWidth="1.5" />
          </g>
        );
      })()}

      {/* "Click to pass" hint */}
      {interactive && highlightSequence.length <= 1 && (
        <g>
          <rect x={W / 2 - 118} y={H - PY - 26} width={236} height={22} rx="5"
            fill="rgba(88,166,255,0.1)" stroke="rgba(88,166,255,0.25)" strokeWidth="1" />
          <text x={W / 2} y={H - PY - 11} textAnchor="middle"
            fill="rgba(88,166,255,0.8)" fontSize="9" fontWeight="600"
            style={{ userSelect: 'none' }}>
            Click a position to make the first pass
          </text>
        </g>
      )}

      {/* Nodes */}
      {graph.nodes.map(node => {
        const mapped = nodeMap[node.id];
        if (!mapped) return null;
        const [cx, cy] = toSvg(mapped.x, mapped.y);
        const idx = highlightSequence.indexOf(node.id);
        const inSeq = idx !== -1;
        const isFirst = idx === 0;
        const isLast = idx === highlightSequence.length - 1 && highlightSequence.length > 1;
        const isBallCarrier = node.id === ballNodeId;
        const isCurrentCarrier = node.id === currentCarrier;
        const fill = isFirst
          ? '#58a6ff'
          : isLast
          ? '#00e6b4'
          : inSeq
          ? '#79c0ff'
          : 'rgba(88,166,255,0.78)';
        const inf = nodeInfluence[node.id] ?? 0;
        const baseR = 11 + inf * 5;
        const r = inSeq ? Math.max(baseR, 15) : baseR;
        const isKeyOutlet = !isBallCarrier && !inSeq && inf >= 0.65 && node.id !== currentCarrier;
        const isOptimal  = node.id === optimalNodeId && !inSeq && !isBallCarrier;
        // Covered: no edge from carrier or very low escape_prob — dim the node
        const isCovered  = !inSeq && !isBallCarrier && !isOptimal
          && node.id !== currentCarrier && nodeEscapeProb[node.id] < 0.28;

        return (
          <g key={node.id}
            onClick={() => interactive && !isFirst && onNodeClick(node.id)}
            onMouseEnter={() => interactive && setHoveredNode(node.id)}
            onMouseLeave={() => interactive && setHoveredNode(null)}
            style={{ cursor: interactive && !isFirst ? 'pointer' : 'default',
                     opacity: isCovered ? 0.45 : 1 }}>
            {isBallCarrier && (
              <circle cx={cx} cy={cy} r={r + 8}
                fill="none" stroke="rgba(240,200,48,0.4)" strokeWidth="1.5" strokeDasharray="4 3" />
            )}
            {isCurrentCarrier && !isBallCarrier && (
              <circle cx={cx} cy={cy} r={r + 8}
                fill="none" stroke="rgba(0,230,180,0.45)" strokeWidth="1.5" strokeDasharray="4 3" />
            )}
            {isKeyOutlet && (
              <circle cx={cx} cy={cy} r={r + 7}
                fill="none" stroke="rgba(88,166,255,0.35)" strokeWidth="3" />
            )}
            {/* Pulsing gold ring — optimal escape target */}
            {isOptimal && (
              <circle cx={cx} cy={cy} r={r + 9}
                fill="none" stroke="rgba(240,200,48,0.85)" strokeWidth="2.2">
                <animate attributeName="r" values={`${r + 7};${r + 14};${r + 7}`}
                  dur="1.4s" repeatCount="indefinite" />
                <animate attributeName="opacity" values="0.9;0.35;0.9"
                  dur="1.4s" repeatCount="indefinite" />
              </circle>
            )}
            <circle cx={cx} cy={cy}
              r={r}
              fill={fill}
              stroke={
                inSeq
                  ? 'rgba(255,255,255,0.9)'
                  : isBallCarrier
                  ? 'rgba(240,200,48,0.8)'
                  : 'rgba(255,255,255,0.18)'
              }
              strokeWidth={inSeq ? 2 : isBallCarrier ? 2 : 1}
              className={interactive && !inSeq && !isFirst ? 'pitch-node-hover' : ''}
            />
            <text x={cx} y={cy + 4} textAnchor="middle"
              fill="white"
              fontSize={node.id.length > 2 ? '7' : '8'}
              fontWeight="600"
              style={{ pointerEvents: 'none', userSelect: 'none' }}>
              {DISPLAY_LABELS[node.id] ?? node.id}
            </text>
          </g>
        );
      })}

      {/* Edge color legend */}
      <g>
        <circle cx={PX + 6} cy={H - PY - 8} r={4} fill="rgba(88,166,255,0.7)" />
        <text x={PX + 14} y={H - PY - 4} fill="rgba(88,166,255,0.65)" fontSize="7.5" style={{ userSelect: 'none' }}>Safe</text>
        <circle cx={PX + 46} cy={H - PY - 8} r={4} fill="rgba(227,179,65,0.7)" />
        <text x={PX + 54} y={H - PY - 4} fill="rgba(227,179,65,0.65)" fontSize="7.5" style={{ userSelect: 'none' }}>Risky</text>
        <circle cx={PX + 90} cy={H - PY - 8} r={4} fill="rgba(248,81,73,0.6)" />
        <text x={PX + 98} y={H - PY - 4} fill="rgba(248,81,73,0.55)" fontSize="7.5" style={{ userSelect: 'none' }}>Avoid</text>
      </g>
    </svg>
  );
}
