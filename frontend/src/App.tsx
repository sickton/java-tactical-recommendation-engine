import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LeagueSelect from './pages/LeagueSelect';
import ClubSelect from './pages/ClubSelect';
import Fixtures from './pages/Fixtures';
import Match from './pages/Match';
import Result from './pages/Result';
import ModeSelect from './pages/ModeSelect';
import QuerySelect from './pages/QuerySelect';
import Moments from './pages/Moments';
import MomentDetail from './pages/MomentDetail';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LeagueSelect />} />
        <Route path="/clubs" element={<ClubSelect />} />
        <Route path="/fixtures" element={<Fixtures />} />
        <Route path="/match" element={<Match />} />
        <Route path="/result" element={<Result />} />
        <Route path="/mode" element={<ModeSelect />} />
        <Route path="/query" element={<QuerySelect />} />
        <Route path="/moments" element={<Moments />} />
        <Route path="/moment" element={<MomentDetail />} />
      </Routes>
    </BrowserRouter>
  );
}
