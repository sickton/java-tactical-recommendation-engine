import { useState } from 'react';
import PitchGraph from '../PitchGraph';
import PressureMap from '../PressureMap';
import { getDisplayCoords } from '../../constants/pitchCoords';
import type {
  BreakThePressConfig,
  PuzzleBrief,
  Moment,
  EvaluateResult,
  NetworkNode,
} from '../../types';

interface Props {
  config: BreakThePressConfig;
  brief: PuzzleBrief;
  moment: Moment;
  teamName: string;
  pressingTeamName: string;
  onRetry: () => void;
}

const DISPLAY_LABELS: Record<string, string> = { CB: 'LCB', CB2: 'RCB', CB3: 'CCB' };
function label(id: string): string { return DISPLAY_LABELS[id] ?? id; }

function scoreColor(p: number): string {
  if (p >= 0.7) return '#3fb950';
  if (p >= 0.4) return '#e3b341';
  return '#f85149';
}

function formatFormation(f: string): string {
  return f.replace('F_', '').replace(/_/g, '-');
}

function computeVacatedZones(
  config: BreakThePressConfig,
  ballY: number,
): Array<{ x: number; y: number }> {
  return config.pressing_graph.nodes
    .map((n: NetworkNode) => {
      const [fx, fy] = getDisplayCoords(n.id, config.pressing_formation);
      const baseX = 1 - fx;
      const baseY = fy;
      if (Math.abs(ballY - baseY) < 0.22) return null;
      return { x: baseX, y: baseY };
    })
    .filter(Boolean) as Array<{ x: number; y: number }>;
}

const MEDAL_STYLE: Record<string, { color: string; bg: string }> = {
  GOLD:   { color: '#f0c830', bg: 'rgba(240,200,48,0.12)' },
  SILVER: { color: '#c0c0c0', bg: 'rgba(192,192,192,0.12)' },
  BRONZE: { color: '#cd7f32', bg: 'rgba(205,127,50,0.12)' },
  MISS:   { color: '#f85149', bg: 'rgba(248,81,73,0.12)' },
  '—':    { color: '#8b949e', bg: 'rgba(139,148,158,0.12)' },
};

