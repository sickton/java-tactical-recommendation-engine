import { useEffect } from 'react';

export function useLeagueTheme(league: string | null) {
  useEffect(() => {
    document.body.classList.remove('league-pl', 'league-sa');
    if (league) {
      document.body.classList.add(`league-${league.toLowerCase()}`);
      localStorage.setItem('jgaffer_league', league);
    }
  }, [league]);
}
