import './Analytics.css';
import { useState, useEffect, useRef } from 'react';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { usePageTitle } from '../hooks/usePageTitle';
import { formatTactic } from '../utils/formatTactic';
import type { TacticAnalytics, LeagueNormalizerResult, PhaseNormalizerResult } from '../types';

const TACTICS = [
  'GEGENPRESSING', 'HIGH_PRESS', 'TIKI_TAKA', 'CONTROL',
  'COUNTER_ATTACK', 'DIRECT_PLAY', 'LOW_BLOCK',
];

const PHASE_LABELS: Record<string, string> = {
  EARLY_MINUTES: 'Early Minutes',
  CLOSING_HALF:  'Closing Half',
  HALF_TIME:     'Half Time',
  BUILD_PHASE:   'Build Phase',
  TENSION_TIME:  'Tension Time',
  LATE_GAME:     'Late Game',
  STOPPAGE_TIME: 'Stoppage Time',
};

// winRate from backend is already 0–100 (e.g. 65.0 = 65%)
function winRateColor(rate: number): string {
  if (rate >= 55) return '#4ade80';
  if (rate >= 40) return '#e8b84b';
  return '#e8668c';
}

// ── Historical tactic cards (segmented W/D/L bar) ─────────────────
function HistoricalBars({ data }: { data: TacticAnalytics[] }) {
  const winRefs  = useRef<(HTMLDivElement | null)[]>([]);
  const drawRefs = useRef<(HTMLDivElement | null)[]>([]);
  const lossRefs = useRef<(HTMLDivElement | null)[]>([]);
  const sorted = [...data].sort((a, b) => b.winRate - a.winRate);

  useEffect(() => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        sorted.forEach((row, i) => {
          const t    = row.total || 1;
          const wPct = (row.wins   / t) * 100;
          const dPct = (row.draws  / t) * 100;
          const lPct = (row.losses / t) * 100;
          const dur  = `${0.5 + i * 0.06}s cubic-bezier(0.4,0,0.2,1)`;
          const wEl = winRefs.current[i];
          const dEl = drawRefs.current[i];
          const lEl = lossRefs.current[i];
          if (wEl) { wEl.style.transition = `width ${dur}`; wEl.style.width = wPct + '%'; }
          if (dEl) { dEl.style.transition = `width ${dur}`; dEl.style.width = dPct + '%'; }
          if (lEl) { lEl.style.transition = `width ${dur}`; lEl.style.width = lPct + '%'; }
        });
      });
    });
  }, [sorted]);

  return (
    <div className="an-hist-cards">
      {sorted.map((row, i) => {
        const t    = row.total || 1;
        const dPct = (row.draws  / t) * 100;
        const lPct = (row.losses / t) * 100;
        return (
          <div key={row.startTactic} className="an-hist-card">
            <div className="an-hist-name">{formatTactic(row.startTactic)}</div>
            <div className="an-hist-seg-track">
              <div ref={el => { winRefs.current[i]  = el; }} className="an-hist-seg-win"  style={{ width: '0%' }} />
              <div ref={el => { drawRefs.current[i] = el; }} className="an-hist-seg-draw" style={{ width: '0%' }} />
              <div ref={el => { lossRefs.current[i] = el; }} className="an-hist-seg-loss" style={{ width: '0%' }} />
            </div>
            <div className="an-hist-stats">
              <span className="an-hist-stat an-hist-stat-w">{row.winRate.toFixed(1)}% W</span>
              <span className="an-hist-sep">·</span>
              <span className="an-hist-stat an-hist-stat-d">{dPct.toFixed(1)}% D</span>
              <span className="an-hist-sep">·</span>
              <span className="an-hist-stat an-hist-stat-l">{lPct.toFixed(1)}% L</span>
            </div>
            <div className="an-hist-total">{row.total} simulation{row.total !== 1 ? 's' : ''}</div>
          </div>
        );
      })}
    </div>
  );
}

