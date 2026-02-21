import { useEffect, useRef, useState, useCallback } from 'react';
import { Link, useLocation } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import TeamCrest from '../components/TeamCrest';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import { formatTactic } from '../utils/formatTactic';
import type { MatchEvent, SimulationData } from '../types';

const ALL_TACTICS = ['GEGENPRESSING', 'HIGH_PRESS', 'TIKI_TAKA', 'CONTROL', 'COUNTER_ATTACK', 'DIRECT_PLAY', 'LOW_BLOCK'];

const PHASES: [number, number, string][] = [
  [0,  15, 'Early Minutes'],
  [16, 44, 'Closing Half'],
  [45, 50, 'Half Time'],
  [51, 60, 'Build Phase'],
  [61, 70, 'Tension Time'],
  [71, 87, 'Late Game'],
  [88, 90, 'Stoppage Time'],
];

const ATMOSPHERE = [
  'Ball worked across the midfield...',
  'Keeper claims the cross comfortably.',
  'Free kick won in a dangerous position.',
  'Corner cleared to the edge of the box.',
  'Referee waves play on.',
  'Great touch to control — but the shot goes wide.',
  'Long ball forward, keeper off his line to collect.',
  'Midfielder slots a through ball — flagged offside.',
  "Set piece rehearsed in training... doesn't quite come off.",
  'Foul given. Tempers rising slightly.',
  'The fans are getting restless.',
  'Substitution being considered on the touchline.',
  'Good pressing from the front forces a mistake.',
  'Counter-attack snuffed out by a last-ditch tackle.',
  'The wall is set. Free kick tipped over the bar!',
  'End to end. Intensity picking up.',
  'The manager is pacing the technical area.',
  'Brilliant save to keep the score level!',
  'Nerves starting to show in the final third.',
  'Yellow card. Players crowd the referee.',
  "They're sitting deep now, hard to break down.",
  'Brilliant through ball — but the flag goes up.',
  'The clock is ticking...',
  'Chance! Just over the bar.',
  'Crowd roars as the chance goes begging.',
];

function findPhaseIdx(minute: number): number {
  for (let i = 0; i < PHASES.length; i++) {
    if (minute >= PHASES[i][0] && minute <= PHASES[i][1]) return i;
  }
  return PHASES.length - 1;
}

function iqRatingLabel(score: number, outcome: 'win' | 'draw' | 'loss'): string {
  const high = score >= 60;
  const medium = score >= 30 && score < 60;
  if (outcome === 'win') {
    if (high)   return 'Elite Tactician';
    if (medium) return 'Bold Gaffer';
    return 'Maverick';
  }
  if (outcome === 'draw') {
    if (high)   return 'Solid Gaffer';
    if (medium) return 'Work in Progress';
    return 'Gambler';
  }
  if (high)   return 'Nearly There';
  if (medium) return 'Out of Your Depth';
  return 'Clueless in the Dugout';
}

type SimPhase = 'running' | 'picking' | 'done';

interface FeedItem { id: number; type: string; label: string; }

