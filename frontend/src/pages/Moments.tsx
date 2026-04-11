import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import { CRESTS } from '../constants/crests';
import type { Moment, StoryResponse } from '../types';
import { getMomentImage } from '../constants/momentImages';

const QUERY_TYPES = [
  { key: 'dramatic',      label: 'Dramatic',        emoji: '🔥' },
  { key: 'dominant',      label: 'Dominant',         emoji: '💪' },
  { key: 'comeback',      label: 'Comeback',         emoji: '⚡' },
  { key: 'pressure',      label: 'Under Pressure',   emoji: '🛡️' },
  { key: 'turning_point', label: 'Turning Points',   emoji: '🔄' },
  { key: 'surprise',      label: 'Surprise',         emoji: '🎲' },
];

interface CacheEntry { moments: Moment[]; ts: number; }

function getCacheEntry(key: string): CacheEntry | null {
  const raw = sessionStorage.getItem(key);
  if (!raw) return null;
  try { return JSON.parse(raw) as CacheEntry; } catch { return null; }
}

function relativeTime(ts: number): string {
  const diff = Math.floor((Date.now() - ts) / 1000);
  if (diff < 60) return 'just now';
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  return `${Math.floor(diff / 3600)}h ago`;
}

export default function Moments() {
  const [params, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const teamId   = params.get('teamId') ?? '';
  const teamName = decodeURIComponent(params.get('teamName') ?? '');
  const league   = params.get('league') ?? 'PL';
  const mode     = params.get('mode') ?? 'simple';
  const queryType = params.get('queryType') ?? 'dramatic';

  const [moments, setMoments] = useState<Moment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(false);
  const [cacheTs, setCacheTs] = useState<number | null>(null);

  // Tracks which query types have been cached this session (for dot indicators)
  const [cachedTypes, setCachedTypes] = useState<Set<string>>(() => {
    const s = new Set<string>();
    QUERY_TYPES.forEach(qt => {
      if (getCacheEntry(`moments:${teamName}:${league}:${mode}:${qt.key}`)) s.add(qt.key);
    });
    return s;
  });

  // Per-type bypass-cache flag (set on Regenerate, cleared after fresh fetch)
  const [forceRefresh, setForceRefresh] = useState<Set<string>>(new Set());

  useLeagueTheme(league);
  usePageTitle(`${teamName} — Season Moments`);

  const cacheKey = `moments:${teamName}:${league}:${mode}:${queryType}`;

  // ── Main fetch ──────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!forceRefresh.has(queryType)) {
      const entry = getCacheEntry(cacheKey);
      if (entry) {
        setMoments(entry.moments);
        setCacheTs(entry.ts);
        setLoading(false);
        setError(false);
        return;
      }
    }

    setLoading(true);
    setError(false);
    setCacheTs(null);

    fetch(`/api/story?team=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}&queryType=${queryType}`)
      .then(r => { if (!r.ok) throw new Error('Failed'); return r.json(); })
      .then((data: StoryResponse) => {
        const entry: CacheEntry = { moments: data.moments, ts: Date.now() };
        sessionStorage.setItem(cacheKey, JSON.stringify(entry));
        setMoments(entry.moments);
        setCacheTs(entry.ts);
        setCachedTypes(prev => new Set([...prev, queryType]));
        setForceRefresh(prev => { const n = new Set(prev); n.delete(queryType); return n; });
        setLoading(false);
      })
      .catch(() => { setError(true); setLoading(false); });
  }, [teamName, league, mode, queryType, forceRefresh]);

  // ── Background prefetch of all other query types ─────────────────────────
  useEffect(() => {
    if (loading || error) return;

    QUERY_TYPES
      .filter(qt => qt.key !== queryType)
      .forEach(qt => {
        const key = `moments:${teamName}:${league}:${mode}:${qt.key}`;
        if (getCacheEntry(key)) return; // already cached

        fetch(`/api/story?team=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}&queryType=${qt.key}`)
          .then(r => r.ok ? r.json() : null)
          .then((data: StoryResponse | null) => {
            if (!data) return;
            const entry: CacheEntry = { moments: data.moments, ts: Date.now() };
            sessionStorage.setItem(key, JSON.stringify(entry));
            setCachedTypes(prev => new Set([...prev, qt.key]));
          })
          .catch(() => {});
      });
  }, [loading, error]);

  // ── Handlers ────────────────────────────────────────────────────────────────
  function handleTabChange(key: string) {
    if (key === queryType) return;
    setSearchParams(prev => { prev.set('queryType', key); return prev; });
  }

  function handleRegenerate() {
    sessionStorage.removeItem(cacheKey);
    setCachedTypes(prev => { const n = new Set(prev); n.delete(queryType); return n; });
    setForceRefresh(prev => new Set([...prev, queryType]));
  }

  const count = mode === 'simple' ? 5 : 10;

  return (
    <>
      <Header />
      <main className="container">

        {/* ── Hero ── */}
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
          <p className="hero-sub" style={{ marginTop: 6 }}>
            {count} moments from the 2024/25 season. Pick one to explore.
          </p>
        </div>

        {/* ── Query-type tab switcher ── */}
        <div className="moments-tabs">
          {QUERY_TYPES.map(qt => (
            <button
              key={qt.key}
              className={`moments-tab${qt.key === queryType ? ' active' : ''}`}
              onClick={() => handleTabChange(qt.key)}
              disabled={loading && qt.key !== queryType}
            >
              {qt.emoji} {qt.label}
              {cachedTypes.has(qt.key) && qt.key !== queryType && (
                <span className="moments-tab-dot" title="Cached" />
              )}
            </button>
          ))}
        </div>

        {/* ── Skeletons ── */}
        {loading && (
          <div className="moments-grid">
            {Array.from({ length: count }).map((_, i) => (
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

        {/* ── Error ── */}
        {error && (
          <div className="moments-error">
            <p>Something went wrong. Make sure the RAG service is running.</p>
            <Link
              to={`/query?teamId=${teamId}&teamName=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}`}
              className="btn-back"
            >
              &#8592; Try again
            </Link>
          </div>
        )}

        {/* ── Moment cards ── */}
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

        {/* ── Footer bar ── */}
        <div className="moments-footer-bar">
          <Link
            to={`/query?teamId=${teamId}&teamName=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}`}
            className="btn-back"
          >
            &#8592; Back
          </Link>

          <div className="moments-regen-group">
            {cacheTs && !loading && (
              <span className="moments-cache-label">Generated {relativeTime(cacheTs)}</span>
            )}
            <button className="btn-back" onClick={handleRegenerate} disabled={loading}>
              &#8635; Regenerate
            </button>
          </div>
        </div>

      </main>
      <Footer />
    </>
  );
}