// ── League normalizer card ────────────────────────────────────────
function LeagueNormCard({ data }: { data: LeagueNormalizerResult }) {
  const fillRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = fillRef.current;
    if (!el) return;
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        el.style.transition = 'width 1.1s cubic-bezier(0.4,0,0.2,1)';
        el.style.width = data.winRate + '%';
      });
    });
  }, [data]);

  const color = winRateColor(data.winRate);

  return (
    <div className="an-norm-body">
      <div className="an-norm-tactic">{formatTactic(data.tactic)}</div>
      <div className="an-win-rate" style={{ color }}>{data.winRate.toFixed(1)}%</div>
      <div className="an-win-label">Win Rate</div>
      <div className="an-wr-track">
        <div ref={fillRef} className="an-wr-fill" style={{ width: '0%', background: color }} />
      </div>
      <div className="an-wdl-row">
        <div className="an-wdl-cell">
          <span className="an-wdl-num an-wdl-w">{data.wins}</span>
          <span className="an-wdl-key">W</span>
        </div>
        <div className="an-wdl-cell">
          <span className="an-wdl-num an-wdl-d">{data.draws}</span>
          <span className="an-wdl-key">D</span>
        </div>
        <div className="an-wdl-cell">
          <span className="an-wdl-num an-wdl-l">{data.losses}</span>
          <span className="an-wdl-key">L</span>
        </div>
      </div>
      <div className="an-wdl-total">
        {data.totalSimulations} simulations · all {data.league === 'PL' ? 'PL' : 'SA'} teams
      </div>
    </div>
  );
}

// ── Phase normalizer chart ────────────────────────────────────────
function PhaseNormChart({ data }: { data: PhaseNormalizerResult }) {
  const fillRefs = useRef<(HTMLDivElement | null)[]>([]);

  useEffect(() => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        fillRefs.current.forEach((el, i) => {
          if (!el || !data.phases[i]) return;
          el.style.transition = `width ${0.6 + i * 0.1}s cubic-bezier(0.4,0,0.2,1)`;
          el.style.width = data.phases[i].winRate + '%';
        });
      });
    });
  }, [data]);

  return (
    <div className="an-bars">
      {data.phases.map((phase, i) => {
        const color = winRateColor(phase.winRate);
        return (
          <div key={phase.phase} className="an-bar-row">
            <div className="an-bar-label an-bar-label--phase">
              {PHASE_LABELS[phase.phase] ?? phase.phase}
            </div>
            <div className="an-bar-track">
              <div
                ref={el => { fillRefs.current[i] = el; }}
                className="an-bar-fill"
                style={{ width: '0%', background: color }}
              />
            </div>
            <div className="an-bar-pct" style={{ color }}>{phase.winRate.toFixed(1)}%</div>
            <div className="an-bar-meta">{phase.wins}/{phase.totalSimulations}</div>
          </div>
        );
      })}
    </div>
  );
}

