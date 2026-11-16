import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade's rate on the sheet: `rate` is a percent as text with two decimals (never a float). */
export type Rate = { sheetId: string; grade: string; rate: string };

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'];

/** The sheet's rate for each grade that has one. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
  });
}

/** The rate for a grade, or undefined when the grade has none. */
export function rateFor(rates: Rate[] | undefined, grade: string): string | undefined {
  return rates?.find((r) => r.grade === grade)?.rate;
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

/** What is due on an amount at a rate: amount × rate / 100, rounded half up (away from zero) to two
 *  decimals; 'n/a' when there is no rate. */
export function due(amount: string, rate: string | undefined): string {
  if (rate === undefined) return 'n/a';
  const p = hundredths(amount) * hundredths(rate); // in 1/10000 of a hundredth
  const a = p < 0n ? -p : p;
  const r = (a + 5000n) / 10000n;
  return shown(p < 0n ? -r : r);
}

/** Two-decimal values added up, skipping 'n/a'. */
export function sum(values: string[]): string {
  return shown(values.filter((v) => v !== 'n/a').reduce((s, v) => s + hundredths(v), 0n));
}
