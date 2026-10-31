import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade's rate as the server sends it: `pct` is a percent with two decimals. */
export type Rate = { grade: string; pct: string };

export const ratesKey = (sheet: string) => ['sheet', sheet, 'rates'] as const;

/** The sheet's rates, one per grade that has one, re-read whenever the sheet is loaded. */
export function useRates(sheet: string) {
  return useQuery({
    queryKey: ratesKey(sheet),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheet)}/rates`),
    staleTime: 0,
    gcTime: 0,
  });
}

/** A percent the server accepts: 0.00 or more, with up to two decimals. */
export const PCT = /^\d{1,5}(\.\d{1,2})?$/;

/** The rate for each grade that has one; a grade missing here has no rate. */
export function rateByGrade(rates: Rate[]): Map<string, string> {
  return new Map(rates.map((r) => [r.grade, r.pct]));
}

/** "12.34" as a whole number of hundredths (1234n), exactly. */
export function hundredths(value: string): bigint {
  const [whole, frac = ''] = value.split('.');
  return BigInt(whole) * 100n + BigInt((frac + '00').slice(0, 2));
}

function format(cents: bigint): string {
  return `${cents / 100n}.${(cents % 100n).toString().padStart(2, '0')}`;
}

/**
 * What is due on `amount` (two decimals) at `pct` percent: amount times pct divided by 100,
 * rounded half up to two decimals, worked in whole numbers so it is exact. In cents, or null
 * when there is no rate.
 */
export function dueCents(amount: string, pct: string | undefined): bigint | null {
  if (pct === undefined) return null;
  // cents * hundredths-of-a-percent is in ten-thousandths of a cent.
  return (hundredths(amount) * hundredths(pct) + 5000n) / 10000n;
}

/** A due in cents as shown: two decimals, or n/a when there is no rate. */
export function showDue(cents: bigint | null): string {
  return cents === null ? 'n/a' : format(cents);
}

/** Dues added up, skipping the n/a ones. */
export function totalDue(dues: Array<bigint | null>): string {
  return format(dues.reduce<bigint>((sum, d) => (d === null ? sum : sum + d), 0n));
}
