import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade's rate: a percent with two decimals. */
export type Rate = { grade: string; pct: string };

export function ratesKey(sheetId: string) {
  return ['sheet', sheetId, 'rates'];
}

/** The sheet's rates, by grade; a grade with no rate is not listed. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ rates: Rate[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`)).rates,
  });
}

/** The rate (a two-decimal percent) for a grade, or undefined when the grade has none. */
export function rateFor(rates: Rate[] | undefined, grade: string | undefined): string | undefined {
  return (rates ?? []).find((r) => r.grade === grade)?.pct;
}

/** A two-decimal number as a whole count of hundredths ("12.50" -> 1250). */
function hundredths(v: string): bigint {
  const [whole, frac = ''] = v.trim().split('.');
  return BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
}

/** A due: an amount (two decimals) times a rate (a two-decimal percent), divided by 100, rounded
 *  half up to two decimals; 'n/a' when there is no rate. Worked in whole numbers, so exact. */
export function due(amount: string, pct: string | undefined): string {
  if (pct === undefined) return 'n/a';
  const product = hundredths(amount) * hundredths(pct);       // in 1/1_000_000 of a unit
  const cents = (product + 5000n) / 10000n;                    // amounts are never negative
  return `${cents / 100n}.${(cents % 100n).toString().padStart(2, '0')}`;
}

/** Dues added up, skipping 'n/a'. */
export function sumDues(dues: string[]): string {
  const cents = dues.filter((d) => d !== 'n/a').reduce((sum, d) => sum + hundredths(d), 0n);
  return `${cents / 100n}.${(cents % 100n).toString().padStart(2, '0')}`;
}
