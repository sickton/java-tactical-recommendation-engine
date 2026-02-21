import { useEffect, useRef } from 'react';
import { Link, useLocation } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import TeamCrest from '../components/TeamCrest';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import { formatTactic } from '../utils/formatTactic';
import type { RecommendResult } from '../types';

interface ResultState extends RecommendResult {
  matchId: number;
  teamId: number;
  league: string;
}

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

export default function Result() {
  const { state } = useLocation() as { state: ResultState };
  useLeagueTheme(state?.league ?? null);
  usePageTitle(state?.teamName ? `${state.teamName} — Recommendation` : 'Result');

  const confidenceFillRef = useRef<HTMLDivElement>(null);
  const homeScoreRef = useRef<HTMLSpanElement>(null);
  const awayScoreRef = useRef<HTMLSpanElement>(null);
  const didAnimate = useRef(false);

  useEffect(() => {
    if (!state || didAnimate.current) return;
    didAnimate.current = true;

    // Confidence bar
    const fill = confidenceFillRef.current;
    if (fill) {
      const pct = state.recommendation.confidence;
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          fill.style.transition = 'width 1.1s cubic-bezier(0.4,0,0.2,1)';
          fill.style.width = pct + '%';
        });
      });
    }

    // Score counter-roll
    if (state.teamGoals > 0 && homeScoreRef.current) {
      homeScoreRef.current.textContent = '0';
      setTimeout(() => animateCount(homeScoreRef.current!, state.teamGoals, 700), 300);
    }
    if (state.opponentGoals > 0 && awayScoreRef.current) {
      awayScoreRef.current.textContent = '0';
      setTimeout(() => animateCount(awayScoreRef.current!, state.opponentGoals, 700), 300);
    }
  }, [state]);

  if (!state) {
    return (
      <>
        <Header />
        <main className="result-page">
          <p style={{ padding: '2rem', color: 'var(--ac)' }}>No result data found. <Link to="/">Play Again</Link></p>
        </main>
        <Footer />
      </>
    );
  }

  const { teamName, opponentName, minute, teamGoals, opponentGoals, userTactic, recommendation, agrees, explanation, league, teamId, matchId } = state;

  const briefingLines = explanation
    .split('\n')
    .map(l => l.trim())
    .filter(l => l.length > 0)
    .map(l => l.startsWith('- ') ? '• ' + l.slice(2) : l);

  return (
    <>
      <Header />
      <main className="result-page">

        <div className="match-breadcrumb">
          <Link to="/" className="match-breadcrumb-link">← Play Again</Link>
        </div>

        {/* Hero Scoreboard */}
        <div className="match-hero">
          <div className="match-hero-code">Tactical Recommendation</div>

          <div className="match-hero-board">
            <div className="match-hero-team">
              <TeamCrest teamName={teamName} className="hero-crest" />
              <span className="match-hero-name">{teamName}</span>
              <div className="match-hero-pills">
                <span className="stat-pill">Your pick: {formatTactic(userTactic)}</span>
              </div>
            </div>

            <div className="match-hero-centre">
              <div className="match-hero-score">
                <span ref={homeScoreRef}>{teamGoals}</span>
                <span className="match-hero-score-sep">&ndash;</span>
                <span ref={awayScoreRef}>{opponentGoals}</span>
              </div>
              <div className="match-hero-minute-badge">{minute}'</div>
            </div>

            <div className="match-hero-team">
              <TeamCrest teamName={opponentName} className="hero-crest" />
              <span className="match-hero-name">{opponentName}</span>
              <div className="match-hero-pills">
                <span className="stat-pill">Opponent</span>
              </div>
            </div>
          </div>

          <div className="match-phase-bar">
            <div className="match-phase-track">
              <div className="match-phase-fill" style={{ width: `${minute}%` }}></div>
            </div>
            <div className="match-phase-label">{minute} minutes played</div>
          </div>
        </div>

        {/* Split layout */}
        <div className="result-split">

          {/* LEFT: Verdict card */}
          <div className="result-verdict">
            <div className={`verdict-banner${agrees ? ' verdict-agree' : ' verdict-disagree'}`}>
              <span className="verdict-banner-icon">{agrees ? '✓' : '→'}</span>
              {agrees
                ? 'Yes Gaffer! Spot on — I agree with you.'
                : 'Gaffer, I think we should try something different.'}
            </div>

            <div className="verdict-tactic-block">
              <div className="verdict-label">Engine Recommends</div>
              <div className="verdict-tactic-name">{formatTactic(recommendation.tactic)}</div>
              <div className="verdict-formation-badge">{recommendation.formation}</div>

              <div className="verdict-confidence-row">
                <span className="verdict-confidence-label">Engine Confidence</span>
                <span className="verdict-confidence-pct">{recommendation.confidence}%</span>
              </div>
              <div className="verdict-confidence-track">
                <div
                  ref={confidenceFillRef}
                  className="verdict-confidence-fill"
                  style={{ width: '0%' }}
                ></div>
              </div>
            </div>

            {!agrees && (
              <div className="verdict-user-tactic">
                <span className="verdict-user-label">You picked</span>
                <span className="verdict-user-name">{formatTactic(userTactic)}</span>
              </div>
            )}

            <Link to={`/match?matchId=${matchId}&teamId=${teamId}&league=${league}`} className="btn-primary result-play-again">
              Try Again →
            </Link>
          </div>

          {/* RIGHT: Explanation */}
          <div className="result-explanation-panel">
            <div className="result-explanation-header">
              <span className="tactic-guide-icon">▶</span>
              <span className="tactic-guide-title">Tactical Briefing</span>
            </div>
            <div className="result-explanation-body">
              <div className="briefing-text">
                {briefingLines.map((line, i) => (
                  <div key={i} className="briefing-line">{line}</div>
                ))}
              </div>
            </div>
          </div>

        </div>
      </main>
      <Footer />
    </>
  );
}
