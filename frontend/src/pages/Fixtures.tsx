import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import { TEAM_CODES } from '../constants/teamCodes';
import { CRESTS } from '../constants/crests';
import type { Fixture } from '../types';

interface FixturesData {
  teamId: number;
  teamName: string;
  league: string;
  homeFixtures: Fixture[];
  awayFixtures: Fixture[];
}

function FixtureCrest({ code }: { code: string }) {
  const fullName = TEAM_CODES[code] ?? code;
  // Try to find a crest by full name, or any key that starts with the resolved name
  const src = CRESTS[fullName] ?? '';
  if (!src) return null;
  return (
    <img
      src={src}
      alt={fullName}
      className="fixture-crest"
      onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
    />
  );
}

export default function Fixtures() {
  const [params] = useSearchParams();
  const teamId = params.get('teamId') ?? '1';
  const league = params.get('league') ?? 'PL';
  const navigate = useNavigate();
  const [data, setData] = useState<FixturesData | null>(null);

  useLeagueTheme(league);
  usePageTitle(data ? `${data.teamName} — Fixtures` : 'Fixtures');

  useEffect(() => {
    fetch(`/api/fixtures?teamId=${teamId}&league=${league}`)
      .then(r => r.json())
      .then(setData);
  }, [teamId, league]);

  if (!data) return <><Header /><main className="container"><p style={{ color: '#fff', padding: 40 }}>Loading...</p></main><Footer /></>;

  const goToMatch = (matchId: number) =>
    navigate(`/match?matchId=${matchId}&teamId=${teamId}&league=${league}`);

  const renderFixtures = (list: Fixture[], isHome: boolean) =>
    list.map(f => {
      const dashIdx = f.title.indexOf('-');
      const homeCode = dashIdx >= 0 ? f.title.slice(0, dashIdx) : f.title;
      const awayCode  = dashIdx >= 0 ? f.title.slice(dashIdx + 1) : '';
      // Only show the opponent — user already knows which team they picked
      const oppCode = isHome ? awayCode : homeCode;
      const oppName = TEAM_CODES[oppCode] ?? oppCode;
      return (
        <div key={f.id} className="fixture-row" onClick={() => goToMatch(f.id)}>
          <div className="fixture-pills">
            <FixtureCrest code={oppCode} />
            <span className="fixture-opp-name">{oppName}</span>
          </div>
        </div>
      );
    });

  return (
    <>
      <Header />
      <main className="container">
        <div className="fixtures-page-hero">
          <h1 className="hero-title">{data.teamName}</h1>
          <p className="hero-sub">Select a match to analyse</p>
        </div>

        <div className="fixtures-page">
          <div className="fixtures-panels">
            <div className="fixtures-panel">
              <div className="fixtures-section-header">Home Fixtures</div>
              <div className="fixtures-list">{renderFixtures(data.homeFixtures, true)}</div>
            </div>
            <div className="fixtures-panel-divider" />
            <div className="fixtures-panel">
              <div className="fixtures-section-header">Away Fixtures</div>
              <div className="fixtures-list">{renderFixtures(data.awayFixtures, false)}</div>
            </div>
          </div>
        </div>

        <div className="fixtures-back">
          <Link to={`/clubs?league=${league}`} className="btn-back">&#8592; Back to Club Selection</Link>
        </div>
      </main>
      <Footer />
    </>
  );
}
