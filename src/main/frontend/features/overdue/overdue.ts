import { counts } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { claimDue, total } from '../rates/rates';
import type { Rate } from '../rates/rates';

/** A claim's due-by (yyyy-MM-dd): its date plus the terms in days. */
export function dueBy(date: string, terms: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + terms);
  return d.toISOString().slice(0, 10);
}

/** Whether a claim is overdue: it is OPEN and its due-by is before the sheet's today. */
export function overdue(claim: Claim, terms: number, today: string): boolean {
  return claim.status === 'OPEN' && today !== '' && dueBy(claim.date, terms) < today;
}

/**
 * A person's overdue claims that count (none of their open ones while they are held): how many, and
 * their figures added up (skipping n/a).
 */
export function personOverdue(
  claims: Claim[],
  rates: Rate[],
  grade: string,
  held: boolean,
  floor: string,
  terms: number,
  today: string,
): { count: number; total: string } {
  const late = claims.filter((c) => counts(c, held) && overdue(c, terms, today));
  return { count: late.length, total: total(late.map((c) => claimDue(c, rates, grade, floor))) };
}
