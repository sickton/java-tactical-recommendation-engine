import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import { CRESTS } from '../constants/crests';
import type { Moment, StoryResponse } from '../types';
import { getMomentImage } from '../constants/momentImages';

export default function Moments() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const teamId = params.get('teamId') ?? '';
  const teamName = decodeURIComponent(params.get('teamName') ?? '');
  const league = params.get('league') ?? 'PL';
  const mode = params.get('mode') ?? 'simple';
  const queryType = params.get('queryType') ?? 'dramatic';

  const [moments, setMoments] = useState<Moment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useLeagueTheme(league);
  usePageTitle(`${teamName} — Season Moments`);

  useEffect(() => {
    setLoading(true);
    setError(false);
    fetch(`/api/story?team=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}&queryType=${queryType}`)
      .then(r => {
        if (!r.ok) throw new Error('Failed');
        return r.json();
      })
      .then((data: StoryResponse) => {
        setMoments(data.moments);
        setLoading(false);
      })
      .catch(() => {
        setError(true);
        setLoading(false);
      });
  }, [teamName, league, mode, queryType]);

  const queryLabels: Record<string, string> = {
    dramatic: '🔥 Dramatic',
    dominant: '💪 Dominant',
    comeback: '⚡ Comeback',
    pressure: '🛡️ Under Pressure',
    turning_point: '🔄 Turning Points',
    surprise: '🎲 Surprise',
  };

  return (
    <>
      <Header />
      <main className="container">
        <div className="page-hero">
          <div className="mode-team-badge">
            <img
              src={CRESTS[teamName] ?? ''}
              alt={teamName}
              className="mode-team-crest"
              onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
            />
            <span className="mode-team-name">{teamName}</span>
          </div>
          <h1 className="hero-title">{queryLabels[queryType] ?? queryType} Moments</h1>
          <p className="hero-sub">
            {mode === 'simple' ? '5 moments' : '10 moments'} from the 2024/25 season. Pick one to explore.
          </p>
        </div>

        {loading && (
          <div className="moments-grid">
            {Array.from({ length: mode === 'simple' ? 5 : 10 }).map((_, i) => (
              <div key={i} className="moment-card-skeleton">
                <div className="skeleton-row">
                  <div className="skeleton-line" style={{ width: '48px' }} />
                  <div className="skeleton-line" style={{ width: '90px' }} />
                </div>
                <div className="skeleton-line" style={{ width: '80%', height: '18px' }} />
                <div className="skeleton-line" style={{ width: '60%', height: '18px' }} />
                <div className="skeleton-line" style={{ width: '40%', height: '14px' }} />
                <div className="skeleton-line" style={{ width: '100%' }} />
                <div className="skeleton-line" style={{ width: '90%' }} />
                <div className="skeleton-line" style={{ width: '70%' }} />
              </div>
            ))}
          </div>
        )}

        {error && (
          <div className="moments-error">
            <p>Something went wrong. Make sure the RAG service is running.</p>
            <Link to={`/query?teamId=${teamId}&teamName=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}`} className="btn-back">
              &#8592; Try again
            </Link>
          </div>
        )}

        {!loading && !error && (
          <div className="moments-grid">
            {moments.map((moment, i) => (
              <button
                key={i}
                className="moment-card"
                style={{ '--moment-bg': `url(${getMomentImage(i)})` } as React.CSSProperties}
                onClick={() =>
                  navigate('/moment', {
                    state: { moment, teamName, league, teamId, mode, queryType, imageIndex: i },
                  })
                }
              >
                <div className="moment-card-bg" />
                <span className="moment-number">{String(i + 1).padStart(2, '0')}</span>
                <div className="moment-card-top">
                  <span className="moment-minute">⏱ {moment.minute}'</span>
                  <span className="moment-concept">{moment.concept}</span>
                </div>
                <h3 className="moment-headline">{moment.headline}</h3>
                <p className="moment-match">{moment.match}</p>
                <p className="moment-score">{moment.score}</p>
                <p className="moment-narrative">{moment.narrative}</p>
                <span className="moment-cta">Explore this moment →</span>
              </button>
            ))}
          </div>
        )}

        <div className="fixtures-back" style={{ marginTop: 32 }}>
          <Link
            to={`/query?teamId=${teamId}&teamName=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}`}
            className="btn-back"
          >
            &#8592; Back
          </Link>
        </div>
      </main>
      <Footer />
    </>
  );
}
