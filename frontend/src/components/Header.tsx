import { Link } from 'react-router-dom';

export default function Header() {
  return (
    <header>
      <div className="header-bar">
        <Link to="/" className="logo-group">
          <span className="logo-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="1" y="4" width="26" height="20" rx="2" stroke="#4ade80" strokeWidth="1.6" fill="none"/>
              <line x1="14" y1="4" x2="14" y2="24" stroke="#4ade80" strokeWidth="1.2"/>
              <circle cx="14" cy="14" r="3.5" stroke="#4ade80" strokeWidth="1.2" fill="none"/>
              <rect x="1" y="10" width="4" height="8" stroke="#4ade80" strokeWidth="1.1" fill="none"/>
              <rect x="23" y="10" width="4" height="8" stroke="#4ade80" strokeWidth="1.1" fill="none"/>
            </svg>
          </span>
          <span className="logo-text">JGaffer</span>
        </Link>
        <span className="header-divider"></span>
        <span className="header-sub">Tactical Recommendation Engine</span>
        <div className="header-right">
          <img
            className="header-league-badge header-league-badge-pl"
            src="https://upload.wikimedia.org/wikipedia/en/thumb/f/f2/Premier_League_Logo.svg/200px-Premier_League_Logo.svg.png"
            alt="Premier League"
          />
          <img
            className="header-league-badge header-league-badge-sa"
            src="https://upload.wikimedia.org/wikipedia/en/thumb/a/ab/Serie_A_ENILIVE_logo.svg/960px-Serie_A_ENILIVE_logo.svg.png"
            alt="Serie A"
          />
        </div>
      </div>
    </header>
  );
}
