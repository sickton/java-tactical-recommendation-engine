import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LeagueSelect from './pages/LeagueSelect';
import ClubSelect from './pages/ClubSelect';
import Fixtures from './pages/Fixtures';
import Match from './pages/Match';
import Result from './pages/Result';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LeagueSelect />} />
        <Route path="/clubs" element={<ClubSelect />} />
        <Route path="/fixtures" element={<Fixtures />} />
        <Route path="/match" element={<Match />} />
        <Route path="/result" element={<Result />} />
      </Routes>
    </BrowserRouter>
  );
}
