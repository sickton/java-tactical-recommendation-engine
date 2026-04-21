import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LeagueSelect from './pages/LeagueSelect';
import ClubSelect from './pages/ClubSelect';
import ModeSelect from './pages/ModeSelect';
import QuerySelect from './pages/QuerySelect';
import Moments from './pages/Moments';
import MomentDetail from './pages/MomentDetail';
import Puzzle from './pages/Puzzle';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LeagueSelect />} />
        <Route path="/clubs" element={<ClubSelect />} />
        <Route path="/mode" element={<ModeSelect />} />
        <Route path="/query" element={<QuerySelect />} />
        <Route path="/moments" element={<Moments />} />
        <Route path="/moment" element={<MomentDetail />} />
        <Route path="/puzzle" element={<Puzzle />} />
      </Routes>
    </BrowserRouter>
  );
}
