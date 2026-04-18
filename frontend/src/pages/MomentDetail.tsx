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

export default function MomentDetail() {
  const location = useLocation();
  const navigate = useNavigate();
  const state = location.state as LocationState;

  const { moment, teamName, league, teamId, mode, queryType, imageIndex } = state ?? {};
  const heroImage = getMomentImage(imageIndex ?? 0);

  const [explanation, setExplanation] = useState<string>('');
  const [displayedExplanation, setDisplayedExplanation] = useState<string>('');
  const [loading, setLoading] = useState(true);

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
    if (!explanation) return;
    let i = 0;
    setDisplayedExplanation('');
    const timer = setInterval(() => {
      i++;
      setDisplayedExplanation(explanation.slice(0, i));
      if (i >= explanation.length) clearInterval(timer);
    }, 12);
    return () => clearInterval(timer);
  }, [explanation]);

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

  return (
    <>
      <Header />
      <main className="container">
        <div className="detail-hero-banner">
          <img
            src={heroImage}
            alt=""
            className="detail-hero-img"
          />
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
          <div className="detail-meta">
            <span className="moment-minute">⏱ {moment.minute}'</span>
            <span className="moment-concept">{moment.concept}</span>
          </div>
          <h1 className="detail-headline">{moment.headline}</h1>
          <p className="detail-match">{moment.match}</p>
          <p className="detail-score">{moment.score}</p>

          <div className="detail-divider" />

          <h3 className="detail-section-title">What happened</h3>
          <p className="detail-narrative">{moment.narrative}</p>

          <h3 className="detail-section-title">Understanding the moment</h3>
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

        <div style={{ display: 'flex', gap: 16, marginTop: 32, flexWrap: 'wrap' }}>
          <button
            className="btn-puzzle-cta"
            onClick={() =>
              navigate('/puzzle', {
                state: { moment, teamName, league, teamId, mode, queryType, imageIndex },
              })
            }
          >
            ⚽ Try the Tactical Puzzle
          </button>
        </div>

        <div style={{ display: 'flex', gap: 16, marginTop: 16, flexWrap: 'wrap' }}>
          <button
            className="btn-back"
            onClick={() =>
              navigate(`/moments?teamId=${teamId}&teamName=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}&queryType=${queryType}`)
            }
          >
            &#8592; Explore other moments
          </button>
          <button
            className="btn-back"
            onClick={() =>
              navigate(`/query?teamId=${teamId}&teamName=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}`)
            }
          >
            🔄 Try a different theme
          </button>
          <button
            className="btn-back"
            onClick={() => navigate(`/clubs?league=${league}`)}
          >
            🏟️ Try another team
          </button>
        </div>
      </main>
      <Footer />
    </>
  );
}
