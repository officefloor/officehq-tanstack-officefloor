import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts, type Claim } from '../claims/claims';

/** A grade's rate on the sheet: `rate` is a percent as text with two decimals (never a float), in force from
 *  `from` (yyyy-MM-dd; null: from the beginning of time). */
export type Rate = { sheetId: string; grade: string; rate: string; from: string | null };

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'];

/** The sheet's rates, by grade then from-date. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
  });
}

/** The grade's rate in force on a date (yyyy-MM-dd): the one with the latest from-date on or before it, a rate
 *  with no from-date applying from the beginning of time; undefined when none applies. */
export function rateFor(rates: Rate[] | undefined, grade: string, on: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade || (r.from !== null && r.from > on)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found?.rate;
}

/** A two-decimal number ("12.34") in hundredths, exactly. */
function hundredths(value: string): bigint {
  const [whole, frac = ''] = value.trim().split('.');
  return BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
}

/** Hundredths shown with two decimals. */
function shown(h: bigint): string {
  const sign = h < 0n ? '-' : '';
  const a = h < 0n ? -h : h;
  return `${sign}${a / 100n}.${String(a % 100n).padStart(2, '0')}`;
}

/** What is due on an amount at a rate: amount × rate / 100, rounded half to even to two decimals; 'n/a' when
 *  there is no rate. */
export function due(amount: string, rate: string | undefined): string {
  if (rate === undefined) return 'n/a';
  const p = hundredths(amount) * hundredths(rate); // in 1/10000 of a hundredth
  const a = p < 0n ? -p : p;
  const q = a / 10000n;
  const rem = a % 10000n;
  const r = rem > 5000n || (rem === 5000n && q % 2n === 1n) ? q + 1n : q;
  return shown(p < 0n ? -r : r);
}

/** A claim's due: the figure fixed when it was settled, otherwise its amount at the rate in force on its date. */
export function claimDue(claim: Claim, rate: string | undefined): string {
  return claim.due ?? due(claim.amount, rate);
}

/** A person's due-total: the figures of their claims that count (each at the rate in force on its date) added
 *  up; 'n/a' when their grade has no rate today, unless a settled claim keeps a figure. */
export function personDue(claims: Claim[], rates: Rate[] | undefined, grade: string, today: string): string {
  const counted = claims.filter(counts);
  if (rateFor(rates, grade, today) === undefined && !counted.some((c) => c.due !== null)) return 'n/a';
  return sum(counted.map((c) => claimDue(c, rateFor(rates, grade, c.date))));
}

/** Two-decimal values added up, skipping 'n/a'. */
export function sum(values: string[]): string {
  return shown(values.filter((v) => v !== 'n/a').reduce((s, v) => s + hundredths(v), 0n));
}
