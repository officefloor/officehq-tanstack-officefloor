import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Grade } from '../people/people';
import { counts } from '../claims/claims';
import type { Claim } from '../claims/claims';

/**
 * A grade's rate: a percent with two decimals, from a date (yyyy-MM-dd; '' when it applies from the
 * beginning of time).
 */
export type Rate = { grade: Grade; pct: string; from: string };

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

/** The sheet's rates, by grade then from-date (none first). */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ rates: Rate[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`)).rates,
  });
}

/**
 * A grade's rate in force on a date (yyyy-MM-dd): the one whose from-date is the latest on or
 * before it. Null when the date is before every rate the grade has (or it has none).
 */
export function rateOn(rates: Rate[], grade: Grade, date: string): string | null {
  let best: Rate | null = null;
  for (const r of rates) {
    if (r.grade !== grade || r.from > date) continue;
    if (best === null || r.from > best.from) best = r;
  }
  return best?.pct ?? null;
}

/** The sheet's rate for a grade on a date, or null when none is in force (or the rates are not read yet). */
export function useGradeRate(sheetId: string, grade: Grade, date: string): string | null {
  return rateOn(useRates(sheetId).data ?? [], grade, date);
}

/** A percent as a rate takes it: 0.00 or more, with up to two decimals. */
export const PCT = /^\d+(\.\d{1,2})?$/;

export const NA = 'n/a';

/** A two-decimal number as a whole count of hundredths. */
function hundredths(value: string): bigint {
  const [whole, frac = ''] = value.trim().split('.');
  return BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
}

function showCents(cents: bigint): string {
  return `${cents / 100n}.${(cents % 100n).toString().padStart(2, '0')}`;
}

/**
 * An amount times a rate, divided by 100, rounded half to even to two decimals, raised to the
 * sheet's `floor` when below it; n/a with no rate (no floor applies to n/a).
 * Worked in whole numbers so no floating-point error creeps in (amounts and rates are never
 * negative).
 */
export function due(amount: string, pct: string | null, floor: string): string {
  if (pct === null) return NA;
  const rounded = percentOf(amount, pct, 1n);
  const least = hundredths(floor);
  return showCents(rounded < least ? least : rounded);
}

/** `times` × a percent of a two-decimal figure, in cents, rounded half to even. */
function percentOf(figure: string, pct: string, times: bigint): bigint {
  // cents × basis points is the due in millionths of a unit: 10000 of them to the cent.
  const x = hundredths(figure) * hundredths(pct) * times;
  const cents = x / 10000n;
  const rest = x % 10000n;
  return rest > 5000n || (rest === 5000n && cents % 2n === 1n) ? cents + 1n : cents;
}

/**
 * What the late charge needs: the sheet's late percent (two decimals), its terms (days) and its
 * today (yyyy-MM-dd; '' until read).
 */
export type Late = { pct: string; terms: number; today: string };

/** Whole days from one date (yyyy-MM-dd) to another, worked in UTC so no zone shifts it. */
function daysBetween(from: string, to: string): number {
  const utc = (date: string) => {
    const [y, m, d] = date.split('-').map(Number);
    return Date.UTC(y, m - 1, d);
  };
  return Math.round((utc(to) - utc(from)) / 86_400_000);
}

/**
 * A claim's late charge on its `figure`: for an OPEN claim past its due-by (its date plus the
 * terms) on the sheet's today, the late percent of the figure for every full 30 days it is overdue,
 * rounded half to even; else nothing. A statement never binds it: it follows the sheet's today.
 */
function lateCharge(claim: Claim, figure: string, late: Late): bigint {
  if (claim.status !== 'OPEN' || figure === NA || late.today === '') return 0n;
  const overdue = daysBetween(claim.date, late.today) - late.terms;
  const periods = overdue > 0 ? Math.floor(overdue / 30) : 0;
  return periods === 0 ? 0n : percentOf(figure, late.pct, BigInt(periods));
}

/** Due values added up, skipping n/a. */
export function sumDue(values: string[]): string {
  return showCents(values.filter((v) => v !== NA).reduce((n, v) => n + hundredths(v), 0n));
}

/**
 * A claim's due: the one fixed when it was settled (its late charge frozen in), else the figure its
 * person's last statement bound it to, else its amount at the rate in force on its date, at least
 * the sheet's `floor`; an overdue claim's late charge added on top.
 */
export function claimDue(claim: Claim, pct: string | null, floor: string, late: Late): string {
  if (claim.due !== '') return claim.due;
  const figure = claim.stated ? claim.statedDue : due(claim.amount, pct, floor);
  const charge = lateCharge(claim, figure, late);
  return charge === 0n ? figure : showCents(hundredths(figure) + charge);
}

/**
 * A person's due-total: the figures of their claims that count (none OPEN while they are `held`) (each at the rate in force on its
 * date), added up, skipping n/a. With no rate for their grade today it is n/a, unless a settled
 * claim that counts keeps a fixed figure (settled, or bound by a statement).
 */
export function personDueTotal(
  rates: Rate[],
  grade: Grade,
  today: string,
  claims: Claim[],
  held: boolean,
  floor: string,
  late: Late,
): string {
  const counted = claims.filter((c) => counts(c, held));
  if (rateOn(rates, grade, today) === null && !counted.some((c) => c.due !== '' || (c.stated && c.statedDue !== NA))) return NA;
  return sumDue(counted.map((c) => claimDue(c, rateOn(rates, grade, c.date), floor, late)));
}

