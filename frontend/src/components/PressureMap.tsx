import type { NetworkGraph, NetworkNode } from '../types';
import { getDisplayCoords } from '../constants/pitchCoords';

const DISPLAY_LABELS: Record<string, string> = {
  CB:  'LCB',
  CB2: 'RCB',
};

interface Props {
  pressingGraph: NetworkGraph;
  escapeGraph: NetworkGraph;
  ballCarrierId: string;
  escapeFormation: string;
  pressingFormation: string;
}

const W = 500;
const H = 320;
const PX = 24;
const PY = 20;

type Role = 'harasser' | 'shadower' | 'anchor';

function toSvg(x: number, y: number): [number, number] {
  return [PX + x * (W - 2 * PX), PY + y * (H - 2 * PY)];
}

function normDist(x1: number, y1: number, x2: number, y2: number): number {
  return Math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2);
}

function jitter(id: string): [number, number] {
  let h = 5381;
  for (let i = 0; i < id.length; i++) h = Math.imul((h << 5) + h, 1) ^ id.charCodeAt(i);
  return [
    (((h >>> 0) & 0xFF) / 255 - 0.5) * 0.055,
    (((h >>> 8) & 0xFF) / 255 - 0.5) * 0.055,
  ];
}

function getPressPos(presser: NetworkNode, escapeNodes: NetworkNode[]): [number, number] {
  if (escapeNodes.length === 0) return [presser.x, presser.y];
  let nearest = escapeNodes[0];
  let minDist = Infinity;
  escapeNodes.forEach(en => {
    const d = normDist(presser.x, presser.y, en.x, en.y);
    if (d < minDist) { minDist = d; nearest = en; }
  });
  const [jx, jy] = jitter(presser.id);
  return [
    Math.max(0.05, Math.min(0.94, nearest.x * 0.25 + presser.x * 0.75 + jx)),
    Math.max(0.05, Math.min(0.94, nearest.y * 0.25 + presser.y * 0.75 + jy)),
  ];
}

function shortenLine(
  x1: number, y1: number, x2: number, y2: number, amount: number
): [number, number] {
  const dx = x2 - x1, dy = y2 - y1;
  const len = Math.sqrt(dx * dx + dy * dy);
  if (len < amount * 2) return [x2, y2];
  return [x2 - (dx / len) * amount, y2 - (dy / len) * amount];
}

// Tapered wedge shadow cast FROM the shadower TOWARD their Liverpool mark.
// This shows the passing lane they are physically blocking — not a geometric
// shadow from the ball, but the "channel deletion" the defender creates.
function coverShadowPath(
  defX: number, defY: number,
  markX: number, markY: number,
  nodeR: number
): string {
  const dx = markX - defX, dy = markY - defY;
  const dist = Math.sqrt(dx * dx + dy * dy);
  if (dist < 4) return '';
  const ux = dx / dist, uy = dy / dist;
  const perpX = -uy, perpY = ux;
  // Shadow starts at defender edge and ends just short of the mark
  const startDist = nodeR + 3;
  const endDist = Math.max(startDist + 20, dist * 0.82);
  const startW = nodeR * 0.5;
  const endW   = nodeR * 3.8;
  const sLx = defX + ux * startDist - perpX * startW;
  const sLy = defY + uy * startDist - perpY * startW;
  const sRx = defX + ux * startDist + perpX * startW;
  const sRy = defY + uy * startDist + perpY * startW;
  const eLx = defX + ux * endDist - perpX * endW;
  const eLy = defY + uy * endDist - perpY * endW;
  const eRx = defX + ux * endDist + perpX * endW;
  const eRy = defY + uy * endDist + perpY * endW;
  return `M ${sLx.toFixed(1)},${sLy.toFixed(1)} L ${eLx.toFixed(1)},${eLy.toFixed(1)} L ${eRx.toFixed(1)},${eRy.toFixed(1)} L ${sRx.toFixed(1)},${sRy.toFixed(1)} Z`;
}

