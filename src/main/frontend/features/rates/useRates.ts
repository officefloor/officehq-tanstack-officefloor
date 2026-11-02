import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade's rate: a percent with two decimals, in force from its from-date (yyyy-mm-dd; null when it
 *  applies from the beginning of time). */
export type Rate = { grade: string; pct: string; from: string | null };

export function ratesKey(sheetId: string) {
  return ['sheet', sheetId, 'rates'];
}

/** The sheet's rates, by grade then from-date (an undated rate first); a grade with no rate is not
 *  listed. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ rates: Rate[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`)).rates,
  });
}

/** The rate (a two-decimal percent) for a grade in force on a date (yyyy-mm-dd): the one with the
 *  latest from-date on or before it, an undated rate applying from the beginning of time; undefined
 *  when none applies. */
export function rateFor(rates: Rate[] | undefined, grade: string | undefined, on: string | undefined): string | undefined {
  if (on === undefined) return undefined;
  let found: Rate | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade || (r.from !== null && r.from > on)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found?.pct;
}

/** A two-decimal number as a whole count of hundredths ("12.50" -> 1250, "-0.30" -> -30). */
function hundredths(v: string): bigint {
  const t = v.trim();
  const negative = t.startsWith('-');
  const [whole, frac = ''] = (negative ? t.slice(1) : t).split('.');
  const cents = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -cents : cents;
}

/** A whole count of hundredths as a two-decimal number (-30 -> "-0.30"). */
function money(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${(abs % 100n).toString().padStart(2, '0')}`;
}

/** A due: an amount (two decimals) times a rate (a two-decimal percent), divided by 100, rounded
 *  half up (when `halfUp`) or half to even to two decimals, and at least the sheet's floor (two decimals; none while it is not
 *  loaded; a correction, a negative amount, has no floor); 'n/a' when there is no rate. Worked in
 *  whole numbers, so exact. */
export function due(amount: string, pct: string | undefined, floor: string | undefined, halfUp: boolean): string {
  if (pct === undefined) return 'n/a';
  let cents = percentOf(amount, pct, 1n, halfUp);
  if (floor !== undefined && !isCorrection(amount) && cents < hundredths(floor)) cents = hundredths(floor);
  return money(cents);
}

/** Whether an amount (two decimals) is a correction's: below zero. */
export function isCorrection(amount: string): boolean {
  return hundredths(amount) < 0n;
}

/** An amount (two decimals) times a rate (a two-decimal percent) times a whole number, divided by
 *  100, rounded half up (when `halfUp`, away from zero) or half to even, in hundredths (a negative amount rounds as its size does, then takes
 *  its sign back). */
function percentOf(amount: string, pct: string, times: bigint, halfUp: boolean): bigint {
  const signed = hundredths(amount) * hundredths(pct) * times; // in 1/1_000_000 of a unit
  const product = signed < 0n ? -signed : signed;
  const rest = product % 10000n;
  let cents = product / 10000n;
  if (rest > 5000n || (rest === 5000n && (halfUp || cents % 2n === 1n))) cents += 1n;
  return signed < 0n ? -cents : cents;
}

/** A figure (two decimals) with a late charge added on top: the late percent (two decimals) of the
 *  figure for each of `periods` full 30 days, rounded like a due; 'n/a' stays 'n/a'. */
export function withCharge(figure: string, latePct: string, periods: number, halfUp: boolean): string {
  if (figure === 'n/a' || periods <= 0) return figure;
  return money(hundredths(figure) + percentOf(figure, latePct, BigInt(periods), halfUp));
}

/** Dues added up, skipping 'n/a'. */
export function sumDues(dues: string[]): string {
  return money(dues.filter((d) => d !== 'n/a').reduce((sum, d) => sum + hundredths(d), 0n));
}
