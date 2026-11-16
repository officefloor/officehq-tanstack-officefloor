import type { Claim } from '../claims/claims';

/** What the late charge on a sheet depends on: its terms (days), its today (yyyy-MM-dd, '' while it loads) and its
 *  late percent (two decimals). */
export type Late = { terms: number; today: string; pct: string };

/** Days since 1970-01-01 of a yyyy-MM-dd date. */
function day(date: string): number {
  const [y, m, d] = date.split('-').map(Number);
  return Date.UTC(y, m - 1, d) / 86_400_000;
}

/** A claim's due-by (yyyy-MM-dd): its date plus the sheet's terms in days. */
export function dueBy(claim: Claim, terms: number): string {
  const [y, m, d] = claim.date.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d + terms)).toISOString().slice(0, 10);
}

/** Whether a claim is overdue: it is OPEN and its due-by is before the sheet's today. */
export function isOverdue(claim: Claim, terms: number, today: string): boolean {
  return claim.status === 'OPEN' && today !== '' && dueBy(claim, terms) < today;
}

/** How many full 30 days an overdue claim is overdue (0 when it is not overdue, its due is fixed by settling, or it is a
 *  correction, which bears no late charge). */
export function lateSteps(claim: Claim, late: Late): number {
  if (claim.due !== null || claim.amount.trim().startsWith('-') || !isOverdue(claim, late.terms, late.today)) return 0;
  return Math.floor((day(late.today) - day(dueBy(claim, late.terms))) / 30);
}
