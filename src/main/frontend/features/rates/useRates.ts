import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade's rate as the server sends it: `pct` is a percent with two decimals, `from` the date it
 *  is in force from (yyyy-mm-dd), or null when it applies from the beginning of time. */
export type Rate = { grade: string; pct: string; from: string | null };

export const ratesKey = (sheet: string) => ['sheet', sheet, 'rates'] as const;

/** The sheet's rates, by grade then from-date (the one with none first), re-read whenever the sheet is loaded. */
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

/**
 * The grade's rate in force on `date` (yyyy-mm-dd): the one whose from-date is the latest on or
 * before it, a rate with no from-date applying from the beginning of time. Undefined when none does.
 */
export function rateOn(rates: Rate[], grade: string, date: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found?.pct;
}

/** "12.34" as a whole number of hundredths (1234n), exactly. */
export function hundredths(value: string): bigint {
  const negative = value.startsWith('-');
  const [whole, frac = ''] = (negative ? value.slice(1) : value).split('.');
  const cents = BigInt(whole) * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -cents : cents;
}

function format(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${(abs % 100n).toString().padStart(2, '0')}`;
}

/**
 * What is due on `amount` (two decimals) at `pct` percent: amount times pct divided by 100,
 * rounded to two decimals (half up with `halfUp`, else half to even), worked in whole numbers so it
 * is exact. In cents, or null when there is no rate.
 */
export function dueCents(amount: string, pct: string | undefined, halfUp = false): bigint | null {
  if (pct === undefined) return null;
  // cents * hundredths-of-a-percent is in ten-thousandths of a cent.
  // Rounded on its size, so a negative amount (a correction) rounds like a positive one.
  const product = hundredths(amount) * hundredths(pct);
  const abs = product < 0n ? -product : product;
  const rounded = roundTenThousandths(abs, halfUp);
  return product < 0n ? -rounded : rounded;
}

/** A size in ten-thousandths of a cent, rounded to whole cents: half up with `halfUp`, else half to even. */
export function roundTenThousandths(abs: bigint, halfUp: boolean): bigint {
  const whole = abs / 10000n;
  const rest = abs % 10000n;
  return rest > 5000n || (rest === 5000n && (halfUp || whole % 2n === 1n)) ? whole + 1n : whole;
}

/** A due in cents as shown: two decimals, or n/a when there is no rate. */
export function showDue(cents: bigint | null): string {
  return cents === null ? 'n/a' : format(cents);
}

/** Dues added up, skipping the n/a ones. */
export function totalDue(dues: Array<bigint | null>): string {
  return format(dues.reduce<bigint>((sum, d) => (d === null ? sum : sum + d), 0n));
}
