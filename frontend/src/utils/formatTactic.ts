/** Converts tactic enum strings to readable Title Case.
 *  e.g. HIGH_PRESS → "High Press", GEGENPRESSING → "Gegenpressing"
 */
export function formatTactic(tactic: string): string {
  return tactic
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, c => c.toUpperCase());
}

/** Plain-English labels for non-football audiences.
 *  Used on the Match page tactic picker and guide panel.
 *  Falls back to formatTactic() if the key is not in the map.
 */
const PLAIN_LABELS: Record<string, string> = {
  GEGENPRESSING:  'Swarm them the moment they get the ball',
  HIGH_PRESS:     'Get in their faces and force a mistake',
  TIKI_TAKA:      'Keep the ball and frustrate the opponent',
  CONTROL:        'Hold your shape and give nothing away',
  COUNTER_ATTACK: 'Let them attack — then punish the space they leave',
  DIRECT_PLAY:    'Get the ball to your striker as fast as possible',
  LOW_BLOCK:      'Leave them no space — defend everything',
};

export function plainTactic(tactic: string): string {
  return PLAIN_LABELS[tactic] ?? formatTactic(tactic);
}
