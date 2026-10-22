import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade's rate on the sheet: a percent with two decimals. A grade with no rate is not listed. */
export type Rate = { grade: string; rate: string };

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

/** The sheet's rate for each grade that has one. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
  });
}

/** The rate for a grade, or undefined when the grade has none (or the rates are still loading). */
export function useRateFor(sheetId: string, grade: string): string | undefined {
  return useRates(sheetId).data?.find((r) => r.grade === grade)?.rate;
}

/** A two-decimal amount as whole cents, exactly. */
function toCents(amount: string): bigint {
  const [whole, frac = ''] = amount.trim().split('.');
  return BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
}

function fromCents(cents: bigint): string {
  return `${cents / 100n}.${String(cents % 100n).padStart(2, '0')}`;
}

/** Due on an amount: amount times rate, divided by 100, rounded half up to two decimals (exact,
 *  in integers); 'n/a' when there is no rate. */
export function dueOn(amount: string, rate: string | undefined): string {
  if (rate === undefined) return 'n/a';
  const scaled = toCents(amount) * toCents(rate); // in 1/10000 of a cent
  return fromCents((scaled + 5000n) / 10000n);
}

/** Two-decimal amounts added up exactly, skipping 'n/a'. */
export function sumDue(dues: string[]): string {
  return fromCents(dues.filter((d) => d !== 'n/a').reduce((total, d) => total + toCents(d), 0n));
}
