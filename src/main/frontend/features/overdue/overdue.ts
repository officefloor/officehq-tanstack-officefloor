import type { Claim } from '../claims/claims';
import { claimDue, rateFor, sum, type Rate } from '../rates/rates';

/** A claim's due-by (yyyy-MM-dd): its date plus the sheet's terms in days. */
export function dueBy(claim: Claim, terms: number): string {
  const [y, m, d] = claim.date.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d + terms)).toISOString().slice(0, 10);
}

/** Whether a claim is overdue: it is OPEN and its due-by is before the sheet's today. */
export function isOverdue(claim: Claim, terms: number, today: string): boolean {
  return claim.status === 'OPEN' && today !== '' && dueBy(claim, terms) < today;
}

/** The overdue count and overdue-total (their dues added up, skipping n/a) of the given claims that count. */
export function overdue(
  claims: Claim[],
  rates: Rate[] | undefined,
  grade: string,
  terms: number,
  today: string,
  floor: string,
): { count: number; total: string } {
  const late = claims.filter((c) => isOverdue(c, terms, today));
  return { count: late.length, total: sum(late.map((c) => claimDue(c, rateFor(rates, grade, c.date), floor))) };
}
