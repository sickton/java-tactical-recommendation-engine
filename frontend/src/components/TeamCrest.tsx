import { CRESTS } from '../constants/crests';

interface Props {
  teamName: string;
  className?: string;
  size?: number;
}

export default function TeamCrest({ teamName, className = 'club-crest', size }: Props) {
  const src = CRESTS[teamName] ?? '';
  if (!src) return null;
  return (
    <img
      src={src}
      alt={teamName}
      className={className}
      style={size ? { width: size, height: size } : undefined}
      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
    />
  );
}
