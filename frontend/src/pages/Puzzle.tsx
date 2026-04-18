import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import PitchGraph from '../components/PitchGraph';
import PressureMap from '../components/PressureMap';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import type { Moment, NetworkResponse, NetworkGraph, NetworkEdge } from '../types';

interface LocationState {
  moment: Moment;
  teamName: string;
  league: string;
  teamId: string;
  mode: string;
  queryType: string;
  imageIndex: number;
}

// Canonical positions for each formation — ~10 roles representing 11 players
// (CB covers both center backs as one positional role)
const FORMATION_POSITIONS: Record<string, string[]> = {
  F_4_3_3:   ['GK', 'CB', 'CB2', 'LB', 'RB', 'CDM', 'CM', 'CAM', 'LW', 'RW', 'ST'],
  F_4_2_3_1: ['GK', 'CB', 'CB2', 'LB', 'RB', 'CDM', 'CM', 'CAM', 'LW', 'RW', 'ST'],
  F_4_4_2:   ['GK', 'CB', 'CB2', 'LB', 'RB', 'LM',  'CDM', 'CM', 'RM', 'CF', 'ST'],
  F_3_5_2:   ['GK', 'CB', 'LWB', 'RWB', 'CDM', 'CM', 'LM', 'RM', 'CF', 'ST'],
  F_3_4_3:   ['GK', 'CB', 'LWB', 'RWB', 'CDM', 'CM', 'LW', 'RW', 'CF', 'ST'],
  F_5_3_2:   ['GK', 'CB', 'LWB', 'RWB', 'LB', 'RB', 'CDM', 'CM', 'CF', 'ST'],
};

function filterGraph(graph: NetworkGraph, formation: string): NetworkGraph {
  const allowed = FORMATION_POSITIONS[formation];
  if (!allowed) return graph;
  const validSet = new Set(allowed);
  const nodes = graph.nodes.filter(n => validSet.has(n.id));
  // Inject a synthetic CB2 node alongside CB for back-four formations
  if (validSet.has('CB2') && !nodes.find(n => n.id === 'CB2')) {
    const cb = nodes.find(n => n.id === 'CB');
    if (cb) nodes.push({ id: 'CB2', x: cb.x, y: cb.y });
  }
  const nodeIds = new Set(nodes.map(n => n.id));
  const edges = graph.edges.filter(e => nodeIds.has(e.from) && nodeIds.has(e.to));
  return { nodes, edges };
}

// Positions where a player realistically receives the ball under press
const PRESSABLE_POSITIONS = ['CB', 'CDM', 'CM', 'LB', 'RB', 'CAM'];

function randomBallCarrier(nodes: string[]): string {
  const preferred = PRESSABLE_POSITIONS.filter(p => nodes.includes(p));
  const pool = preferred.length > 0 ? preferred : nodes;
  return pool[Math.floor(Math.random() * pool.length)];
}

function parseScore(score: string): [number, number] {
  const nums = score.match(/\d+/g);
  if (nums && nums.length >= 2) return [parseInt(nums[0]), parseInt(nums[1])];
  return [0, 0];
}

function getOpposition(matchStr: string, teamName: string): string {
  const parts = matchStr.split(/\s+vs\.?\s+/i);
  if (parts.length !== 2) return '';
  const home = parts[0].trim();
  const away = parts[1].trim();
  return home.toLowerCase() === teamName.toLowerCase() ? away : home;
}

function computeOptimalPath(edges: NetworkEdge[], startNode: string, maxHops = 4): string[] {
  const path = [startNode];
  let current = startNode;
  for (let i = 0; i < maxHops; i++) {
    const best = edges
      .filter(e => e.from === current && !path.includes(e.to))
      .sort((a, b) => (b.escape_prob ?? 0) - (a.escape_prob ?? 0))[0];
    if (!best) break;
    path.push(best.to);
    current = best.to;
  }
  return path;
}

