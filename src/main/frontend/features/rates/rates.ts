import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts, type Claim } from '../claims/claims';

/** A grade's rate on the sheet: a percent with two decimals, in force from its from-date (yyyy-mm-dd, or
 *  null: from the beginning of time). A grade may have several, one per from-date; none when it has no rate. */
export type Rate = { grade: string; rate: string; from: string | null };

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

/** The sheet's rates, every grade's every from-date. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
  });
}

/** A grade's rate in force on a date (yyyy-mm-dd): the one whose from-date is the latest on or before
 *  it, a rate with no from-date applying from the beginning of time; undefined when none applies. */
export function rateOn(rates: Rate[], grade: string, date: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (!found || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found?.rate;
}

/** A grade's rate in force on a date, or undefined when none applies (or the rates are still loading). */
export function useRateFor(sheetId: string, grade: string, date: string): string | undefined {
  return rateOn(useRates(sheetId).data ?? [], grade, date);
}

/** A two-decimal amount as whole cents, exactly. */
function toCents(amount: string): bigint {
  const [whole, frac = ''] = amount.trim().split('.');
  return BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
}

function fromCents(cents: bigint): string {
  return `${cents / 100n}.${String(cents % 100n).padStart(2, '0')}`;
}

/** Due on an amount: amount times rate, divided by 100, rounded half to even to two decimals (exact,
 *  in integers); 'n/a' when there is no rate. */
export function dueOn(amount: string, rate: string | undefined): string {
  if (rate === undefined) return 'n/a';
  const scaled = toCents(amount) * toCents(rate); // in 1/10000 of a cent
  const sign = scaled < 0n ? -1n : 1n;
  const abs = scaled * sign;
  let cents = abs / 10000n;
  const rest = abs % 10000n;
  if (rest > 5000n || (rest === 5000n && cents % 2n === 1n)) cents += 1n;
  return fromCents(cents * sign);
}

/** Two-decimal amounts added up exactly, skipping 'n/a'. */
export function sumDue(dues: string[]): string {
  return fromCents(dues.filter((d) => d !== 'n/a').reduce((total, d) => total + toCents(d), 0n));
}

/** A person's due-total: the figures of their claims that count (each as shown on its row) added up;
 *  'n/a' when their grade has no rate today (rateToday undefined), unless a settled claim keeps a figure,
 *  then the figures there are added up. */
export function personDue(claims: Claim[], rateOnDate: (date: string) => string | undefined,
    rateToday: string | undefined): string {
  const counted = claims.filter(counts);
  if (rateToday === undefined && !counted.some((c) => c.status === 'SETTLED' && c.settledDue !== null)) return 'n/a';
  return sumDue(counted.map((c) => claimDue(c, rateOnDate(c.date))));
}

/** A claim's due: the one fixed when it was settled, else live on its amount at the rate. */
export function claimDue(claim: Claim, rate: string | undefined): string {
  return claim.settledDue ?? dueOn(claim.amount, rate);
}

