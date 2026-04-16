import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import { CRESTS } from '../constants/crests';

const QUERY_TYPES = [
  {
    key: 'dramatic',
    label: 'Dramatic',
    emoji: '🔥',
    description: 'Close calls, last-minute goals, and nervy finishes.',
  },
  {
    key: 'dominant',
    label: 'Dominant',
    emoji: '💪',
    description: 'Commanding performances where the result was never in doubt.',
  },
  {
    key: 'comeback',
    label: 'Comeback',
    emoji: '⚡',
    description: 'Moments when the team fought back from behind.',
  },
  {
    key: 'pressure',
    label: 'Under Pressure',
    emoji: '🛡️',
    description: 'Backs against the wall — defending deep and holding on.',
  },
  {
    key: 'turning_point',
    label: 'Turning Points',
    emoji: '🔄',
    description: 'The exact moments when everything changed.',
  },
  {
    key: 'surprise',
    label: 'Surprise Me',
    emoji: '🎲',
    description: 'Let the system decide. Something unexpected every time.',
  },
];

export default function QuerySelect() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const teamId = params.get('teamId') ?? '';
  const teamName = decodeURIComponent(params.get('teamName') ?? '');
  const league = params.get('league') ?? 'PL';
  const mode = params.get('mode') ?? 'simple';

  useLeagueTheme(league);
  usePageTitle(`${teamName} — What kind of moments?`);

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
          <h1 className="hero-title">What kind of moments?</h1>
          <p className="hero-sub">Pick a theme and we'll find the most significant moments from the season.</p>
        </div>

        <div className="query-grid">
          {QUERY_TYPES.map(q => (
            <button
              key={q.key}
              className="query-card"
              onClick={() =>
                navigate(
                  `/moments?teamId=${teamId}&teamName=${encodeURIComponent(teamName)}&league=${league}&mode=${mode}&queryType=${q.key}`
                )
              }
            >
              <span className="query-emoji">{q.emoji}</span>
              <h3 className="query-label">{q.label}</h3>
              <p className="query-description">{q.description}</p>
            </button>
          ))}
        </div>

        <div className="fixtures-back" style={{ marginTop: 32 }}>
          <Link
            to={`/mode?teamId=${teamId}&teamName=${encodeURIComponent(teamName)}&league=${league}`}
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