function sequenceScore(edges: NetworkEdge[], seq: string[]): number {
  if (seq.length < 2) return 0;
  let total = 0, count = 0;
  for (let i = 0; i < seq.length - 1; i++) {
    const edge = edges.find(e => e.from === seq[i] && e.to === seq[i + 1]);
    if (edge?.escape_prob !== undefined) { total += edge.escape_prob; count++; }
  }
  return count > 0 ? total / count : 0;
}

function scoreColor(p: number): string {
  if (p >= 0.7) return '#3fb950';
  if (p >= 0.4) return '#e3b341';
  return '#f85149';
}

function formatFormation(f: string): string {
  return f.replace('F_', '').replace(/_/g, '-');
}

const DISPLAY_LABELS: Record<string, string> = { CB: 'LCB', CB2: 'RCB' };
function label(id: string): string { return DISPLAY_LABELS[id] ?? id; }

function sequenceVerdict(userScore: number, optimalScore: number): string {
  if (optimalScore === 0) return '';
  const ratio = userScore / optimalScore;
  if (ratio >= 0.9) return 'Excellent — your route closely matches the optimal escape path.';
  if (ratio >= 0.7) return 'Good read — solid route, though a higher-probability path existed.';
  if (ratio >= 0.5) return 'Decent instinct — in the right area but left some gaps under pressure.';
  return 'The pressing team would likely win the ball here. Try starting deeper or switching flanks.';
}

function getMedal(userScore: number, optimalScore: number): { label: string; color: string; bg: string } {
  if (optimalScore === 0) return { label: '—', color: '#8b949e', bg: 'rgba(139,148,158,0.12)' };
  const ratio = userScore / optimalScore;
  if (ratio >= 0.9) return { label: 'GOLD',   color: '#f0c830', bg: 'rgba(240,200,48,0.12)' };
  if (ratio >= 0.7) return { label: 'SILVER', color: '#c0c0c0', bg: 'rgba(192,192,192,0.12)' };
  if (ratio >= 0.5) return { label: 'BRONZE', color: '#cd7f32', bg: 'rgba(205,127,50,0.12)' };
  return { label: 'MISS', color: '#f85149', bg: 'rgba(248,81,73,0.12)' };
}

