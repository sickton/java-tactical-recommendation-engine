import { useEffect, useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import type { Moment, PuzzleResponse } from '../types';
import BreakThePress from '../components/puzzles/BreakThePress';

interface LocationState {
  moment: Moment;
  teamName: string;
  league: string;
  teamId: string;
  mode: string;
  queryType: string;
  imageIndex: number;
}

function getOpposition(matchStr: string): string {
  const atHome = matchStr.match(/^(.+?)\s+at home$/i);
  if (atHome) return atHome[1].trim();
  const away = matchStr.match(/^(.+?)\s+away$/i);
  if (away) return away[1].trim();
  const parts = matchStr.split(/\s+vs\.?\s+/i);
  return parts.length === 2 ? parts[1].trim() : '';
}

function formatGamePhase(phase: string): string {
  return phase.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
}

export default function Puzzle() {
  const location = useLocation();
  const navigate = useNavigate();
  const state = location.state as LocationState | null;
  const { moment, teamName, league, teamId, mode, queryType, imageIndex } = state ?? {};

  const [puzzleData, setPuzzleData] = useState<PuzzleResponse | null>(null);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState<string | null>(null);

  useLeagueTheme(league ?? null);
  usePageTitle('Tactical Puzzle');

  const pressingTeam = moment ? getOpposition(moment.match) : '';

  useEffect(() => {
    if (!moment || !teamName) {
      setError('Missing moment data.');
      setLoading(false);
      return;
    }
    if (!pressingTeam) {
      setError('Could not identify the opposition from this moment.');
      setLoading(false);
      return;
    }

    const [homeGoals, awayGoals] = (() => {
      const nums = moment.score.match(/\d+/g);
      return nums && nums.length >= 2 ? [parseInt(nums[0]), parseInt(nums[1])] : [0, 0];
    })();

    fetch('/api/puzzle', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        moment: {
          headline:         moment.headline,
          minute:           moment.minute,
          match:            moment.match,
          score:            moment.score,
          concept:          moment.concept,
          team:             teamName,
          tactical_problem: moment.tactical_problem ?? '',
          mission:          moment.mission ?? '',
        },
        league,
        pressing_team: pressingTeam,
        home_goals:    homeGoals,
        away_goals:    awayGoals,
      }),
    })
      .then(r => {
        if (!r.ok) throw new Error(`Server error: ${r.status}`);
        return r.json();
      })
      .then((data: PuzzleResponse) => {
        setPuzzleData(data);
        setLoading(false);
      })
      .catch(err => {
        setError('Failed to load puzzle. Make sure the services are running.');
        setLoading(false);
      });
  }, []);

  function handleRetry() {
    navigate('/moment', {
      state: { moment, teamName, league, teamId, mode, queryType, imageIndex },
    });
  }

  // ── No state ─────────────────────────────────────────────────────────────
  if (!moment) {
    return (
      <>
        <Header />
        <main className="container">
          <p style={{ color: '#8b949e', marginTop: 48 }}>
            No moment selected. <Link to="/" className="btn-back">Go home</Link>
          </p>
        </main>
        <Footer />
      </>
    );
  }

  // ── Context bar shared across all puzzle types ────────────────────────────
  const contextBar = (
    <div className="puzzle-context-bar">
      <div className="puzzle-context-item">
        <span className="puzzle-context-label">MATCH</span>
        <span className="puzzle-context-value">{moment.match}</span>
      </div>
      <div className="puzzle-context-item">
        <span className="puzzle-context-label">SCORE</span>
        <span className="puzzle-context-value puzzle-context-score">{moment.score}</span>
      </div>
      <div className="puzzle-context-item">
        <span className="puzzle-context-label">MINUTE</span>
        <span className="puzzle-context-value">{moment.minute}'</span>
      </div>
      {puzzleData && (
        <div className="puzzle-context-item">
          <span className="puzzle-context-label">PHASE</span>
          <span className="puzzle-context-value">
            {formatGamePhase(puzzleData.config.game_phase)}
          </span>
        </div>
      )}
      <span className="puzzle-concept-pill">{moment.concept}</span>
    </div>
  );

  return (
    <>
      <Header />
      <main className="container puzzle-page">

        {contextBar}

        {/* ── Loading ── */}
        {loading && (
          <div className="puzzle-loading">
            <div className="moments-spinner" />
            <p>Building tactical puzzle…</p>
          </div>
        )}

        {/* ── Error ── */}
        {error && (
          <div className="moments-error" style={{ marginTop: 40 }}>
            <p>{error}</p>
            <button className="btn-back" onClick={handleRetry}>← Back to moment</button>
          </div>
        )}

        {/* ── Puzzle router — renders the right component based on puzzle_type ── */}
        {!loading && !error && puzzleData && (() => {
          switch (puzzleData.puzzle_type) {
            case 'break_the_press':
              return (
                <BreakThePress
                  config={puzzleData.config}
                  brief={puzzleData.brief}
                  moment={moment}
                  teamName={teamName ?? ''}
                  pressingTeamName={pressingTeam}
                  onRetry={handleRetry}
                />
              );
            default:
              return (
                <div className="moments-error" style={{ marginTop: 40 }}>
                  <p>Unknown puzzle type: {puzzleData.puzzle_type}</p>
                  <button className="btn-back" onClick={handleRetry}>← Back</button>
                </div>
              );
          }
        })()}

      </main>
      <Footer />
    </>
  );
}
