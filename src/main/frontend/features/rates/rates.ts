import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';

/** A grade's rate: `pct` is a percent with two decimals; `from` (yyyy-MM-dd) is the date it applies
 *  from, null for from the beginning of time. A grade may have several; one with no rate has none. */
export type Rate = { grade: string; pct: string; from: string | null };

/** Under the sheet's key, so loading the sheet again re-reads its rates. */
export const ratesKey = (sheet: string) => [...sheetKey(sheet), 'rates'] as const;

export function useRates(sheet: string) {
  return useQuery({
    queryKey: ratesKey(sheet),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheet)}/rates`),
    enabled: sheet !== '',
  });
}

/** The grade's percent in force on the date (yyyy-MM-dd): the rate with the latest from-date on or
 *  before it, or undefined when none of the grade's rates applies then. */
export function rateOf(rates: Rate[] | undefined, grade: string, on: string | undefined): string | undefined {
  if (on === undefined) return undefined;
  let best: Rate | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade || (r.from !== null && r.from > on)) continue;
    if (best === undefined || (best.from ?? '') < (r.from ?? '')) best = r;
  }
  return best?.pct;
}

export const NA = 'n/a';

/** Whole hundredths of a two-decimal number, read exactly from its text. */
function hundredths(value: string): bigint {
  const [whole, frac = ''] = value.trim().replace(/^-/, '').split('.');
  const n = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return value.trim().startsWith('-') ? -n : n;
}

/** Hundredths as text with two decimals. */
function format(cents: bigint): string {
  const neg = cents < 0n;
  const abs = neg ? -cents : cents;
  return `${neg ? '-' : ''}${abs / 100n}.${String(abs % 100n).padStart(2, '0')}`;
}

/** amount × pct ÷ 100, rounded half away from zero to two decimals; n/a when there is no rate. */
export function due(amount: string, pct: string | undefined): string {
  if (pct === undefined) return NA;
  // amount (hundredths) × pct (hundredths) is in units of 1/10000 cent-percent: ÷ 10000 gives cents.
  const product = hundredths(amount) * hundredths(pct);
  const abs = product < 0n ? -product : product;
  const cents = (abs + 5000n) / 10000n;
  return format(product < 0n ? -cents : cents);
}

/** The sum of these dues, skipping n/a; two decimals. */
export function dueTotal(dues: string[]): string {
  return format(dues.filter((d) => d !== NA).reduce((sum, d) => sum + hundredths(d), 0n));
}