export default function Puzzle() {
  const location = useLocation();
  const navigate = useNavigate();
  const state = location.state as LocationState | null;
  const { moment, teamName, league, teamId, mode, queryType, imageIndex } = state ?? {};

  const [networkData, setNetworkData] = useState<NetworkResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [userSequence, setUserSequence] = useState<string[]>([]);
  const [submitted, setSubmitted] = useState(false);
  const [optimalPath, setOptimalPath] = useState<string[]>([]);
  const [escapeGraph, setEscapeGraph] = useState<NetworkGraph | null>(null);
  const [pressGraph, setPressGraph] = useState<NetworkGraph | null>(null);
  const [ballCarrier, setBallCarrier] = useState<string>('');
  const [clickFeedback, setClickFeedback] = useState<'safe' | 'risky' | 'avoid' | null>(null);

  useLeagueTheme(league ?? null);
  usePageTitle('Tactical Puzzle');

  const pressingTeam = moment ? getOpposition(moment.match, teamName ?? '') : '';
  const [homeGoals, awayGoals] = moment ? parseScore(moment.score) : [0, 0];

  useEffect(() => {
    if (!moment || !pressingTeam || !teamName) return;
    fetch(
      `/api/network?escapingTeam=${encodeURIComponent(teamName)}&pressingTeam=${encodeURIComponent(pressingTeam)}&league=${league}&minute=${moment.minute}&homeGoals=${homeGoals}&awayGoals=${awayGoals}`
    )
      .then(r => r.json())
      .then((data: NetworkResponse) => {
        const filteredEscape = filterGraph(data.escape_graph, data.escaping_formation);
        const filteredPress = filterGraph(data.pressing_graph, data.pressing_formation);
        const carrier = randomBallCarrier(filteredEscape.nodes.map(n => n.id));

        setNetworkData(data);
        setEscapeGraph(filteredEscape);
        setPressGraph(filteredPress);
        setBallCarrier(carrier);
        setUserSequence([carrier]); // ball is pre-placed at the most-pressed position
        setLoading(false);
      })
      .catch(() => { setError('Failed to load tactical graph.'); setLoading(false); });
  }, []);

  function handleNodeClick(nodeId: string) {
    if (submitted || userSequence.includes(nodeId) || userSequence.length >= 5) return;
    const current = userSequence[userSequence.length - 1];
    const edge = escapeGraph?.edges.find(e => e.from === current && e.to === nodeId);
    const p = edge?.escape_prob ?? 0.5;
    const feedback: 'safe' | 'risky' | 'avoid' = p >= 0.65 ? 'safe' : p >= 0.35 ? 'risky' : 'avoid';
    if (feedback === 'avoid') {
      setClickFeedback('avoid');
      setTimeout(() => setClickFeedback(null), 900);
      return; // blocked — ball intercepted
    }
    setClickFeedback(feedback);
    setTimeout(() => setClickFeedback(null), 700);
    setUserSequence(prev => [...prev, nodeId]);
  }

  function handleUndo() {
    // Never undo past the ball carrier (first node is locked)
    if (userSequence.length <= 1) return;
    setUserSequence(prev => prev.slice(0, -1));
  }

  function handleSubmit() {
    if (userSequence.length < 2 || !escapeGraph) return;
    setOptimalPath(computeOptimalPath(escapeGraph.edges, ballCarrier, 4));
    setSubmitted(true);
  }

  function handleReset() {
    setSubmitted(false);
    setUserSequence([ballCarrier]);
    setOptimalPath([]);
  }

  if (!moment || !teamName) {
    return (
      <>
        <Header />
        <main className="container">
          <p style={{ color: '#8b949e', marginTop: 48 }}>
            No moment data.{' '}
            <button className="btn-back" onClick={() => navigate('/')}>Go home</button>
          </p>
        </main>
        <Footer />
      </>
    );
  }

  const edges = escapeGraph?.edges ?? [];
  const avgFormationProb = edges.length
    ? edges.reduce((s, e) => s + (e.escape_prob ?? 0), 0) / edges.length
    : 0;
  const userScore = sequenceScore(edges, userSequence);
  const optimalScore = sequenceScore(edges, optimalPath);
  const gamePhaseLabel = networkData
    ? networkData.game_phase.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())
    : '';


  return (
    <>
      <Header />
      <main className="container puzzle-page">

        {/* Context bar */}
        <div className="puzzle-context-bar">
          <div className="puzzle-context-item">
            <span className="puzzle-context-label">Match</span>
            <span className="puzzle-context-value">{moment.match}</span>
          </div>
          <div className="puzzle-context-item">
            <span className="puzzle-context-label">Minute</span>
            <span className="puzzle-context-value">{moment.minute}'</span>
          </div>
          <div className="puzzle-context-item">
            <span className="puzzle-context-label">Score</span>
            <span className="puzzle-context-value">{moment.score}</span>
          </div>
          {gamePhaseLabel && (
            <div className="puzzle-context-item">
              <span className="puzzle-context-label">Phase</span>
              <span className="puzzle-context-value">{gamePhaseLabel}</span>
            </div>
          )}
          <div className="puzzle-context-item">
            <span className="puzzle-context-label">Concept</span>
            <span className="puzzle-context-value puzzle-concept-pill">{moment.concept}</span>
          </div>
        </div>

        <h1 className="puzzle-headline">{moment.headline}</h1>
        <p className="puzzle-subline">
          <strong>{pressingTeam}</strong> are pressing hard.
          Build <strong>{teamName}</strong>'s escape route through their structure.
        </p>

        {/* How-to instruction strip */}
        {!submitted && ballCarrier && (
          <div className="puzzle-howto">
            <div className="puzzle-howto-step">
              <span className="puzzle-howto-num">1</span>
              <span>Left panel shows <strong>{pressingTeam}</strong>'s players converging on the ball — read their pressure</span>
            </div>
            <div className="puzzle-howto-divider">→</div>
            <div className="puzzle-howto-step">
              <span className="puzzle-howto-num">2</span>
              <span>Ball is at <strong className="puzzle-carrier-highlight">{label(ballCarrier)}</strong> — the most pressured position. Click the next position to pass to</span>
            </div>
            <div className="puzzle-howto-divider">→</div>
            <div className="puzzle-howto-step">
              <span className="puzzle-howto-num">3</span>
              <span>Chain up to 4 passes, then hit <strong>Submit</strong></span>
            </div>
          </div>
        )}

        {loading && (
          <div className="moments-loading" style={{ marginTop: 48 }}>
            <div className="moments-spinner" />
            <p>Building tactical graph...</p>
          </div>
        )}

        {error && <p style={{ color: '#f85149', marginTop: 24 }}>{error}</p>}

        {networkData && escapeGraph && pressGraph && !loading && (
          <>
            <div className="puzzle-panels">
              {/* Left — pressing structure */}
              <div className="puzzle-panel">
                <div className="puzzle-panel-header">
                  <span className="puzzle-panel-badge badge-pressing">PRESSING</span>
                  <h3 className="puzzle-panel-title">{pressingTeam}'s structure</h3>
                </div>
                <p className="puzzle-panel-sub">
                  {formatFormation(networkData.pressing_formation)} — players converging on {teamName}'s {label(ballCarrier)}
                </p>
                <PressureMap
                  pressingGraph={pressGraph}
                  escapeGraph={escapeGraph}
                  ballCarrierId={ballCarrier}
                />
              </div>

              {/* Right — escape route builder */}
              <div className="puzzle-panel">
                <div className="puzzle-panel-header">
                  <span className="puzzle-panel-badge badge-escape">ESCAPE</span>
                  <h3 className="puzzle-panel-title">{teamName}'s passing options</h3>
                </div>
                <p className="puzzle-panel-sub">
                  {submitted
                    ? `Your route: ${userSequence.join(' → ')}`
                    : userSequence.length <= 1
                    ? `Ball at ${label(ballCarrier)} — click the next position to pass to`
                    : `${userSequence.map(label).join(' → ')} — keep going or submit`}
                </p>
                <PitchGraph
                  graph={escapeGraph}
                  interactive={!submitted}
                  highlightSequence={userSequence}
                  onNodeClick={handleNodeClick}
                  mode="escape"
                  ballNodeId={ballCarrier}
                  jitter={false}
                />
                {!submitted && (
                  <>
                    {/* Breadcrumb trail — shows sequence slots up to 5 */}
                    <div className="puzzle-breadcrumb">
                      {Array.from({ length: 5 }).map((_, i) => {
                        const node = userSequence[i];
                        const isActive = node && i === userSequence.length - 1;
                        const isBall = i === 0;
                        return (
                          <span key={i} className="puzzle-breadcrumb-item">
                            {i > 0 && <span className="puzzle-breadcrumb-arrow">→</span>}
                            <span className={`puzzle-breadcrumb-slot${node ? ' filled' : ''}${isActive ? ' active' : ''}`}>
                              {node ? (isBall ? `⚽ ${label(node)}` : label(node)) : '?'}
                            </span>
                          </span>
                        );
                      })}
                    </div>

                    {/* Click feedback flash */}
                    {clickFeedback && (
                      <div className={`puzzle-click-feedback puzzle-click-feedback--${clickFeedback}`}>
                        {clickFeedback === 'avoid' && 'Intercepted — too risky'}
                        {clickFeedback === 'risky' && 'Risky pass — keep going'}
                        {clickFeedback === 'safe'  && 'Clean pass'}
                      </div>
                    )}

                    {userSequence.length >= 2 && (
                      <div className="puzzle-prob-bar-wrap">
                        <span className="puzzle-prob-bar-label">Escape probability</span>
                        <div className="puzzle-prob-bar-track">
                          <div
                            className="puzzle-prob-bar-fill"
                            style={{
                              width: `${Math.round(userScore * 100)}%`,
                              background: scoreColor(userScore),
                            }}
                          />
                        </div>
                        <span className="puzzle-prob-bar-pct" style={{ color: scoreColor(userScore) }}>
                          {Math.round(userScore * 100)}%
                        </span>
                      </div>
                    )}
                    <div className="puzzle-controls">
                      <button className="btn-back" onClick={handleUndo} disabled={userSequence.length <= 1}>
                        ↩ Undo
                      </button>
                      <button
                        className="btn-puzzle-submit"
                        onClick={handleSubmit}
                        disabled={userSequence.length < 2}
                      >
                        Submit sequence →
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>

            {/* Result section */}
            {submitted && (() => {
              const medal = getMedal(userScore, optimalScore);
              return (
              <div className="puzzle-result">
                <div className="puzzle-result-header">
                  <h2 className="puzzle-result-title">Result</h2>
                  <span className="puzzle-medal-pill" style={{ color: medal.color, background: medal.bg, border: `1px solid ${medal.color}` }}>
                    {medal.label}
                  </span>
                </div>
                <div className="puzzle-result-panels">
                  <div className="puzzle-result-card">
                    <div className="puzzle-result-card-label">Your sequence</div>
                    <div className="puzzle-result-sequence">
                      {userSequence.map((n, i) => (
                        <span key={n}>
                          <span className="seq-node seq-node-user">{label(n)}</span>
                          {i < userSequence.length - 1 && <span className="seq-arrow">→</span>}
                        </span>
                      ))}
                    </div>
                    <div className="puzzle-result-stat">
                      <span className="puzzle-result-stat-label">Avg escape probability</span>
                      <span className="puzzle-result-stat-value" style={{ color: scoreColor(userScore) }}>
                        {(userScore * 100).toFixed(0)}%
                      </span>
                    </div>
                    <p className="puzzle-result-desc">{sequenceVerdict(userScore, optimalScore)}</p>
                  </div>

                  <div className="puzzle-result-card puzzle-result-card-highlight">
                    <div className="puzzle-result-card-label">Optimal route</div>
                    <div className="puzzle-result-sequence">
                      {optimalPath.map((n, i) => (
                        <span key={n}>
                          <span className="seq-node seq-node-optimal">{label(n)}</span>
                          {i < optimalPath.length - 1 && <span className="seq-arrow">→</span>}
                        </span>
                      ))}
                    </div>
                    <div className="puzzle-result-stat">
                      <span className="puzzle-result-stat-label">Avg escape probability</span>
                      <span className="puzzle-result-stat-value" style={{ color: scoreColor(optimalScore) }}>
                        {(optimalScore * 100).toFixed(0)}%
                      </span>
                    </div>
                    <p className="puzzle-result-desc">
                      Highest-probability escape path from {label(ballCarrier)}, based on historical press-escape data.
                    </p>
                  </div>

                  <div className="puzzle-result-card">
                    <div className="puzzle-result-card-label">Formation baseline</div>
                    <div className="puzzle-result-formation">{formatFormation(networkData.escaping_formation)}</div>
                    <div className="puzzle-result-stat">
                      <span className="puzzle-result-stat-label">Formation avg escape rate</span>
                      <span className="puzzle-result-stat-value" style={{ color: scoreColor(avgFormationProb) }}>
                        {(avgFormationProb * 100).toFixed(0)}%
                      </span>
                    </div>
                    <p className="puzzle-result-desc">
                      In a {formatFormation(networkData.escaping_formation)} during {gamePhaseLabel.toLowerCase()},
                      teams escape press {(avgFormationProb * 100).toFixed(0)}% of the time on average.
                    </p>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 16, marginTop: 32, flexWrap: 'wrap' }}>
                  <button className="btn-back" onClick={handleReset}>↩ Try again</button>
                  <button
                    className="btn-back"
                    onClick={() => navigate('/moment', {
                      state: { moment, teamName, league, teamId, mode, queryType, imageIndex },
                    })}
                  >
                    ← Back to moment
                  </button>
                </div>
              </div>
            );
            })()}
          </>
        )}
      </main>
      <Footer />
    </>
  );
}
