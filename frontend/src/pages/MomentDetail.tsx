import { useEffect, useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import { CRESTS } from '../constants/crests';
import type { Moment, ExplainResponse } from '../types';
import { getMomentImage } from '../constants/momentImages';

interface LocationState {
  moment: Moment;
  teamName: string;
  league: string;
  teamId: string;
  mode: string;
  queryType: string;
  imageIndex: number;
}

const MISSION_MAP: Record<string, { problem: string; task: string }> = {
  'Press Resistance':     { problem: 'Your team are trapped under pressure and need a way out.', task: 'Find the passing route that breaks the press.' },
  'High Press':           { problem: 'The press is on — one mistake and possession is gone.', task: 'Pick the sequence that survives the press.' },
  'Weak-Side Switch':     { problem: 'The ball is pinned on one side. The far side is open.', task: 'Find the switch before the defence adjusts.' },
  'Third-Man Run':        { problem: 'The obvious pass is covered. A third player is making a run.', task: 'Spot the move and play the right ball.' },
  'Counter Attack':       { problem: 'Space has opened up in behind. The moment is now.', task: 'Pick the right route before the defence recovers.' },
  'Game Management':      { problem: 'The lead is there to protect. Every pass matters.', task: 'Keep the ball and see the game out.' },
  'Cover Shadow':         { problem: 'A player is being blocked out of the game deliberately.', task: 'Find the pass that bypasses the shadow.' },
  'Turning Point':        { problem: 'The match is at a tipping point. One decision changes everything.', task: 'Make the right call under pressure.' },
  'Dominant Possession':  { problem: 'Your team are in control but need to find the opening.', task: 'Move the ball to unlock the defence.' },
  'Comeback':             { problem: 'Your team are behind. The game is running out.', task: 'Find the move that shifts the momentum.' },
  'Under Pressure':       { problem: 'The opposition are squeezing space. Options are shrinking.', task: 'Escape the press and find a foothold.' },
  'Surprise':             { problem: 'Something unexpected is unfolding on the pitch.', task: 'Spot what changed and react to it.' },
};

function getMission(concept: string): { problem: string; task: string } {
  return (
    MISSION_MAP[concept] ?? {
      problem: 'A tactical problem is unfolding on the pitch.',
      task: 'Solve it on the pitch.',
    }
  );
}

export default function MomentDetail() {
  const location = useLocation();
  const navigate = useNavigate();
  const state = location.state as LocationState;

  const { moment, teamName, league, teamId, mode, queryType, imageIndex } = state ?? {};
  const heroImage = getMomentImage(imageIndex ?? 0);

  const [explanation, setExplanation] = useState<string>('');
  const [displayedExplanation, setDisplayedExplanation] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [breakdownOpen, setBreakdownOpen] = useState(false);

  useLeagueTheme(league);
  usePageTitle(moment?.headline ?? 'Moment Detail');

  useEffect(() => {
    if (!moment) return;

    fetch('/api/explain', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        headline: moment.headline,
        match: moment.match,
        minute: moment.minute,
        score: moment.score,
        concept: moment.concept,
        team: teamName,
      }),
    })
      .then(r => r.json())
      .then((data: ExplainResponse) => {
        setExplanation(data.explanation);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, [moment, teamName]);

  useEffect(() => {
    if (!explanation || !breakdownOpen) return;
    let i = 0;
    setDisplayedExplanation('');
    const timer = setInterval(() => {
      i++;
      setDisplayedExplanation(explanation.slice(0, i));
      if (i >= explanation.length) clearInterval(timer);
    }, 12);
    return () => clearInterval(timer);
  }, [explanation, breakdownOpen]);

  if (!moment) {
    return (
      <>
        <Header />
        <main className="container">
          <p style={{ color: '#8b949e', marginTop: 48 }}>No moment selected. <Link to="/" className="btn-back">Go home</Link></p>
        </main>
        <Footer />
      </>
    );
  }

  const mission = {
    problem: moment.tactical_problem ?? getMission(moment.concept).problem,
    task:    moment.mission        ?? getMission(moment.concept).task,
  };

  return (
    <>
      <Header />
      <main className="container">
        <div className="detail-hero-banner">
          <img src={heroImage} alt="" className="detail-hero-img" />
          <div className="detail-hero-overlay" />
          <div className="detail-hero-content">
            <div className="mode-team-badge">
              <img
                src={CRESTS[teamName] ?? ''}
                alt={teamName}
                className="mode-team-crest"
                onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
              />
              <span className="mode-team-name">{teamName}</span>
            </div>
          </div>
        </div>

        <div className="detail-card">
          {/* Context strip */}
          <div className="detail-meta">
            <span className="moment-minute">⏱ {moment.minute}'</span>
            <span className="moment-concept">{moment.concept}</span>
          </div>
          <h1 className="detail-headline">{moment.headline}</h1>
          <p className="detail-match">{moment.match} &mdash; {moment.score}</p>

          <div className="detail-divider" />

          {/* Narrative hook */}
          <h3 className="detail-section-title">What happened</h3>
          <p className="detail-narrative">{moment.narrative}</p>

          <div className="detail-divider" />

          {/* Mission briefing */}
          <div className="brief-block">
            <p className="brief-problem">{mission.problem}</p>
            <p className="brief-task">Your mission: {mission.task}</p>
          </div>

          {/* Primary CTA */}
          <button
            className="btn-puzzle-cta btn-puzzle-cta--inline"
            onClick={() =>
              navigate('/puzzle', {
                state: { moment, teamName, league, teamId, mode, queryType, imageIndex },
              })
            }
          >
            ⚽ Take on the Puzzle
          </button>

          {/* Collapsible full breakdown */}
          <button
            className="brief-breakdown-toggle"
            onClick={() => setBreakdownOpen(o => !o)}
          >
            {breakdownOpen ? '▲ Hide breakdown' : '▼ See the tactical breakdown'}
          </button>

          {breakdownOpen && (
            <div className="brief-breakdown">
              {loading ? (
                <div className="moments-loading">
                  <div className="moments-spinner" />
                  <p>Generating explanation...</p>
                </div>
              ) : (
                <p className="detail-explanation">
                  {displayedExplanation}
                  {displayedExplanation.length < explanation.length && (
                    <span className="typewriter-cursor">|</span>
                  )}
                </p>
              )}
            </div>
          )}
        </div>

        {/* Secondary nav */}
        <div className="detail-nav-row">
          <button
            className="btn-back"
            onClick={() =>
              navigate(`/moments?teamId=${teamId}&teamName=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}&queryType=${queryType}`)
            }
          >
            &#8592; Other moments
          </button>
          <button
            className="btn-back"
            onClick={() =>
              navigate(`/query?teamId=${teamId}&teamName=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}`)
            }
          >
            🔄 Different theme
          </button>
          <button
            className="btn-back"
            onClick={() => navigate(`/clubs?league=${league}`)}
          >
            🏟️ Another team
          </button>
        </div>
      </main>
      <Footer />
    </>
  );
}
