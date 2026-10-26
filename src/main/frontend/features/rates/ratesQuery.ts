import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';
import type { Claim } from '../claims/claimsQuery';
import type { Grade } from '../people/peopleQuery';

/**
 * `rate` is a percent with two decimals ("12.50"), in force from `from` (yyyy-mm-dd; null: from the
 * beginning of time). A grade may have several, one per from-date; a grade with no rate has no entry.
 */
export type Rate = { grade: Grade; rate: string; from: string | null };

/** Under the sheet's key, so re-loading the sheet re-reads its rates. */
export const ratesKey = (sheetId: string) => [...sheetKey(sheetId), 'rates'] as const;

export const ratesQuery = (sheetId: string) => ({
  queryKey: ratesKey(sheetId),
  queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
});

/** The grade's rate on the date (yyyy-mm-dd): the one with the latest from-date on or before it, or undefined when none applies. */
export function rateFor(rates: Rate[], grade: Grade | undefined, date: string): string | undefined {
  // No from-date sorts as '', before every date.
  const applying = rates.filter((r) => r.grade === grade && (r.from ?? '') <= date);
  return applying.reduce<Rate | undefined>((best, r) => (!best || (r.from ?? '') > (best.from ?? '') ? r : best), undefined)?.rate;
}

/** A two-decimal string ("12.50") in whole hundredths, exactly. */
function hundredths(value: string): bigint {
  const [whole, frac = ''] = value.trim().split('.');
  return BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
}

function show(cents: bigint): string {
  return `${cents / 100n}.${(cents % 100n).toString().padStart(2, '0')}`;
}

/** Cents of `amount` times `rate` percent, rounded half to even (both are 0 or more). */
export function dueCents(amount: string, rate: string): bigint {
  const product = hundredths(amount) * hundredths(rate);
  const cents = product / 10000n;
  const rest = product % 10000n;
  return rest > 5000n || (rest === 5000n && cents % 2n === 1n) ? cents + 1n : cents;
}

/** `amount` times `rate` percent with two decimals, or n/a when there is no rate. */
export function due(amount: string, rate: string | undefined): string {
  return rate === undefined ? 'n/a' : show(dueCents(amount, rate));
}

/** Due figures added up, skipping n/a. */
export function sumDue(dues: string[]): string {
  return show(dues.filter((d) => d !== 'n/a').reduce((total, d) => total + hundredths(d), 0n));
}

/** What a claim's late charge depends on: the sheet's today (yyyy-mm-dd), its terms in days and its late percent (two decimals). */
export type Late = { today: string; terms: number; pct: string };

/** The claim's due-by (yyyy-mm-dd): its date plus the sheet's terms in days. */
export function dueBy(claim: Claim, terms: number): string {
  const d = new Date(`${claim.date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + terms);
  return d.toISOString().slice(0, 10);
}

/** An OPEN claim whose due-by is before the sheet's today. */
export const isOverdue = (claim: Claim, terms: number, today: string) =>
  claim.status === 'OPEN' && today !== '' && dueBy(claim, terms) < today;

/** The full 30 days the claim has been overdue on the sheet's today (0 when it is not overdue). */
function lateMonths(claim: Claim, { today, terms }: Late): bigint {
  if (!isOverdue(claim, terms, today)) return 0n;
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${dueBy(claim, terms)}T00:00:00Z`)) / 86_400_000;
  return BigInt(Math.floor(days / 30));
}

/**
 * A claim's figure: a settled claim's frozen due (its late charge frozen in), otherwise the figure its
 * person's last statement bound it to, otherwise its amount times its person's grade's rate on the
 * claim's date, raised to the sheet's `floor` (two decimals). An overdue claim's late charge — the late
 * percent of that figure for every full 30 days overdue, half to even — is added on top: a statement
 * never binds it, so it follows the sheet's today.
 */
export function figure(claim: Claim, rates: Rate[], grade: Grade | undefined, floor: string, late: Late): string {
  if (claim.settledDue !== null) return claim.settledDue;
  const bound = claim.stated ? (claim.statedDue ?? 'n/a') : floored(due(claim.amount, rateFor(rates, grade, claim.date)), floor);
  const months = lateMonths(claim, late);
  if (bound === 'n/a' || months === 0n) return bound;
  return show(hundredths(bound) + dueCents(show(hundredths(bound) * months), late.pct));
}

function floored(live: string, floor: string): string {
  return live === 'n/a' || hundredths(live) >= hundredths(floor) ? live : show(hundredths(floor));
}
