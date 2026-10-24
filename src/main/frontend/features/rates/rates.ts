import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A grade's rate as the store holds it: the percent with two decimals. */
export type Rate = { grade: string; pct: string };

export const ratesQueryKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

export const ratesPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/rates`;

/** The sheet's rate for each grade that has one. */
export function useRates() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: ratesQueryKey(sheet ?? ''),
    queryFn: () => getJson<Rate[]>(ratesPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** The percent for a grade ("12.50"), or undefined when the grade has no rate. */
export function rateOf(rates: Rate[] | undefined, grade: string): string | undefined {
  return rates?.find((r) => r.grade === grade)?.pct;
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