export default function BreakThePress({
  config,
  brief,
  moment,
  teamName,
  pressingTeamName,
  onRetry,
}: Props) {
  const [userSequence, setUserSequence]   = useState<string[]>([config.ball_carrier]);
  const [submitted, setSubmitted]         = useState(false);
  const [result, setResult]               = useState<EvaluateResult | null>(null);
  const [evaluating, setEvaluating]       = useState(false);
  const [clickFeedback, setClickFeedback] = useState<'safe' | 'risky' | 'avoid' | null>(null);
  const [hoveredEdge, setHoveredEdge]     = useState<{ from: string; to: string } | null>(null);

  const edges = config.escape_graph.edges;

  // Best next node from current carrier — for the predictive arc
  const currentCarrier = userSequence[userSequence.length - 1];
  const optimalNodeId = submitted ? null : (() => {
    const best = [...edges]
      .filter(e => e.from === currentCarrier && e.escape_prob !== undefined)
      .sort((a, b) => (b.escape_prob ?? 0) - (a.escape_prob ?? 0))[0];
    return best?.to ?? null;
  })();

  // Vacated zones for the pitch graph
  const ballNode = config.escape_graph.nodes.find(n => n.id === config.ball_carrier);
  const vacatedZones = submitted ? [] : computeVacatedZones(config, ballNode?.y ?? 0.5);

  function handleNodeClick(nodeId: string) {
    if (submitted || userSequence.length >= 5) return;
    // Prevent two identical consecutive passes (passing to yourself) but allow revisiting earlier nodes
    if (nodeId === userSequence[userSequence.length - 1]) return;
    const current = userSequence[userSequence.length - 1];
    const edge = edges.find(e => e.from === current && e.to === nodeId);
    const p = edge?.escape_prob ?? 0.25;
    const feedback: 'safe' | 'risky' | 'avoid' =
      p >= 0.65 ? 'safe' : p >= 0.35 ? 'risky' : 'avoid';
    setClickFeedback(feedback);
    setTimeout(() => setClickFeedback(null), 900);
    setUserSequence(prev => [...prev, nodeId]);
  }

  function handleUndo() {
    if (userSequence.length <= 1) return;
    setUserSequence(prev => prev.slice(0, -1));
  }

  function handleReset() {
    setUserSequence([config.ball_carrier]);
    setSubmitted(false);
    setResult(null);
  }

  async function handleSubmit() {
    if (userSequence.length < 2 || evaluating) return;
    setEvaluating(true);

    try {
      const res = await fetch('/api/evaluate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          puzzle_type: 'break_the_press',
          answer: { sequence: userSequence },
          config: {
            escape_graph:       config.escape_graph,
            pressing_graph:     config.pressing_graph,
            ball_carrier:       config.ball_carrier,
            escaping_formation: config.escaping_formation,
            pressing_formation: config.pressing_formation,
            game_phase:         config.game_phase,
          },
          moment_context: {
            headline:         moment.headline,
            minute:           moment.minute,
            match:            moment.match,
            score:            moment.score,
            concept:          moment.concept,
            team:             teamName,
            tactical_problem: moment.tactical_problem ?? '',
            mission:          moment.mission ?? '',
          },
        }),
      });
      const data: EvaluateResult = await res.json();
      setResult(data);
      setSubmitted(true);
    } catch {
      // silent — keep the puzzle interactive
    } finally {
      setEvaluating(false);
    }
  }

  const medalStyle = result ? (MEDAL_STYLE[result.medal] ?? MEDAL_STYLE['—']) : null;

  return (
    <div className="btp-root">
      {/* ── Two-panel pitch area ── */}
      <div className="puzzle-panels">
        {/* Left — press structure */}
        <div className="puzzle-panel">
          <div className="puzzle-panel-header">
            <span className="puzzle-panel-tag puzzle-panel-tag--pressing">PRESSING</span>
            <span className="puzzle-panel-title">{pressingTeamName}'s structure</span>
          </div>
          <p className="puzzle-panel-sub">
            {formatFormation(config.pressing_formation)} — players converging on {teamName}'s {label(config.ball_carrier)}
          </p>
          <PressureMap
            pressingGraph={config.pressing_graph}
            escapeGraph={config.escape_graph}
            ballCarrierId={currentCarrier}
            escapeFormation={config.escaping_formation}
            pressingFormation={config.pressing_formation}
            pressingTeamLabel={pressingTeamName}
          />
        </div>

        {/* Right — escape graph */}
        <div className="puzzle-panel">
          <div className="puzzle-panel-header">
            <span className="puzzle-panel-tag puzzle-panel-tag--escape">ESCAPE</span>
            <span className="puzzle-panel-title">{teamName}'s passing options</span>
          </div>
          <p className="puzzle-panel-sub">
            {submitted
              ? `Your route: ${userSequence.map(label).join(' → ')}`
              : `Ball at ${label(config.ball_carrier)} — click the next position to pass to`}
          </p>
          <PitchGraph
            graph={config.escape_graph}
            formation={config.escaping_formation}
            interactive={!submitted}
            highlightSequence={userSequence}
            onNodeClick={handleNodeClick}
            ballNodeId={config.ball_carrier}
            onEdgeHover={setHoveredEdge}
            optimalNodeId={optimalNodeId}
            vacatedZones={vacatedZones}
          />
          {clickFeedback && (
            <div className={`puzzle-click-feedback puzzle-click-feedback--${clickFeedback}`}>
              {clickFeedback === 'avoid' && 'Intercepted — too risky'}
              {clickFeedback === 'risky' && 'Risky pass — keep going'}
              {clickFeedback === 'safe'  && 'Clean pass'}
            </div>
          )}
        </div>
      </div>

      {/* ── Result section ── */}
      {submitted && result && (
        <div className="btp-result">
          {/* Medal + playstyle header */}
          <div className="btp-result-header">
            <span
              className="puzzle-medal-pill"
              style={{ color: medalStyle!.color, background: medalStyle!.bg, border: `1px solid ${medalStyle!.color}` }}
            >
              {result.medal}
            </span>
            <div className="btp-playstyle">
              <span className="btp-playstyle-label">Your playstyle</span>
              <span className="btp-playstyle-name">{result.playstyle}</span>
            </div>
          </div>

          {/* Playstyle description */}
          <p className="btp-playstyle-desc">{result.playstyle_desc}</p>

          {/* Coaching */}
          <div className="btp-coaching">
            <span className="btp-coaching-label">Coach says</span>
            <p className="btp-coaching-text">{result.coaching}</p>
          </div>

          {/* Sequence comparison */}
          <div className="btp-sequences">
            <div className="btp-seq-card">
              <div className="btp-seq-card-label">Your route</div>
              <div className="puzzle-result-sequence">
                {userSequence.map((n, i) => (
                  <span key={n}>
                    <span className="seq-node seq-node-user">{label(n)}</span>
                    {i < userSequence.length - 1 && <span className="seq-arrow">→</span>}
                  </span>
                ))}
              </div>
              <span className="btp-seq-score" style={{ color: scoreColor(result.user_score) }}>
                {Math.round(result.user_score * 100)}% escape probability
              </span>
            </div>

            <div className="btp-seq-card btp-seq-card--optimal">
              <div className="btp-seq-card-label">Optimal route</div>
              <div className="puzzle-result-sequence">
                {result.optimal_path.map((n, i) => (
                  <span key={n}>
                    <span className="seq-node seq-node-optimal">{label(n)}</span>
                    {i < result.optimal_path.length - 1 && <span className="seq-arrow">→</span>}
                  </span>
                ))}
              </div>
              <span className="btp-seq-score" style={{ color: scoreColor(result.optimal_score) }}>
                {Math.round(result.optimal_score * 100)}% escape probability
              </span>
            </div>
          </div>

          {/* Actions */}
          <div className="btp-actions">
            <button className="btn-back" onClick={handleReset}>↩ Try again</button>
            <button className="btn-back" onClick={onRetry}>← Back to moment</button>
          </div>
        </div>
      )}

      {/* ── Fixed bottom bar ── */}
      {!submitted && (
        <div className="puzzle-controls-wrap">
          {/* Breadcrumb */}
          <div className="puzzle-breadcrumb">
            {Array.from({ length: 5 }).map((_, i) => {
              const node = userSequence[i];
              const isActive = !!node && i === userSequence.length - 1;
              const isBall = i === 0;
              const passEdge = i > 0 && userSequence[i - 1] && node
                ? edges.find(e => e.from === userSequence[i - 1] && e.to === node)
                : null;
              const pct = passEdge?.escape_prob !== undefined
                ? Math.round(passEdge.escape_prob * 100)
                : null;
              const pctColor = pct === null ? '' : pct >= 65 ? '#58a6ff' : pct >= 35 ? '#e3b341' : '#f85149';
              return (
                <span key={i} className="puzzle-breadcrumb-item">
                  {i > 0 && <span className="puzzle-breadcrumb-arrow">–</span>}
                  <span className={`puzzle-breadcrumb-slot${node ? ' filled' : ''}${isActive ? ' active' : ''}`}>
                    {node ? (isBall ? `⚽ ${label(node)}` : label(node)) : '?'}
                    {pct !== null && (
                      <span className="puzzle-breadcrumb-stat" style={{ color: pctColor }}>{pct}%</span>
                    )}
                  </span>
                </span>
              );
            })}
          </div>
          {/* Controls */}
          <div className="puzzle-controls">
            <button className="btn-puzzle-action" onClick={handleUndo} disabled={userSequence.length <= 1}>
              ↩ Undo
            </button>
            <button className="btn-puzzle-action" onClick={handleReset} disabled={userSequence.length <= 1}>
              ✕ Clear
            </button>
            <button
              className={`btn-puzzle-submit btn-puzzle-submit--grow${userSequence.length >= 2 ? ' btn-puzzle-submit--ready' : ''}`}
              onClick={handleSubmit}
              disabled={userSequence.length < 2 || evaluating}
            >
              {evaluating ? 'Evaluating…' : 'Submit sequence →'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
