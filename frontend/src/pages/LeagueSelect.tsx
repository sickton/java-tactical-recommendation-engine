import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import { usePageTitle } from '../hooks/usePageTitle';

export default function LeagueSelect() {
  const navigate = useNavigate();

  usePageTitle('Choose Your League');

  // Landing page is always neutral — strip any league theme and clear saved league
  useEffect(() => {
    document.body.classList.remove('league-pl', 'league-sa');
    localStorage.removeItem('jgaffer_league');
  }, []);

  return (
    <>
      <Header />
      <main className="container">
        <div className="page-hero">
          <h1 className="hero-title">Choose Your League</h1>
          <p className="hero-sub">Select a league to start your tactical session.</p>
        </div>

        <div className="league-grid">
          <a
            href="#"
            className="league-card"
            onClick={(e) => { e.preventDefault(); navigate('/clubs?league=PL'); }}
          >
            <div className="logo-badge">
              <img
                className="league-logo"
                src="https://upload.wikimedia.org/wikipedia/en/thumb/f/f2/Premier_League_Logo.svg/200px-Premier_League_Logo.svg.png"
                alt="Premier League"
              />
            </div>
            <div className="league-card-name">Premier League</div>
            <div className="league-card-sub">England</div>
            <div className="league-card-sub">Season 24/25</div>
          </a>

          <a
            href="#"
            className="league-card"
            onClick={(e) => { e.preventDefault(); navigate('/clubs?league=SA'); }}
          >
            <div className="logo-badge">
              <img
                className="league-logo"
                src="https://upload.wikimedia.org/wikipedia/en/thumb/a/ab/Serie_A_ENILIVE_logo.svg/960px-Serie_A_ENILIVE_logo.svg.png"
                alt="Serie A"
              />
            </div>
            <div className="league-card-name">Serie A</div>
            <div className="league-card-sub">Italy</div>
            <div className="league-card-sub">Season 24/25</div>
          </a>
        </div>
      </main>
      <Footer />
    </>
  );
}
