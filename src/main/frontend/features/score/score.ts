/**
 * A person's score: their settled figures (in cents) divided by 100, minus 5 for every overdue claim of theirs that
 * counts, rounded half to even to a whole number. Worked in whole cents so it is exact: (cents − 50000 × overdue) / 10000.
 */
export function score(settled: string, overdueCount: number): string {
  const value = Math.round(Number(settled) * 100) - 50000 * overdueCount;
  const whole = Math.floor(Math.abs(value) / 10000);
  const rest = Math.abs(value) - whole * 10000;
  const up = rest > 5000 || (rest === 5000 && whole % 2 === 1);
  const result = Math.sign(value) * (whole + (up ? 1 : 0));
  return String(result === 0 ? 0 : result);
}
