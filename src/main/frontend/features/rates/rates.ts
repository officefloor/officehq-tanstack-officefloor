import { useQuery } from '@tanstack/react-query';
import { useReadScope } from '../../query/live';
import type { Grade } from '../people/people';

/** A grade's rate: a percent with two decimals, in force from its from-date (yyyy-MM-dd; null when it
 *  applies from the beginning of time) through its to-date (null when it has no end). Its level is the sheet it is
 *  kept on: 0 this sheet's own, 1 its parent's, 2 its parent's parent, up to {@link RATE_LEVELS}. Its version counts
 *  the values set for its grade and from-date, this one (the newest, the one in force) included. A forward-only rate
 *  (saved with a from-date on or after the sheet's today) carries the claim sequence's value when it was saved (after;
 *  null for a rate that applies to every claim) and the version it replaced (before; null when none): a claim added
 *  before it uses that one instead (see {@link claimRateOf}). */
export type Rate = {
  grade: Grade;
  pct: string;
  from: string | null;
  to: string | null;
  level: number;
  version: number;
  after?: number | null;
  before?: Rate | null;
};

/** The most levels up a sheet's parents give it rates. */
export const RATE_LEVELS = 3;

export const ratesKey = (sheet: string) => ['rates', sheet] as const;

export const ratesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/rates`;

/** What a candidate rate for a grade would do: how many OPEN and PENDING claims would show a different due, and the
 *  sum of the differences (new due minus current due, two decimals; negative when lower). */
export type RatePreview = { count: number; diff: string };

export const ratePreviewPath = (sheet: string, grade: Grade, pct: string) =>
  `${ratesPath(sheet)}/preview?grade=${encodeURIComponent(grade)}&pct=${encodeURIComponent(pct)}`;

/** Every rate on the sheet, its own by grade then from-date (the one with none first), then its parents' by level;
 *  a grade with no rate is absent. */
export function useRates(sheet: string) {
  const read = useReadScope(ratesKey(sheet));
  return useQuery({
    queryKey: read.key,
    queryFn: () => read.get<Rate[]>(ratesPath(sheet)),
  });
}

/** The rate for a grade on a date (yyyy-MM-dd): the one whose from-date is the latest on or before it, or
 *  undefined when the date is before every rate the grade has, or after the to-date of that one (or the date
 *  is not yet known). When the sheet's own rates give none, the next level up is used. */
export function rateOf(rates: Rate[], grade: Grade | undefined, date: string | undefined): string | undefined {
  if (date === undefined || grade === undefined) return undefined;
  for (let level = 0; level <= RATE_LEVELS; level++) {
    const pct = rateAt(rates, grade, date, level);
    if (pct !== undefined) return pct;
  }
  return undefined;
}

/** The rates as a claim added at the sequence value sees them (null for one kept from before it was recorded): each
 *  forward-only rate saved after it gives way to the version it replaced, or is left out when it replaced none. */
export function seenBy(rates: Rate[], added: number | null | undefined): Rate[] {
  const seen: Rate[] = [];
  for (const rate of rates) {
    let r: Rate | null | undefined = rate;
    while (r != null && r.after != null && (added == null || added <= r.after)) r = r.before;
    if (r != null) seen.push(r);
  }
  return seen;
}

/** The rate for a grade on a claim's date, of the rates the claim sees (see {@link seenBy}). */
export function claimRateOf(rates: Rate[], grade: Grade | undefined, claim: { date: string; added?: number | null }): string | undefined {
  return rateOf(seenBy(rates, claim.added), grade, claim.date);
}

function rateAt(rates: Rate[], grade: Grade, date: string, level: number): string | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.level ?? 0) !== level || (r.from !== null && r.from > date)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  if (found?.to != null && found.to < date) return undefined;
  return found?.pct;
}

/** A two-decimal string as a whole number of hundredths. */
export function hundredths(value: string): bigint {
  const negative = value.startsWith('-');
  const [whole, frac = ''] = value.replace(/^-/, '').split('.');
  const cents = BigInt(whole) * 100n + BigInt(frac.padEnd(2, '0').slice(0, 2));
  return negative ? -cents : cents;
}

/** A whole number of hundredths as a two-decimal string. */
export function twoDecimals(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${String(abs % 100n).padStart(2, '0')}`;
}

/** An amount times a percent, divided by 100, in hundredths rounded half to even (1.025 -> 1.02,
 *  1.035 -> 1.04), or half up when `halfUp` (1.025 -> 1.03); undefined when there is no rate. Worked in whole
 *  numbers so no float error shows. */
export function dueCents(amount: string, pct: string | undefined, halfUp = false): bigint | undefined {
  if (pct === undefined) return undefined;
  const product = hundredths(amount) * hundredths(pct);
  // Rounded on the magnitude, so a negative amount (a correction) rounds as its positive would.
  const abs = product < 0n ? -product : product;
  let quotient = abs / 10000n;
  const remainder = abs % 10000n;
  if (remainder > 5000n || (remainder === 5000n && (halfUp || quotient % 2n === 1n))) quotient += 1n;
  return product < 0n ? -quotient : quotient;
}

/** A due as the screen shows it: two decimals, or n/a when there is no rate. */
export function showDue(cents: bigint | undefined): string {
  return cents === undefined ? 'n/a' : twoDecimals(cents);
}
