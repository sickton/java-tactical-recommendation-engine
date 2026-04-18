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

function edgeStroke(edge: NetworkEdge): { color: string; width: number } {
  const p = edge.escape_prob ?? 0;
  if (p >= 0.65) return { color: 'rgba(88,166,255,0.75)', width: 2.0 };   // blue — safe
  if (p >= 0.35) return { color: 'rgba(227,179,65,0.70)', width: 1.6 };   // yellow — risky
  return           { color: 'rgba(248,81,73,0.55)', width: 1.2 };         // red — avoid
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
  graph, formation, interactive, highlightSequence, onNodeClick, ballNodeId,
}: Props) {
  const nodeMap: Record<string, { id: string; x: number; y: number }> = {};
  graph.nodes.forEach(n => {
    const [dx, dy] = getDisplayCoords(n.id, formation);
    nodeMap[n.id] = { id: n.id, x: dx, y: dy };
  });
  const nodeInfluence = buildNodeInfluence(graph);

  // The player currently holding the ball (last in sequence)
  const currentCarrier = highlightSequence[highlightSequence.length - 1] ?? ballNodeId;

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
        <pattern id="pg-grass" x={PX} y={PY} width={W - 2 * PX} height="36" patternUnits="userSpaceOnUse">
          <rect width={W - 2 * PX} height="36" fill="#162316" />
          <rect width={W - 2 * PX} height="18" fill="#182e17" />
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
        const inSeq = isSeqEdge(highlightSequence, edge.from, edge.to);
        const isActive = edge.from === currentCarrier;
        const ghost = !isActive && !inSeq;
        const { color, width } = edgeStroke(edge);
        const d = curvedPath(x1, y1, x2, y2);
        return (
          <path key={i}
            d={d}
            stroke={inSeq ? 'rgba(0,230,180,0.95)' : isActive ? color : 'rgba(255,255,255,0.08)'}
            strokeWidth={inSeq ? 3 : isActive ? width : 0.7}
            strokeLinecap="round"
            fill="none"
            opacity={ghost ? 0.4 : 1}
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

        return (
          <g key={node.id}
            onClick={() => interactive && !isFirst && onNodeClick(node.id)}
            style={{ cursor: interactive && !isFirst ? 'pointer' : 'default' }}>
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