export default function Simulation() {
  const { state: initData } = useLocation() as { state: SimulationData };
  useLeagueTheme(initData?.league ?? null);
  usePageTitle(initData?.teamName ? `${initData.teamName} — Simulation` : 'Simulation');

  // ── refs (mutable, no re-render) ─────────────────────────────────────────
  const simEventsRef      = useRef<MatchEvent[]>([]);
  const eventCursorRef    = useRef(0);
  const pausedRef         = useRef(false);
  const currentMinuteRef  = useRef(initData?.startMinute ?? 1);
  const tickRef           = useRef<ReturnType<typeof setInterval> | null>(null);
  const lastAtmoMinuteRef = useRef(-4);
  const tacticPerPhaseRef = useRef<Record<number, string>>({});
  const lastPickerPhaseRef = useRef(-1);
  const cumulMatchRef     = useRef(0);
  const cumulTotalRef     = useRef(0);
  const liveHomeRef       = useRef(0);
  const liveAwayRef       = useRef(0);
  const feedRef           = useRef<HTMLDivElement>(null);
  const fidelityFillRef   = useRef<HTMLDivElement>(null);

  // ── state (triggers re-render) ───────────────────────────────────────────
  const [minute, setMinute]               = useState(initData?.startMinute ?? 1);
  const [phaseLabel, setPhaseLabel]       = useState('Simulating...');
  const [progressPct, setProgressPct]    = useState((initData?.startMinute ?? 1) / 90 * 100);
  const [teamGoals, setTeamGoals]         = useState(0);
  const [oppGoals, setOppGoals]           = useState(0);
  const [currentTactic, setCurrentTactic] = useState(initData?.startTactic ?? 'CONTROL');
  const [feed, setFeed]                   = useState<FeedItem[]>([]);
  const [simPhase, setSimPhase]           = useState<SimPhase>('running');
  const [pickerPhaseIdx, setPickerPhaseIdx] = useState(-1);
  const [pickerTactic, setPickerTactic]   = useState('');
  const [finalData, setFinalData]         = useState<{ teamG: number; oppG: number; iqScore: number; rating: string; detail: string } | null>(null);
  const [liveBadgeText, setLiveBadgeText] = useState('LIVE');

  const nextId = useRef(0);
  const addFeedItem = useCallback((type: string, label: string) => {
    const id = nextId.current++;
    setFeed(prev => [...prev, { id, type, label }]);
    setTimeout(() => {
      feedRef.current?.scrollTo({ top: feedRef.current.scrollHeight, behavior: 'smooth' });
    }, 50);
  }, []);

  // ── tick helpers ──────────────────────────────────────────────────────────
  const setMinuteDisplay = useCallback((min: number) => {
    const clamped = Math.min(90, min);
    setMinute(clamped);
    setProgressPct(clamped / 90 * 100);
    for (const [s, e, name] of PHASES) {
      if (clamped >= s && clamped <= e) { setPhaseLabel(name); break; }
    }
  }, []);

  const refreshScore = useCallback((homeG: number, awayG: number) => {
    const isHome = initData?.isHome ?? true;
    setTeamGoals(isHome ? homeG : awayG);
    setOppGoals(isHome ? awayG : homeG);
  }, [initData]);

  const handleEvent = useCallback((ev: MatchEvent) => {
    if (ev.type === 'GOAL_HOME' || ev.type === 'GOAL_AWAY') {
      if (ev.type === 'GOAL_HOME') liveHomeRef.current++;
      else liveAwayRef.current++;
      refreshScore(liveHomeRef.current, liveAwayRef.current);
      const isHome = initData?.isHome ?? true;
      const scorer = ev.type === 'GOAL_HOME'
        ? (isHome ? initData.teamName : initData.opponentName)
        : (isHome ? initData.opponentName : initData.teamName);
      const label = `${ev.minute}' — GOAL! ${scorer} score! ${liveHomeRef.current}-${liveAwayRef.current}`;
      addFeedItem(ev.type.toLowerCase(), label);
      return;
    }
    if (ev.type === 'PHASE_CHANGE') {
      addFeedItem('phase_change', ev.label);
      const phIdx = findPhaseIdx(ev.minute);
      if (phIdx > lastPickerPhaseRef.current) {
        pausedRef.current = true;
        setTimeout(() => {
          setPickerPhaseIdx(phIdx);
          setPickerTactic(tacticPerPhaseRef.current[phIdx] ?? (initData?.startTactic ?? 'CONTROL'));
          setSimPhase('picking');
        }, 600);
      }
      return;
    }
    addFeedItem(ev.type.toLowerCase(), ev.label);
  }, [addFeedItem, refreshScore, initData]);

  const showFinalResult = useCallback(() => {
    if (tickRef.current) clearInterval(tickRef.current);
    setMinuteDisplay(90);
    setLiveBadgeText('FT');
    const isHome = initData?.isHome ?? true;
    const tg = isHome ? liveHomeRef.current : liveAwayRef.current;
    const og = isHome ? liveAwayRef.current : liveHomeRef.current;
    const total = cumulTotalRef.current;
    const matching = cumulMatchRef.current;
    const iqScore = total === 0 ? 100 : Math.round(matching / total * 100);
    const outcome = tg > og ? 'win' : tg === og ? 'draw' : 'loss';
    const rating = iqRatingLabel(iqScore, outcome);
    const resultLine = outcome === 'win' ? 'Victory!' : outcome === 'draw' ? 'Draw' : 'Defeat';
    const phaseDetail = total === 0
      ? 'No tactic changes made'
      : `${matching} / ${total} tactic picks matched the engine`;
    setFinalData({ teamG: tg, oppG: og, iqScore, rating, detail: `${resultLine} — ${phaseDetail}` });
    setSimPhase('done');

    // Animate fidelity bar
    if (fidelityFillRef.current) {
      const el = fidelityFillRef.current;
      el.style.transition = 'none';
      el.style.width = '0%';
      requestAnimationFrame(() => requestAnimationFrame(() => {
        el.style.transition = 'width 1.2s cubic-bezier(0.4,0,0.2,1)';
        el.style.width = iqScore + '%';
      }));
    }
  }, [initData, setMinuteDisplay]);

  const fireEventsAtMinute = useCallback((min: number) => {
    const events = simEventsRef.current;
    while (eventCursorRef.current < events.length && events[eventCursorRef.current].minute <= min) {
      handleEvent(events[eventCursorRef.current++]);
    }
  }, [handleEvent]);

  const maybeAtmosphere = useCallback((min: number) => {
    const hasReal = simEventsRef.current.some(ev => ev.minute === min);
    if (hasReal) return;
    if (min - lastAtmoMinuteRef.current < 4) return;
    lastAtmoMinuteRef.current = min;
    const text = ATMOSPHERE[Math.floor(Math.random() * ATMOSPHERE.length)];
    addFeedItem('atmosphere', `${min}' — ${text}`);
  }, [addFeedItem]);

  const startClock = useCallback((fromMin: number) => {
    if (tickRef.current) clearInterval(tickRef.current);
    currentMinuteRef.current = fromMin;
    setMinuteDisplay(fromMin);
    tickRef.current = setInterval(() => {
      if (pausedRef.current) return;
      if (currentMinuteRef.current < 90) {
        currentMinuteRef.current++;
        setMinuteDisplay(currentMinuteRef.current);
        fireEventsAtMinute(currentMinuteRef.current);
        maybeAtmosphere(currentMinuteRef.current);
      } else {
        if (tickRef.current) clearInterval(tickRef.current);
        setTimeout(showFinalResult, 800);
      }
    }, 1000);
  }, [setMinuteDisplay, fireEventsAtMinute, maybeAtmosphere, showFinalResult]);

  // ── Bootstrap ─────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!initData?.events?.length) {
      showFinalResult();
      return;
    }

    const d = initData;
    liveHomeRef.current = d.startHomeGoals;
    liveAwayRef.current = d.startAwayGoals;
    refreshScore(liveHomeRef.current, liveAwayRef.current);

    for (let i = 0; i < 7; i++) tacticPerPhaseRef.current[i] = d.startTactic;
    simEventsRef.current = d.events;
    lastPickerPhaseRef.current = findPhaseIdx(d.startMinute);

    addFeedItem('kickoff', `▶ Simulation starts at ${d.startMinute}'`);
    setTimeout(() => startClock(d.startMinute), 1200);

    return () => { if (tickRef.current) clearInterval(tickRef.current); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Phase picker confirm ───────────────────────────────────────────────────
  async function handlePickerConfirm() {
    if (!pickerTactic || !initData) return;
    setSimPhase('running');
    pausedRef.current = false;

    for (let i = pickerPhaseIdx; i < 7; i++) tacticPerPhaseRef.current[i] = pickerTactic;
    lastPickerPhaseRef.current = pickerPhaseIdx;
    setCurrentTactic(pickerTactic);

    const fromMin = Math.max(PHASES[pickerPhaseIdx]?.[0] ?? currentMinuteRef.current, currentMinuteRef.current);
    const body = new URLSearchParams({
      teamId:        String(initData.teamId),
      matchId:       String(initData.matchId),
      league:        initData.league,
      fromMinute:    String(fromMin),
      fromHomeGoals: String(liveHomeRef.current),
      fromAwayGoals: String(liveAwayRef.current),
      tacticsJson:   JSON.stringify(tacticPerPhaseRef.current),
    });

    try {
      const resp = await fetch('/api/simulate/phase', { method: 'POST', body, headers: { 'Content-Type': 'application/x-www-form-urlencoded' } });
      const data = await resp.json();
      const newEvents: MatchEvent[] = (data.events ?? []).filter(
        (ev: MatchEvent) => !(ev.type === 'PHASE_CHANGE' && ev.minute <= currentMinuteRef.current)
      );
      simEventsRef.current = simEventsRef.current.slice(0, eventCursorRef.current);
      simEventsRef.current.push(...newEvents);
      cumulMatchRef.current += (data.matchingPhases ?? 0);
      cumulTotalRef.current += (data.totalPhases ?? 0);
      startClock(currentMinuteRef.current);
    } catch {
      startClock(currentMinuteRef.current);
    }
  }

  if (!initData) {
    return (
      <>
        <Header />
        <main className="sim-page">
          <p style={{ padding: '2rem', color: 'var(--ac)' }}>No simulation data. <Link to="/">Play Again</Link></p>
        </main>
        <Footer />
      </>
    );
  }

  const { teamName, opponentName } = initData;

  return (
    <>
      <Header />
      <main className="sim-page">

        <div className="match-breadcrumb">
          <Link to="/" className="match-breadcrumb-link">← Play Again</Link>
        </div>

        {/* Scoreboard */}
        <div className="sim-hero">
          <div className="sim-hero-inner">
            <div className="sim-hero-team">
              <TeamCrest teamName={teamName} className="hero-crest sim-crest-managed" />
              <span className="sim-hero-name">{teamName}</span>
            </div>

            <div className="sim-hero-score-block">
              <div className={`sim-live-badge${simPhase === 'done' ? ' sim-ft-badge' : ''}`}>{liveBadgeText}</div>
              <div className="sim-score-display">
                <span className="sim-score-val">{teamGoals}</span>
                <span className="sim-score-sep">&ndash;</span>
                <span className="sim-score-val">{oppGoals}</span>
              </div>
              <div className="sim-minute-badge">{minute}'</div>
              <div className="sim-tactic-live">
                Tactic: <span>{formatTactic(currentTactic)}</span>
              </div>
            </div>

            <div className="sim-hero-team">
              <TeamCrest teamName={opponentName} className="hero-crest sim-crest-opponent" />
              <span className="sim-hero-name">{opponentName}</span>
            </div>
          </div>

          <div className="match-phase-bar" style={{ marginTop: '10px' }}>
            <div className="match-phase-track">
              <div className="match-phase-fill" style={{ width: `${progressPct}%` }}></div>
            </div>
            <div className="match-phase-label">{phaseLabel}</div>
          </div>
        </div>

        {/* Split layout */}
        <div className="sim-split">

          {/* LEFT: Event feed */}
          <div className="sim-timeline-panel">
            <div className="sim-panel-header"><span>▶ Match Timeline</span></div>
            <div className="sim-timeline-feed" ref={feedRef}>
              {feed.map(item => (
                <div key={item.id} className={`sim-event sim-event-${item.type}`}>{item.label}</div>
              ))}
            </div>
          </div>

          {/* RIGHT: Picker / Result / Waiting */}
          <div className="sim-picker-panel">

            {/* Phase picker */}
            {simPhase === 'picking' && (
              <div className="sim-picker-card" style={{ display: 'flex' }}>
                <div className="sim-picker-header">
                  <span className="sim-picker-phase-name">{PHASES[pickerPhaseIdx]?.[2] ?? 'Next Phase'}</span>
                  <span className="sim-picker-subtitle">What's your call, Gaffer?</span>
                </div>
                <div className="sim-picker-tactics">
                  {ALL_TACTICS.map(t => (
                    <button
                      key={t}
                      type="button"
                      className={`sim-tactic-pill${pickerTactic === t ? ' active' : ''}`}
                      onClick={() => setPickerTactic(t)}
                    >
                      {formatTactic(t)}
                    </button>
                  ))}
                </div>
                <button className="btn-simulate sim-picker-confirm" onClick={handlePickerConfirm}>
                  Continue ▶
                </button>
              </div>
            )}

            {/* Final result */}
            {simPhase === 'done' && finalData && (
              <div className="sim-result-card" style={{ display: 'flex' }}>
                <div className="sim-result-ft-badge">FULL TIME</div>
                <div className="sim-result-score-big">
                  <span>{finalData.teamG}</span>
                  <span className="sim-score-sep">&ndash;</span>
                  <span>{finalData.oppG}</span>
                </div>
                <div className="sim-result-team-labels">
                  <span>{teamName}</span>
                  <span>{opponentName}</span>
                </div>

                <div className="sim-fidelity-block">
                  <span className="sim-fidelity-label">Tactical IQ</span>
                  <div className="sim-fidelity-bar-track">
                    <div ref={fidelityFillRef} className="sim-fidelity-bar-fill" style={{ width: '0%' }}></div>
                  </div>
                  <span className="sim-fidelity-pct">{finalData.iqScore}%</span>
                </div>

                <div className="sim-rating-badge">{finalData.rating}</div>
                <div className="sim-iq-detail">{finalData.detail}</div>

                <Link to="/" className="btn-primary sim-play-again">Play Again →</Link>
              </div>
            )}

            {/* Waiting / running */}
            {simPhase === 'running' && (
              <div className="sim-waiting" style={{ display: 'flex' }}>
                <div className="sim-waiting-spinner"></div>
                <p className="sim-waiting-label">Match in progress</p>
                <div className="sim-waiting-stats">
                  <div className="sim-stat-row">
                    <span className="sim-stat-label">Tactic</span>
                    <span className="sim-stat-value">{formatTactic(currentTactic)}</span>
                  </div>
                  <div className="sim-stat-row">
                    <span className="sim-stat-label">Phase</span>
                    <span className="sim-stat-value">{phaseLabel}</span>
                  </div>
                  <div className="sim-stat-row">
                    <span className="sim-stat-label">Score</span>
                    <span className="sim-stat-value">{teamGoals} – {oppGoals}</span>
                  </div>
                </div>
                <p className="sim-waiting-hint">Tactic picker opens at each phase boundary</p>
              </div>
            )}

          </div>
        </div>

      </main>
      <Footer />
    </>
  );
}
