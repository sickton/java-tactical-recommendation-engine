import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import { CRESTS } from '../constants/crests';
import { TEAM_CODES } from '../constants/teamCodes';
import type { Fixture } from '../types';

interface Team { id: number; name: string; }
interface FixtureData {
  teamName: string;
  homeFixtures: Fixture[];
  awayFixtures: Fixture[];
}

/** Reverse lookup: full team name → 3-letter code */
const NAME_TO_CODE: Record<string, string> = Object.fromEntries(
  Object.entries(TEAM_CODES).map(([code, name]) => [name, code])
);

type Phase = 'loading' | 'spinning' | 'revealed';
type Arena = 'home' | 'away';
interface SpinFrame { team: Team | null; key: number; }

export default function Fixtures() {
  const [params]  = useSearchParams();
  const teamId    = parseInt(params.get('teamId') ?? '1');
  const league    = params.get('league') ?? 'PL';
  const navigate  = useNavigate();

  const [allTeams,    setAllTeams]    = useState<Team[]>([]);
  const [fixtureData, setFixtureData] = useState<FixtureData | null>(null);
  const [opponent,    setOpponent]    = useState<Team | null>(null);
  const [phase,       setPhase]       = useState<Phase>('loading');
  const [spinFrame,   setSpinFrame]   = useState<SpinFrame>({ team: null, key: 0 });

  useLeagueTheme(league);
  usePageTitle('Finding Your Opponent...');

  /* ── 1. Fetch clubs + fixtures in parallel, then pick random opponent ── */
  useEffect(() => {
    Promise.all([
      fetch(`/api/clubs?league=${league}`).then(r => r.json()),
      fetch(`/api/fixtures?teamId=${teamId}&league=${league}`).then(r => r.json()),
    ]).then(([clubData, fData]) => {
      const teams: Team[] = clubData.teams ?? [];
      setAllTeams(teams);
      setFixtureData(fData);

      const others = teams.filter(t => t.id !== teamId);
      const chosen = others[Math.floor(Math.random() * others.length)];
      setOpponent(chosen);
      setPhase('spinning');
    });
  }, []);

  /* ── 2. Spin animation — rapid crest cycling that eases to a stop ────── */
  useEffect(() => {
    if (phase !== 'spinning' || !opponent || allTeams.length === 0) return;

    const others       = allTeams.filter(t => t.id !== teamId);
    const TOTAL_FRAMES = 32;
    let   frame        = 0;
    let   timerId: ReturnType<typeof setTimeout>;

    function step() {
      frame++;
      const progress = frame / TOTAL_FRAMES;

      if (frame < TOTAL_FRAMES) {
        // Random crest each frame; key always increments so React remounts the <img>
        const idx = Math.floor(Math.random() * others.length);
        setSpinFrame({ team: others[idx], key: frame });
        // Ease-out: starts ~55 ms, slows to ~475 ms by final frames
        const delay = 55 + Math.pow(progress, 2.6) * 420;
        timerId = setTimeout(step, delay);
      } else {
        // Land on the chosen opponent
        setSpinFrame({ team: opponent, key: frame });
        setTimeout(() => setPhase('revealed'), 260);
      }
    }

    timerId = setTimeout(step, 55);
    return () => clearTimeout(timerId);
  }, [phase]);

  /* ── 3. User picks Home or Away, then navigate to the right fixture ─────── */
  const handleArenaSelect = (arena: Arena) => {
    if (!opponent || !fixtureData) return;

    const oppCode = NAME_TO_CODE[opponent.name];
    if (!oppCode) return;

    // homeFixtures: user is home  → title = "USERCODE-OPPCODE"
    // awayFixtures: user is away  → title = "OPPCODE-USERCODE"
    const match = arena === 'home'
      ? fixtureData.homeFixtures.find(f => f.title.split('-')[1] === oppCode)
      : fixtureData.awayFixtures.find(f => f.title.split('-')[0] === oppCode);

    if (!match) return;
    navigate(`/match?matchId=${match.id}&teamId=${teamId}&league=${league}`);
  };

  /* ── Derived display values ───────────────────────────────────────────── */
  const userTeamName = fixtureData?.teamName ?? '';
  const userCrest    = CRESTS[userTeamName]  ?? '';
  const spinCrest    = spinFrame.team ? (CRESTS[spinFrame.team.name] ?? '') : '';
  const isSpinning   = phase === 'spinning';
  const isRevealed   = phase === 'revealed';

  return (
    <>
      <Header />
      <main className="container">
        <div className="op-page">

          {/* ── Hero text ──────────────────────────────────────────────── */}
          <div className="op-hero">
            <h1 className="hero-title">
              {isRevealed ? 'Match Found' : 'Finding your next opponent...'}
            </h1>
            <p className="hero-sub">
              {isRevealed
                ? 'Choose your arena, Gaffer. Home or away?'
                : 'The engine is scanning the fixture list.'}
            </p>
          </div>

          {/* ── VS card — renders once data is loaded ──────────────────── */}
          {phase !== 'loading' && (
            <div className={`op-versus-card${isRevealed ? ' op-card-revealed' : ''}`}>

              {/* Left: user's team (static) */}
              <div className="op-team-block">
                <div className="op-crest-wrap op-crest-static">
                  {userCrest && (
                    <img src={userCrest} alt={userTeamName} className="op-crest" />
                  )}
                </div>
                <span className="op-team-name">{userTeamName}</span>
              </div>

              {/* Centre: VS badge */}
              <div className="op-vs-badge">VS</div>

              {/* Right: opponent — spinning or revealed */}
              <div className="op-team-block">
                <div className={`op-crest-wrap${isSpinning ? ' op-crest-spinning' : ''}${isRevealed ? ' op-crest-revealed' : ''}`}>

                  {/* key change forces React remount → CSS entrance animation fires */}
                  {spinFrame.team && (
                    <img
                      key={spinFrame.key}
                      src={spinCrest}
                      alt={spinFrame.team.name}
                      className="op-crest"
                    />
                  )}

                  {/* Scanning line — only while spinning */}
                  {isSpinning && <div className="op-scan-line" />}
                </div>

                <span className={`op-team-name${isRevealed ? ' op-name-reveal' : ' op-name-hidden'}`}>
                  {isRevealed ? opponent?.name : '???'}
                </span>
              </div>

            </div>
          )}

          {/* ── Arena picker — slides in after reveal ──────────────────── */}
          {isRevealed && (
            <div className="op-action">
              <div className="op-arena-row">
                <button className="op-arena-btn" onClick={() => handleArenaSelect('home')}>
                  <span className="op-arena-venue">Home</span>
                  <span className="op-arena-desc">Play at your ground</span>
                </button>
                <button className="op-arena-btn" onClick={() => handleArenaSelect('away')}>
                  <span className="op-arena-venue">Away</span>
                  <span className="op-arena-desc">Travel to their stadium</span>
                </button>
              </div>
            </div>
          )}

        </div>
      </main>
      <Footer />
    </>
  );
}
