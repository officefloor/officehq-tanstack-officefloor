import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A grade's rate as the store holds it: the percent with two decimals, from a date on (YYYY-MM-DD; null
 *  when it applies from the beginning of time). */
export type Rate = { grade: string; pct: string; from: string | null };

export const ratesQueryKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

export const ratesPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/rates`;

/** The sheet's rates, by grade, then by from-date (a rate with none first). */
export function useRates() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: ratesQueryKey(sheet ?? ''),
    queryFn: () => getJson<Rate[]>(ratesPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** The percent ("12.50") of a grade's rate in force on a date (YYYY-MM-DD): the one whose from-date is the
 *  latest on or before it, a rate with no from-date applying from the beginning of time — or undefined when
 *  no rate applies then. */
export function rateOn(rates: Rate[] | undefined, grade: string, date: string | undefined): string | undefined {
  if (date === undefined) return undefined;
  let found: Rate | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (!found || (found.from ?? '') <= (r.from ?? '')) found = r;
  }
  return found?.pct;
}

/** A two-decimal string ("12.50") in hundredths, exactly. */
const hundredths = (s: string) => BigInt(Math.round(Number(s) * 100));

/**
 * The due on an amount: amount times the percent, divided by 100, rounded half up (away from zero)
 * to two decimals — "n/a" when there is no percent. Computed in whole hundredths, so it is exact.
 */
export function dueOf(amount: string, pct: string | undefined): string {
  if (pct === undefined) return 'n/a';
  // hundredths of the amount times hundredths of the percent = the due in millionths of a percent;
  // dividing by 10000 gives cents.
  const product = hundredths(amount) * hundredths(pct);
  const negative = product < 0n;
  const magnitude = negative ? -product : product;
  const cents = (magnitude + 5000n) / 10000n;
  return formatCents(negative ? -cents : cents);
}

/** Whole cents as a two-decimal string. */
export function formatCents(cents: bigint): string {
  const negative = cents < 0n;
  const m = negative ? -cents : cents;
  return `${negative ? '-' : ''}${m / 100n}.${(m % 100n).toString().padStart(2, '0')}`;
}

/** Adds up two-decimal amounts, skipping "n/a". */
export function sumDue(dues: string[]): string {
  return formatCents(dues.filter((d) => d !== 'n/a').reduce((n, d) => n + hundredths(d), 0n));
}