// ── Main page ────────────────────────────────────────────────────
export default function Analytics() {
  const [league, setLeague] = useState<'PL' | 'SA'>('PL');

  // Each panel has its own tactic selection
  const [leagueTactic, setLeagueTactic] = useState('HIGH_PRESS');
  const [phaseTactic,  setPhaseTactic]  = useState('HIGH_PRESS');

  const [historicalData,    setHistoricalData]    = useState<TacticAnalytics[] | null>(null);
  const [historicalLoading, setHistoricalLoading] = useState(false);

  const [leagueNorm,        setLeagueNorm]        = useState<LeagueNormalizerResult | null>(null);
  const [leagueNormLoading, setLeagueNormLoading] = useState(false);

  const [phaseNorm,         setPhaseNorm]         = useState<PhaseNormalizerResult | null>(null);
  const [phaseNormLoading,  setPhaseNormLoading]  = useState(false);

  usePageTitle('Analytics');

  // Neutral page — remove any league theme class
  useEffect(() => {
    document.body.classList.remove('league-pl', 'league-sa');
  }, []);

  // Auto-load historical data whenever league changes
  useEffect(() => {
    setHistoricalLoading(true);
    setHistoricalData(null);
    fetch(`/api/analytics/win-rate-by-tactic?league=${league}`)
      .then(r => r.json())
      .then((d: TacticAnalytics[]) => { setHistoricalData(d); setHistoricalLoading(false); })
      .catch(() => setHistoricalLoading(false));
  }, [league]);

  // Clear normalizer results on league change
  useEffect(() => {
    setLeagueNorm(null);
    setPhaseNorm(null);
  }, [league]);

  function runLeagueNormalizer() {
    setLeagueNorm(null);
    setLeagueNormLoading(true);
    // 20 teams × 15 samples × 9 iterations = 2,700 trials → ±1.88% margin @ 95% CI
    fetch(`/api/analytics/league-normalizer?league=${league}&tactic=${leagueTactic}&samplesPerTeam=15&iterationsPerSample=9`)
      .then(r => r.json())
      .then((d: LeagueNormalizerResult) => { setLeagueNorm(d); setLeagueNormLoading(false); })
      .catch(() => setLeagueNormLoading(false));
  }

  function runPhaseNormalizer() {
    setPhaseNorm(null);
    setPhaseNormLoading(true);
    // 20 teams × 15 samples × 9 iterations = 2,700 trials per phase → ±1.88% margin @ 95% CI
    fetch(`/api/analytics/phase-normalizer?league=${league}&tactic=${phaseTactic}&samplesPerTeam=15&iterationsPerSample=9`)
      .then(r => r.json())
      .then((d: PhaseNormalizerResult) => { setPhaseNorm(d); setPhaseNormLoading(false); })
      .catch(() => setPhaseNormLoading(false));
  }

  const leagueName = league === 'PL' ? 'Premier League' : 'Serie A';

  return (
    <>
      <Header />
      <main className="an-page">

        {/* Hero */}
        <div className="an-hero">
          <h1 className="an-hero-title">Tactical Analytics</h1>
          <p className="an-hero-sub">Meta analysis and historical performance research</p>
        </div>

        {/* League toggle */}
        <div className="an-top-bar">
          <span className="an-top-label">League</span>
          <div className="an-league-toggle">
            <button
              className={`an-toggle-btn${league === 'PL' ? ' active' : ''}`}
              onClick={() => setLeague('PL')}
            >
              Premier League
            </button>
            <button
              className={`an-toggle-btn${league === 'SA' ? ' active' : ''}`}
              onClick={() => setLeague('SA')}
            >
              Serie A
            </button>
          </div>
        </div>

        {/* ── 3-panel grid ── */}
        <div className="an-panels">

          {/* Panel 1 — Historical Win Rates (auto-loads, no tactic picker) */}
          <div className="an-panel">
            <div className="an-panel-head">
              <div className="an-panel-head-row">
                <div>
                  <div className="an-panel-title">Historical Win Rates</div>
                  <div className="an-panel-desc">From logged simulations · {leagueName}</div>
                </div>
                <span className="an-badge">DB Logs</span>
              </div>
            </div>
            <div className="an-panel-body">
              {historicalLoading && (
                <div className="an-empty">
                  <div className="an-empty-loading">
                    <span className="an-spinner" /> Loading…
                  </div>
                </div>
              )}
              {!historicalLoading && historicalData !== null && historicalData.length === 0 && (
                <div className="an-empty">
                  No simulation logs for {leagueName} yet —<br />
                  run a simulation from any match page to populate this chart.
                </div>
              )}
              {!historicalLoading && historicalData && historicalData.length > 0 && (
                <HistoricalBars data={historicalData} />
              )}
            </div>
          </div>

          {/* Panel 2 — League Normalizer (per-panel tactic picker) */}
          <div className="an-panel">
            <div className="an-panel-head">
              <div className="an-panel-head-row">
                <div>
                  <div className="an-panel-title">League Normalizer</div>
                  <div className="an-panel-desc">Bias-free tactic strength · {leagueName}</div>
                </div>
                <span className="an-badge">Monte Carlo</span>
              </div>
            </div>
            <div className="an-panel-controls">
              <div className="an-panel-pills">
                {TACTICS.map(t => (
                  <button
                    key={t}
                    className={`an-panel-pill${leagueTactic === t ? ' active' : ''}`}
                    onClick={() => { setLeagueTactic(t); setLeagueNorm(null); }}
                  >{formatTactic(t)}</button>
                ))}
              </div>
              <div className="an-panel-run-row">
                <button
                  className="an-panel-run-btn"
                  onClick={runLeagueNormalizer}
                  disabled={leagueNormLoading}
                >
                  {leagueNormLoading
                    ? <><span className="an-spinner-inline" /> Running…</>
                    : '⚡ Run'}
                </button>
              </div>
            </div>
            <div className="an-panel-body">
              {leagueNormLoading && (
                <div className="an-empty">
                  <div className="an-empty-loading">
                    <span className="an-spinner" /> Simulating across all teams…
                  </div>
                </div>
              )}
              {!leagueNormLoading && !leagueNorm && (
                <div className="an-empty">
                  <span className="an-guide-label">How to use</span>
                  <div className="an-guide-steps">
                    <div className="an-guide-step">
                      <span className="an-guide-num">1</span>Pick a tactic above
                    </div>
                    <div className="an-guide-step">
                      <span className="an-guide-num">2</span>Click ⚡ Run
                    </div>
                  </div>
                </div>
              )}
              {!leagueNormLoading && leagueNorm && <LeagueNormCard data={leagueNorm} />}
            </div>
          </div>

          {/* Panel 3 — Phase Normalizer (per-panel tactic picker) */}
          <div className="an-panel">
            <div className="an-panel-head">
              <div className="an-panel-head-row">
                <div>
                  <div className="an-panel-title">Phase Normalizer</div>
                  <div className="an-panel-desc">Win rate across all 7 game phases · {leagueName}</div>
                </div>
                <span className="an-badge">Monte Carlo</span>
              </div>
            </div>
            <div className="an-panel-controls">
              <div className="an-panel-pills">
                {TACTICS.map(t => (
                  <button
                    key={t}
                    className={`an-panel-pill${phaseTactic === t ? ' active' : ''}`}
                    onClick={() => { setPhaseTactic(t); setPhaseNorm(null); }}
                  >{formatTactic(t)}</button>
                ))}
              </div>
              <div className="an-panel-run-row">
                <button
                  className="an-panel-run-btn"
                  onClick={runPhaseNormalizer}
                  disabled={phaseNormLoading}
                >
                  {phaseNormLoading
                    ? <><span className="an-spinner-inline" /> Running…</>
                    : '⚡ Run'}
                </button>
              </div>
            </div>
            <div className="an-panel-body">
              {phaseNormLoading && (
                <div className="an-empty">
                  <div className="an-empty-loading">
                    <span className="an-spinner" /> Simulating across all phases…
                  </div>
                </div>
              )}
              {!phaseNormLoading && !phaseNorm && (
                <div className="an-empty">
                  <span className="an-guide-label">How to use</span>
                  <div className="an-guide-steps">
                    <div className="an-guide-step">
                      <span className="an-guide-num">1</span>Pick a tactic above
                    </div>
                    <div className="an-guide-step">
                      <span className="an-guide-num">2</span>Click ⚡ Run
                    </div>
                  </div>
                </div>
              )}
              {!phaseNormLoading && phaseNorm && <PhaseNormChart data={phaseNorm} />}
            </div>
          </div>

        </div>
      </main>
      <Footer />
    </>
  );
}
