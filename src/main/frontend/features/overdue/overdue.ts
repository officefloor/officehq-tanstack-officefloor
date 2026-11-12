import type { Claim } from '../claims/claims';
import { type Rate, claimDue, dueTotal } from '../rates/rates';

/** The terms a sheet has until they are set: 14 days. */
export const DEFAULT_TERMS = '14';

/** The claim's due-by (yyyy-MM-dd): its date plus the terms (whole days) in force. */
export function dueBy(claim: Claim, terms: string | undefined): string {
  const d = new Date(`${claim.date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + Number(terms ?? DEFAULT_TERMS));
  return d.toISOString().slice(0, 10);
}

/** Whether the claim is overdue: it is OPEN and its due-by is before the sheet's today. */
export function overdue(claim: Claim, terms: string | undefined, today: string | undefined): boolean {
  return claim.status === 'OPEN' && today !== undefined && dueBy(claim, terms) < today;
}

/** A person's overdue claims among these (their claims that count): how many, and the sum of their
 *  figures at the person's grade, skipping n/a. */
export function overdueFigures(
  claims: Claim[],
  rates: Rate[] | undefined,
  grade: string,
  floor: string | undefined,
  terms: string | undefined,
  today: string | undefined,
): { count: number; total: string } {
  const late = claims.filter((c) => overdue(c, terms, today));
  return { count: late.length, total: dueTotal(late.map((c) => claimDue(c, rates, grade, floor))) };
}
