import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { gradeOn } from '../people/people';
import type { Grade, GradeFrom } from '../people/people';
import { counts, isCorrection } from '../claims/claims';
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

/** A two-decimal number (negative for a correction) as a whole count of hundredths. */
function hundredths(value: string): bigint {
  const trimmed = value.trim();
  const negative = trimmed.startsWith('-');
  const [whole, frac = ''] = (negative ? trimmed.slice(1) : trimmed).split('.');
  const cents = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -cents : cents;
}

function showCents(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${(abs % 100n).toString().padStart(2, '0')}`;
}

/**
 * An amount times a rate, divided by 100, rounded to two decimals (half up when `halfUp`, else half
 * to even), raised to the sheet's `floor` when below it; n/a with no rate (no floor applies to n/a).
 * Worked in whole numbers so no floating-point error creeps in. A correction (a negative amount)
 * takes no floor (`floor` null).
 */
export function due(amount: string, pct: string | null, floor: string | null, halfUp: boolean): string {
  if (pct === null) return NA;
  const rounded = percentOf(amount, pct, 1n, halfUp);
  if (floor === null) return showCents(rounded);
  const least = hundredths(floor);
  return showCents(rounded < least ? least : rounded);
}

/**
 * `times` × a percent of a two-decimal figure, in cents, rounded half up when `halfUp`, else half
 * to even (either symmetric about 0).
 */
function percentOf(figure: string, pct: string, times: bigint, halfUp: boolean): bigint {
  // cents × basis points is the due in millionths of a unit: 10000 of them to the cent.
  const signed = hundredths(figure) * hundredths(pct) * times;
  const x = signed < 0n ? -signed : signed;
  const cents = x / 10000n;
  const rest = x % 10000n;
  const rounded = rest > 5000n || (rest === 5000n && (halfUp || cents % 2n === 1n)) ? cents + 1n : cents;
  return signed < 0n ? -rounded : rounded;
}

/**
 * What the late charge and a claim's rounding need: the sheet's late percent (two decimals), its
 * terms (days), its today (yyyy-MM-dd; '' until read) and its policy date (yyyy-MM-dd; '' for its
 * today).
 */
export type Late = { pct: string; terms: number; today: string; policyDate: string };

/**
 * Whether a claim's live figures round half up: it rounds by a date (its own, or for a correction
 * the date of the claim it corrects) before the sheet's policy date (its today when blank); on or
 * after it they round half to even.
 */
export function roundsHalfUp(claim: Claim, late: Late): boolean {
  const policy = late.policyDate !== '' ? late.policyDate : late.today;
  return (claim.roundDate || claim.date) < policy;
}

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
 * rounded as the claim rounds; else nothing. A statement never binds it: it follows the sheet's today.
 */
function lateCharge(claim: Claim, figure: string, late: Late): bigint {
  if (claim.status !== 'OPEN' || figure === NA || late.today === '' || isCorrection(claim)) return 0n;
  const overdue = daysBetween(claim.date, late.today) - late.terms;
  const periods = overdue > 0 ? Math.floor(overdue / 30) : 0;
  return periods === 0 ? 0n : percentOf(figure, late.pct, BigInt(periods), roundsHalfUp(claim, late));
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
  const figure = claim.stated ? claim.statedDue : due(claim.amount, pct, isCorrection(claim) ? null : floor, roundsHalfUp(claim, late));
  const charge = lateCharge(claim, figure, late);
  return charge === 0n ? figure : showCents(hundredths(figure) + charge);
}

/**
 * A person's due-total: the figures of their claims that count (none OPEN while they are `held`) (each at the rate in force on its
 * date for their grade in force on its date), added up, skipping n/a. With no rate for their grade
 * today it is n/a, unless a claim that counts keeps a fixed figure (settled, or bound by a
 * statement) or has a figure under another grade, in force on its date.
 */
export function personDueTotal(
  rates: Rate[],
  grades: GradeFrom[],
  today: string,
  claims: Claim[],
  held: boolean,
  floor: string,
  late: Late,
): string {
  const counted = claims.filter((c) => counts(c, held));
  const grade = gradeOn(grades, today);
  const kept = (c: Claim) => c.due !== '' || (c.stated && c.statedDue !== NA)
    || (gradeOn(grades, c.date) !== grade && rateOn(rates, gradeOn(grades, c.date), c.date) !== null);
  if (rateOn(rates, grade, today) === null && !counted.some(kept)) return NA;
  return sumDue(counted.map((c) => claimDue(c, rateOn(rates, gradeOn(grades, c.date), c.date), floor, late)));
}

