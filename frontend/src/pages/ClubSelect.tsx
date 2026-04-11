import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { useLeagueTheme } from '../hooks/useLeagueTheme';
import { usePageTitle } from '../hooks/usePageTitle';
import { CRESTS } from '../constants/crests';

interface Team { id: number; name: string; }

export default function ClubSelect() {
  const [params] = useSearchParams();
  const league = params.get('league') ?? 'PL';
  const navigate = useNavigate();
  const [teams, setTeams] = useState<Team[]>([]);
  const [search, setSearch] = useState('');

  useLeagueTheme(league);
  usePageTitle(league === 'SA' ? 'Serie A — Pick Your Club' : 'Premier League — Pick Your Club');

  useEffect(() => {
    fetch(`/api/clubs?league=${league}`)
      .then(r => r.json())
      .then(d => setTeams(d.teams ?? []));
  }, [league]);

  const filtered = search.trim()
    ? teams.filter(t => t.name.toLowerCase().includes(search.toLowerCase()))
    : teams;

  return (
    <>
      <Header />
      <main className="container">
        <div className="page-hero">
          <h1 className="hero-title">Pick Your Club</h1>
          <p className="hero-sub">
            {league === 'SA'
              ? 'Select a Serie A team to start your tactical session.'
              : 'Select a Premier League team to start your tactical session.'}
          </p>
        </div>

        <div className="club-search-wrap">
          <input
            className="club-search-input"
            type="text"
            placeholder="Search clubs..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            autoComplete="off"
          />
        </div>

        <div className="club-grid">
          {filtered.map(team => (
            <button
              key={team.id}
              className="club-card"
              onClick={() => navigate(`/mode?teamId=${team.id}&teamName=${encodeURIComponent(team.name)}&league=${league}`)}
            >
              <div className="crest-badge">
                <img
                  src={CRESTS[team.name] ?? ''}
                  alt={team.name}
                  className="club-crest"
                  onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
                />
              </div>
              <span className="club-name">{team.name}</span>
            </button>
          ))}
        </div>

        <div className="fixtures-back" style={{ marginTop: 24 }}>
          <Link to="/" className="btn-back">&#8592; Back to League Selection</Link>
        </div>
      </main>
      <Footer />
    </>
  );
}