// Small triangular pointer on node edge showing facing direction
function bodyPointer(
  cx: number, cy: number,
  toX: number, toY: number,
  r: number
): string {
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

export default function PressureMap({ pressingGraph, escapeGraph, ballCarrierId }: Props) {
  const ballNode = escapeGraph.nodes.find(n => n.id === ballCarrierId);
  const [ballSvgX, ballSvgY] = ballNode ? toSvg(ballNode.x, ballNode.y) : toSvg(0.38, 0.5);

  // Pre-compute pressing positions (mirrored — pressing team attacks from right)
  const pressPos: Record<string, [number, number]> = {};
  pressingGraph.nodes.forEach(pn => {
    pressPos[pn.id] = getPressPos({ ...pn, x: 1 - pn.x }, escapeGraph.nodes);
  });

  // ── Assign pressing roles ─────────────────────────────────────────────
  const ballNormX = ballNode?.x ?? 0.38;
  const ballNormY = ballNode?.y ?? 0.5;
  const sorted = [...pressingGraph.nodes].sort((a, b) => {
    const [ax, ay] = pressPos[a.id];
    const [bx, by] = pressPos[b.id];
    return normDist(ax, ay, ballNormX, ballNormY) - normDist(bx, by, ballNormX, ballNormY);
  });
  const harasserId = sorted[0]?.id;
  const shadowerIds = new Set(sorted.slice(1, 4).map(n => n.id));
  const roleOf = (id: string): Role =>
    id === harasserId ? 'harasser' : shadowerIds.has(id) ? 'shadower' : 'anchor';

  // ── Trap node: Liverpool player left suspiciously uncovered ───────────
  // The one with the largest minimum distance to any presser
  const trapNodeId = (() => {
    let maxMinDist = 0;
    let candidate: string | null = null;
    escapeGraph.nodes.forEach(en => {
      if (en.id === ballCarrierId) return;
      const minD = Math.min(...pressingGraph.nodes.map(pn => {
        const [px, py] = pressPos[pn.id];
        return normDist(px, py, en.x, en.y);
      }));
      if (minD > maxMinDist) { maxMinDist = minD; candidate = en.id; }
    });
    return maxMinDist > 0.20 ? candidate : null;
  })();

  return (
    <svg width="100%" viewBox={`0 0 ${W} ${H}`} className="pitch-svg">
      <defs>
        <pattern id="pm-grass" x={PX} y={PY} width={W - 2 * PX} height="36" patternUnits="userSpaceOnUse">
          <rect width={W - 2 * PX} height="36" fill="#162316" />
          <rect width={W - 2 * PX} height="18" fill="#182e17" />
        </pattern>
        <marker id="pm-arrow" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto">
          <path d="M 0 0 L 6 3 L 0 6 z" fill="rgba(248,81,73,0.95)" />
        </marker>
        <radialGradient id="pm-ball-glow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="rgba(255,210,60,0.45)" />
          <stop offset="100%" stopColor="rgba(255,210,60,0)" />
        </radialGradient>
        <radialGradient id="pm-press-zone" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="rgba(248,81,73,0.22)" />
          <stop offset="100%" stopColor="rgba(248,81,73,0)" />
        </radialGradient>
        <radialGradient id="pm-halo-harasser" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="rgba(248,81,73,0.7)" />
          <stop offset="55%" stopColor="rgba(248,81,73,0.2)" />
          <stop offset="100%" stopColor="rgba(248,81,73,0)" />
        </radialGradient>
        <radialGradient id="pm-halo-shadower" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="rgba(248,81,73,0.35)" />
          <stop offset="100%" stopColor="rgba(248,81,73,0)" />
        </radialGradient>
      </defs>

      {/* Pitch */}
      <rect x={PX} y={PY} width={W - 2 * PX} height={H - 2 * PY} fill="url(#pm-grass)" rx="4" />
      <rect x={PX} y={PY} width={W - 2 * PX} height={H - 2 * PY}
        fill="none" stroke="rgba(255,255,255,0.14)" strokeWidth="1.5" rx="4" />
      <line x1={W / 2} y1={PY} x2={W / 2} y2={H - PY} stroke="rgba(255,255,255,0.1)" strokeWidth="1.5" />
      <circle cx={W / 2} cy={H / 2} r={40} fill="none" stroke="rgba(255,255,255,0.1)" strokeWidth="1.5" />
      <circle cx={W / 2} cy={H / 2} r={2} fill="rgba(255,255,255,0.25)" />
      <rect x={PX} y={H / 2 - 66} width={82} height={132} fill="none" stroke="rgba(255,255,255,0.09)" strokeWidth="1" />
      <rect x={PX} y={H / 2 - 30} width={28} height={60} fill="none" stroke="rgba(255,255,255,0.07)" strokeWidth="1" />
      <rect x={W - PX - 82} y={H / 2 - 66} width={82} height={132} fill="none" stroke="rgba(255,255,255,0.09)" strokeWidth="1" />
      <rect x={W - PX - 28} y={H / 2 - 30} width={28} height={60} fill="none" stroke="rgba(255,255,255,0.07)" strokeWidth="1" />

      {/* High pressure zone around ball */}
      <circle cx={ballSvgX} cy={ballSvgY} r={68} fill="url(#pm-press-zone)" />

      {/* ── LAYER 0: Cover shadows — FROM shadower TOWARD their Liverpool mark ── */}
      {pressingGraph.nodes.map(node => {
        if (roleOf(node.id) !== 'shadower') return null;
        const [px, py] = pressPos[node.id];
        const [cx, cy] = toSvg(px, py);
        // Find the Liverpool player this shadower is marking
        let markNode: NetworkNode | null = null;
        let minD = Infinity;
        escapeGraph.nodes.forEach(en => {
          if (en.id === ballCarrierId) return;
          const d = normDist(px, py, en.x, en.y);
          if (d < minD) { minD = d; markNode = en; }
        });
        if (!markNode) return null;
        const [markSvgX, markSvgY] = toSvg((markNode as NetworkNode).x, (markNode as NetworkNode).y);
        const shadowD = coverShadowPath(cx, cy, markSvgX, markSvgY, 11);
        if (!shadowD) return null;
        return (
          <path key={`shadow-${node.id}`}
            d={shadowD}
            fill="rgba(248,81,73,0.13)"
            stroke="rgba(248,81,73,0.22)"
            strokeWidth="0.5"
          />
        );
      })}

      {/* ── LAYER 1: Liverpool ghost nodes ── */}
      {escapeGraph.nodes.map(node => {
        const [cx, cy] = toSvg(node.x, node.y);
        const isBall = node.id === ballCarrierId;
        const isTrap = node.id === trapNodeId;
        return (
          <g key={`esc-${node.id}`}>
            {isBall && <circle cx={cx} cy={cy} r={28} fill="url(#pm-ball-glow)" />}
            {/* Trap indicator — dashed orange ring: "left open on purpose" */}
            {isTrap && (
              <>
                <circle cx={cx} cy={cy} r={20}
                  fill="none"
                  stroke="rgba(227,179,65,0.7)"
                  strokeWidth="1.5"
                  strokeDasharray="5 3"
                />
                <text x={cx} y={cy - 19} textAnchor="middle"
                  fill="rgba(227,179,65,0.95)" fontSize="9" fontWeight="900"
                  style={{ pointerEvents: 'none', userSelect: 'none' }}>!</text>
              </>
            )}
            <circle cx={cx} cy={cy}
              r={isBall ? 14 : 11}
              fill={isBall ? 'rgba(88,166,255,0.88)' : 'rgba(88,166,255,0.22)'}
              stroke={isBall ? 'rgba(240,200,48,0.95)' : isTrap ? 'rgba(227,179,65,0.5)' : 'rgba(88,166,255,0.3)'}
              strokeWidth={isBall ? 2.5 : 1}
            />
            <text x={cx} y={cy + 4} textAnchor="middle"
              fill={isBall ? 'white' : isTrap ? 'rgba(227,179,65,0.8)' : 'rgba(88,166,255,0.55)'}
              fontSize={(DISPLAY_LABELS[node.id] ?? node.id).length > 2 ? '6' : '7'}
              fontWeight={isBall ? '700' : '500'}
              style={{ pointerEvents: 'none', userSelect: 'none' }}>
              {DISPLAY_LABELS[node.id] ?? node.id}
            </text>
          </g>
        );
      })}

      {/* ── LAYER 2: Pressing arrows — Harasser only gets the aggressive arrow ── */}
      {pressingGraph.nodes.map(node => {
        const role = roleOf(node.id);
        if (role !== 'harasser') return null;  // only harasser charges the ball
        const [px, py] = pressPos[node.id];
        const [sx, sy] = toSvg(px, py);
        const dist = normDist(px, py, ballNormX, ballNormY);
        if (dist < 0.06) return null;
        const [ex, ey] = shortenLine(sx, sy, ballSvgX, ballSvgY, 20);
        return (
          <line key={`arr-${node.id}`}
            x1={sx} y1={sy} x2={ex} y2={ey}
            stroke="rgba(248,81,73,0.90)"
            strokeWidth={3.2}
            strokeLinecap="round"
            markerEnd="url(#pm-arrow)"
          />
        );
      })}

      {/* Ball dot */}
      {ballNode && (
        <circle cx={ballSvgX - 16} cy={ballSvgY - 16} r={6}
          fill="#f0c830" stroke="white" strokeWidth="1.5" />
      )}

      {/* ── LAYER 3: Pressing nodes — styled by role ── */}
      {pressingGraph.nodes.map(node => {
        const [px, py] = pressPos[node.id];
        const [cx, cy] = toSvg(px, py);
        const dist = normDist(px, py, ballNormX, ballNormY);
        const proximity = Math.max(0, 1 - dist / 0.55);
        const role = roleOf(node.id);

        // Role-specific visual properties
        const nodeR =
          role === 'harasser' ? 12 + proximity * 3
          : role === 'shadower' ? 10
          : 9;
        const nodeFill =
          role === 'harasser' ? `rgba(248,81,73,${(0.82 + proximity * 0.15).toFixed(2)})`
          : role === 'shadower' ? 'rgba(248,81,73,0.68)'
          : 'rgba(200,60,50,0.45)';
        const haloGradient =
          role === 'harasser' ? 'url(#pm-halo-harasser)'
          : role === 'shadower' ? 'url(#pm-halo-shadower)'
          : null;
        const haloR =
          role === 'harasser' ? 28 + proximity * 20
          : role === 'shadower' ? 22
          : 0;
        const haloOpacity =
          role === 'harasser' ? 0.55 + proximity * 0.4
          : role === 'shadower' ? 0.35
          : 0;

        // Body orientation: where the player is facing
        let facingSvgX = ballSvgX;
        let facingSvgY = ballSvgY;
        if (role === 'shadower') {
          // Face their nearest Liverpool mark (not the ball carrier)
          let nearest: NetworkNode | null = null;
          let minD = Infinity;
          escapeGraph.nodes.forEach(en => {
            if (en.id === ballCarrierId) return;
            const d = normDist(px, py, en.x, en.y);
            if (d < minD) { minD = d; nearest = en; }
          });
          if (nearest) {
            const [nx, ny] = toSvg((nearest as NetworkNode).x, (nearest as NetworkNode).y);
            facingSvgX = nx; facingSvgY = ny;
          }
        } else if (role === 'anchor') {
          // Face forward — maintaining defensive shape (toward their own goal = x=0 direction)
          facingSvgX = cx - 50;
          facingSvgY = cy;
        }

        const pointerD = bodyPointer(cx, cy, facingSvgX, facingSvgY, nodeR);

        return (
          <g key={`press-${node.id}`}>
            {haloGradient && haloR > 0 && (
              <>
                <circle cx={cx} cy={cy} r={haloR}
                  fill={haloGradient}
                  opacity={haloOpacity}
                />
                {/* Harasser pulse ring — shows active forward movement */}
                {role === 'harasser' && (
                  <circle cx={cx} cy={cy} r={nodeR + 4} fill="none"
                    stroke="rgba(248,81,73,0.6)" strokeWidth="1.5">
                    <animate attributeName="r" from={nodeR + 4} to={haloR + 8} dur="1.4s" repeatCount="indefinite" />
                    <animate attributeName="opacity" from="0.65" to="0" dur="1.4s" repeatCount="indefinite" />
                  </circle>
                )}
              </>
            )}
            <circle cx={cx} cy={cy} r={nodeR}
              fill={nodeFill}
              stroke={role === 'anchor' ? 'rgba(255,255,255,0.1)' : 'rgba(255,255,255,0.25)'}
              strokeWidth={role === 'anchor' ? 0.5 : 1}
            />
            {/* Body orientation pointer */}
            {pointerD && (
              <path d={pointerD}
                fill={role === 'anchor' ? 'rgba(255,255,255,0.35)' : 'rgba(255,255,255,0.75)'}
              />
            )}
            <text x={cx} y={cy + 4} textAnchor="middle"
              fill={role === 'anchor' ? 'rgba(255,255,255,0.55)' : 'white'}
              fontSize={(DISPLAY_LABELS[node.id] ?? node.id).length > 2 ? '6' : '7'}
              fontWeight={role === 'harasser' ? '700' : '500'}
              style={{ pointerEvents: 'none', userSelect: 'none' }}>
              {DISPLAY_LABELS[node.id] ?? node.id}
            </text>
          </g>
        );
      })}

      {/* Legend */}
      <g>
        <circle cx={PX + 8} cy={H - PY - 8} r={5} fill="rgba(88,166,255,0.5)" stroke="rgba(88,166,255,0.4)" strokeWidth="1" />
        <text x={PX + 17} y={H - PY - 4} fill="rgba(88,166,255,0.55)" fontSize="7.5" style={{ userSelect: 'none' }}>Liverpool</text>
        <circle cx={PX + 74} cy={H - PY - 8} r={5} fill="rgba(248,81,73,0.85)" />
        <text x={PX + 83} y={H - PY - 4} fill="rgba(248,81,73,0.7)" fontSize="7.5" style={{ userSelect: 'none' }}>Harasser</text>
        <circle cx={PX + 140} cy={H - PY - 8} r={5} fill="rgba(248,81,73,0.6)" />
        <text x={PX + 149} y={H - PY - 4} fill="rgba(248,81,73,0.55)" fontSize="7.5" style={{ userSelect: 'none' }}>Shadower</text>
        <circle cx={PX + 212} cy={H - PY - 8} r={5} fill="rgba(200,60,50,0.45)" />
        <text x={PX + 221} y={H - PY - 4} fill="rgba(200,60,50,0.5)" fontSize="7.5" style={{ userSelect: 'none' }}>Anchor</text>
        <circle cx={PX + 268} cy={H - PY - 8} r={5} fill="none" stroke="rgba(227,179,65,0.7)" strokeWidth="1.5" strokeDasharray="3 2" />
        <text x={PX + 277} y={H - PY - 4} fill="rgba(227,179,65,0.65)" fontSize="7.5" style={{ userSelect: 'none' }}>Trap</text>
      </g>
    </svg>
  );
}
