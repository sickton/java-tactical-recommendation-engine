/** Converts tactic enum strings to readable Title Case.
 *  e.g. HIGH_PRESS → "High Press", GEGENPRESSING → "Gegenpressing"
 */
export function formatTactic(tactic: string): string {
  return tactic
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, c => c.toUpperCase());
}
