import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import TeamCrest from '../components/TeamCrest';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import { formatTactic } from '../utils/formatTactic';
import { TACTIC_GUIDE } from '../constants/tacticGuide';
import type { MatchData } from '../types';

const ALL_TACTICS = Object.keys(TACTIC_GUIDE);

function animateCount(el: HTMLElement, target: number, duration: number) {
  const start = performance.now();
  const step = (now: number) => {
    const t = Math.min((now - start) / duration, 1);
    const ease = 1 - Math.pow(1 - t, 3);
    el.textContent = String(Math.round(ease * target));
    if (t < 1) requestAnimationFrame(step);
  };
  requestAnimationFrame(step);
}

export default function Match() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const matchId = Number(params.get('matchId'));
  const teamId = Number(params.get('teamId'));
  const league = params.get('league') || 'PL';

  useLeagueTheme(league);

  const [matchData, setMatchData] = useState<MatchData | null>(null);
  const homeLabel = matchData ? matchData.context.home.name : '';
  const awayLabel = matchData ? matchData.context.away.name : '';
  usePageTitle(homeLabel && awayLabel ? `${homeLabel} vs ${awayLabel}` : 'Match');
  const [selectedTactic, setSelectedTactic] = useState<string>('');
  const [hoveredTactic, setHoveredTactic] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const homeScoreRef = useRef<HTMLSpanElement>(null);
  const awayScoreRef = useRef<HTMLSpanElement>(null);
  const didAnimate = useRef(false);

  useEffect(() => {
    fetch(`/api/match?matchId=${matchId}&teamId=${teamId}&league=${league}`)
      .then(r => r.json())
      .then((data: MatchData) => setMatchData(data))
      .catch(() => setError('Failed to load match data.'));
  }, [matchId, teamId, league]);

  // Score counter-roll animation
  useEffect(() => {
    if (!matchData || didAnimate.current) return;
    didAnimate.current = true;
    const homeGoals = matchData.context.homeGoals;
    const awayGoals = matchData.context.awayGoals;
    if (homeGoals > 0 && homeScoreRef.current) {
      homeScoreRef.current.textContent = '0';
      setTimeout(() => animateCount(homeScoreRef.current!, homeGoals, 700), 300);
    }
    if (awayGoals > 0 && awayScoreRef.current) {
      awayScoreRef.current.textContent = '0';
      setTimeout(() => animateCount(awayScoreRef.current!, awayGoals, 700), 300);
    }
  }, [matchData]);

  async function handleRecommend() {
    if (!selectedTactic || !matchData) return;
    setSubmitting(true);
    try {
      const body = new URLSearchParams({
        teamId: String(teamId),
        matchId: String(matchId),
        minute: String(matchData.minute),
        userTactic: selectedTactic,
        league,
      });
      const resp = await fetch('/api/recommend', { method: 'POST', body, headers: { 'Content-Type': 'application/x-www-form-urlencoded' } });
      const data = await resp.json();
      navigate('/result', { state: { ...data, matchId, teamId, league } });
    } catch {
      setError('Failed to get recommendation.');
      setSubmitting(false);
    }
  }

  async function handleSimulate() {
    if (!selectedTactic || !matchData) return;
    setSubmitting(true);
    try {
      const body = new URLSearchParams({
        teamId: String(teamId),
        matchId: String(matchId),
        minute: String(matchData.minute),
        userTactic: selectedTactic,
        league,
      });
      const resp = await fetch('/api/simulate', { method: 'POST', body, headers: { 'Content-Type': 'application/x-www-form-urlencoded' } });
      const data = await resp.json();
      navigate('/simulation', { state: data });
    } catch {
      setError('Failed to start simulation.');
      setSubmitting(false);
    }
  }

  const displayedTactic = hoveredTactic || (selectedTactic ? selectedTactic : null);
  const guideData = displayedTactic ? TACTIC_GUIDE[displayedTactic] : null;

  if (error) return <><Header /><main className="match-page"><p style={{ padding: '2rem', color: 'var(--ac)' }}>{error}</p></main><Footer /></>;
  if (!matchData) return <><Header /><main className="match-page"><p style={{ padding: '2rem', opacity: 0.5 }}>Loading match...</p></main><Footer /></>;

  const { context, gamePhase, teamName } = matchData;

  return (
    <>
      <Header />
      <main className="match-page">

        <div className="match-breadcrumb">
          <Link to={`/fixtures?teamId=${teamId}&league=${league}`} className="match-breadcrumb-link">← Back to Fixtures</Link>
        </div>

        {/* Hero Scoreboard */}
        <div className="match-hero">
          <div className="match-hero-code">{context.title}</div>

          <div className="match-hero-board">
            <div className="match-hero-team">
              <div className="hero-crest-badge">
                <TeamCrest teamName={context.home.name} className="hero-crest" />
              </div>
              <span className="match-hero-name">{context.home.name}</span>
              <div className="match-hero-pills">
                <span className="stat-pill">Stamina: {context.home.staminaLevel}</span>
                <span className="stat-pill">Adapt: {context.home.adaptabilityLevel}</span>
              </div>
            </div>

            <div className="match-hero-centre">
              <div className="match-hero-score">
                <span ref={homeScoreRef}>{context.homeGoals}</span>
                <span className="match-hero-score-sep">&ndash;</span>
                <span ref={awayScoreRef}>{context.awayGoals}</span>
              </div>
              <div className="match-hero-minute-badge">{matchData.minute}'</div>
            </div>

            <div className="match-hero-team">
              <div className="hero-crest-badge">
                <TeamCrest teamName={context.away.name} className="hero-crest" />
              </div>
              <span className="match-hero-name">{context.away.name}</span>
              <div className="match-hero-pills">
                <span className="stat-pill">Stamina: {context.away.staminaLevel}</span>
                <span className="stat-pill">Adapt: {context.away.adaptabilityLevel}</span>
              </div>
            </div>
          </div>

          <div className="match-phase-bar">
            <div className="match-phase-track">
              <div className="match-phase-fill" style={{ width: `${matchData.minute}%` }}></div>
            </div>
            <div className="match-phase-label">{gamePhase}</div>
          </div>
        </div>

        {/* Split layout */}
        <div className="match-split">

          {/* LEFT: Tactic picker */}
          <div className="match-left">
            <div className="tactic-section">
              <div className="dugout-card">
                <span className="dugout-label">You are managing</span>
                <span className="dugout-team">{teamName}</span>
                <span className="dugout-prompt">What tactic would you play?</span>
              </div>

              <div className="tactic-grid">
                {ALL_TACTICS.map(tactic => (
                  <label
                    key={tactic}
                    className={`tactic-option${selectedTactic === tactic ? ' selected' : ''}`}
                    data-tactic={tactic}
                    onMouseEnter={() => setHoveredTactic(tactic)}
                    onMouseLeave={() => setHoveredTactic(null)}
                  >
                    <input
                      type="radio"
                      name="userTactic"
                      value={tactic}
                      checked={selectedTactic === tactic}
                      onChange={() => setSelectedTactic(tactic)}
                    />
                    <span className="tactic-name">{formatTactic(tactic)}</span>
                  </label>
                ))}
              </div>

              <div className="match-action-row">
                <button
                  className="btn-primary"
                  disabled={!selectedTactic || submitting}
                  onClick={handleRecommend}
                >
                  Get Recommendation →
                </button>
                <button
                  className="btn-simulate"
                  disabled={!selectedTactic || submitting}
                  onClick={handleSimulate}
                >
                  Simulate Match ▶
                </button>
              </div>
            </div>
          </div>

          {/* RIGHT: Tactic Guide Panel */}
          <div className="tactic-guide-panel">
            <div className="tactic-guide-header">
              <span className="tactic-guide-icon">☰</span>
              <span className="tactic-guide-title">Tactic Guide</span>
              <span className="tactic-guide-hint">Hover a tactic to learn more</span>
            </div>
            <div className="tactic-guide-body">
              {!guideData ? (
                <div className="tactic-guide-default">
                  <p>Not sure which tactic to pick?</p>
                  <p>Hover over any tactic on the left to see a plain-English explanation, real-life examples, and when it works best.</p>
                  <p>No football knowledge required.</p>
                </div>
              ) : (
                <div className="tactic-guide-content">
                  <div className="guide-tactic-name">
                    <span className="guide-emoji">{guideData.emoji}</span>
                    <span>{formatTactic(displayedTactic!)}</span>
                  </div>
                  <p className="guide-tagline">{guideData.tagline}</p>

                  <div className="guide-section">
                    <div className="guide-section-label">What is it?</div>
                    <p>{guideData.whatIsIt}</p>
                  </div>
                  <div className="guide-section">
                    <div className="guide-section-label">Real-life example</div>
                    <p>{guideData.realLife}</p>
                  </div>
                  <div className="guide-section">
                    <div className="guide-section-label">When it works</div>
                    <p className="guide-positive">{guideData.whenItWorks}</p>
                  </div>
                  <div className="guide-section">
                    <div className="guide-section-label">When it doesn't</div>
                    <p className="guide-negative">{guideData.whenItDoesnt}</p>
                  </div>
                  <div className="guide-analogy">
                    <span className="guide-analogy-emoji">{guideData.analogyEmoji}</span>
                    <span>{guideData.analogy}</span>
                  </div>
                </div>
              )}
            </div>
          </div>

        </div>
      </main>
      <Footer />
    </>
  );
}
