import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import { CRESTS } from '../constants/crests';

export default function ModeSelect() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const teamId = params.get('teamId') ?? '';
  const teamName = decodeURIComponent(params.get('teamName') ?? '');
  const league = params.get('league') ?? 'PL';

  useLeagueTheme(league);
  usePageTitle(`${teamName} — Choose Your Mode`);

  const modes = [
    {
      key: 'simple',
      label: 'Simple',
      count: '5 Moments',
      description: 'A quick snapshot of the season. Five defining moments, curated and narrated.',
      icon: '⚡',
    },
    {
      key: 'indepth',
      label: 'In-Depth',
      count: '10 Moments',
      description: 'A deeper dive. Ten moments across the full season — patterns, drama, and detail.',
      icon: '🔍',
    },
  ];

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
          <h1 className="hero-title">How deep do you want to go?</h1>
          <p className="hero-sub">Choose how many moments from this season's story you want to explore.</p>
        </div>

        <div className="mode-grid">
          {modes.map(mode => (
            <button
              key={mode.key}
              className="mode-card"
              onClick={() =>
                navigate(`/query?teamId=${teamId}&teamName=${encodeURIComponent(teamName)}&league=${league}&mode=${mode.key}`)
              }
            >
              <span className="mode-icon">{mode.icon}</span>
              <h2 className="mode-label">{mode.label}</h2>
              <span className="mode-count">{mode.count}</span>
              <p className="mode-description">{mode.description}</p>
            </button>
          ))}
        </div>

        <div className="fixtures-back" style={{ marginTop: 32 }}>
          <Link to={`/clubs?league=${league}`} className="btn-back">&#8592; Back to Club Selection</Link>
        </div>
      </main>
      <Footer />
    </>
  );
}
