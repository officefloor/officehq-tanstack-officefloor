import { isOpen, type Claim } from './useClaims';

/** A date ("2026-03-01") moved on by a number of days, as YYYY-MM-DD. Worked in UTC so no time
 *  zone shifts it. */
export function addDays(date: string, days: number): string {
  const [y, m, d] = date.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10);
}

/** A claim's due-by: its date plus the sheet's terms. */
export const dueBy = (c: Claim, terms: number) => addDays(c.date, terms);

/** A claim is overdue when it is OPEN (or PENDING) and its due-by is before the sheet's today. */
export const isOverdue = (c: Claim, terms: number, today: string | undefined) =>
  isOpen(c) && today !== undefined && dueBy(c, terms) < today;
