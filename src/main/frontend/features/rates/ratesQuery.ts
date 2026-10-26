import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';
import type { Grade } from '../people/peopleQuery';

/** `rate` is a percent with two decimals ("12.50"). A grade with no rate has no entry. */
export type Rate = { grade: Grade; rate: string };

/** Under the sheet's key, so re-loading the sheet re-reads its rates. */
export const ratesKey = (sheetId: string) => [...sheetKey(sheetId), 'rates'] as const;

export const ratesQuery = (sheetId: string) => ({
  queryKey: ratesKey(sheetId),
  queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
});

/** The grade's rate, or undefined when it has none. */
export function rateFor(rates: Rate[], grade: Grade | undefined): string | undefined {
  return rates.find((r) => r.grade === grade)?.rate;
}

/** A two-decimal string ("12.50") in whole hundredths, exactly. */
function hundredths(value: string): bigint {
  const [whole, frac = ''] = value.trim().split('.');
  return BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
}

function show(cents: bigint): string {
  return `${cents / 100n}.${(cents % 100n).toString().padStart(2, '0')}`;
}

/** Cents of `amount` times `rate` percent, rounded half up (away from zero; both are 0 or more). */
export function dueCents(amount: string, rate: string): bigint {
  return (hundredths(amount) * hundredths(rate) + 5000n) / 10000n;
}

/** `amount` times `rate` percent with two decimals, or n/a when there is no rate. */
export function due(amount: string, rate: string | undefined): string {
  return rate === undefined ? 'n/a' : show(dueCents(amount, rate));
}

/** Due-totals added up, skipping n/a. */
export function sumDue(dues: string[]): string {
  return show(dues.filter((d) => d !== 'n/a').reduce((total, d) => total + hundredths(d), 0n));
}
