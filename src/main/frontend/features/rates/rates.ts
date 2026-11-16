import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts, type Claim } from '../claims/claims';
import { lateBoundaries, roundsHalfUp, type Late } from '../overdue/late';
import { gradeOn, type Person } from '../people/people';
import type { Dated } from '../settings/settings';

/** A grade's rate on the sheet: `rate` is a percent as text with two decimals (never a float), in force from
 *  `from` (yyyy-MM-dd; null: from the beginning of time) through `to` (yyyy-MM-dd; null: no end). */
export type Rate = { sheetId: string; grade: string; rate: string; from: string | null; to: string | null };

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'];

/** The sheet's rates, by grade then from-date. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
  });
}

/** The grade's rate in force on a date (yyyy-MM-dd): the one with the latest from-date on or before it, a rate
 *  with no from-date applying from the beginning of time; undefined when none applies, or that rate's to-date is
 *  before the date. */
export function rateFor(rates: Rate[] | undefined, grade: string, on: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade || (r.from !== null && r.from > on)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found === undefined || (found.to !== null && found.to < on) ? undefined : found.rate;
}

/** A two-decimal number ("12.34") in hundredths, exactly. */
function hundredths(value: string): bigint {
  const t = value.trim();
  const negative = t.startsWith('-');
  const [whole, frac = ''] = (negative ? t.slice(1) : t).split('.');
  const h = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -h : h;
}

/** Hundredths shown with two decimals. */
function shown(h: bigint): string {
  const sign = h < 0n ? '-' : '';
  const a = h < 0n ? -h : h;
  return `${sign}${a / 100n}.${String(a % 100n).padStart(2, '0')}`;
}

/** What is due on an amount at a rate: amount × rate / 100, rounded to two decimals (half to even, or half up when
 *  `halfUp`); 'n/a' when there is no rate. */
export function due(amount: string, rate: string | undefined, halfUp = false): string {
  if (rate === undefined) return 'n/a';
  return shown(percentOf(amount, rate, 1, halfUp));
}

/** `times` × value × pct / 100 in hundredths, rounded to two decimals: half to even, or half up (away from zero)
 *  when `halfUp`. */
function percentOf(value: string, pct: string, times: number, halfUp: boolean): bigint {
  const p = hundredths(value) * hundredths(pct) * BigInt(times); // in 1/10000 of a hundredth
  const a = p < 0n ? -p : p;
  const q = a / 10000n;
  const rem = a % 10000n;
  const r = rem > 5000n || (rem === 5000n && (halfUp || q % 2n === 1n)) ? q + 1n : q;
  return p < 0n ? -r : r;
}

/** A claim's due: the figure fixed when it was settled, otherwise the one its person's last statement bound it to (never while PENDING),
 *  otherwise its amount at the rate in force on its date. Given the sheet's late charge, an overdue claim's figure gains,
 *  for every full 30 days overdue, the late percent in force on that boundary (its due-by plus 30, 60, 90 ... days) of
 *  it, each rounded like the claim and added up (never bound by a statement; frozen in when settled). Last, the live
 *  due is at least the sheet's floor in force on its date (a correction has no floor). Both round by the sheet's
 *  policy date (see Late). */
export function claimDue(claim: Claim, rate: string | undefined, floor: Dated, late?: Late): string {
  if (claim.due !== null) return claim.due;
  const halfUp = roundsHalfUp(claim, late);
  const figure = claim.stated && claim.status !== 'PENDING' ? (claim.statedDue ?? 'n/a') : due(claim.amount, rate, halfUp);
  if (figure === 'n/a') return figure;
  const boundaries = late === undefined ? [] : lateBoundaries(claim, late);
  const charged = hundredths(figure) + boundaries.reduce((t, b) => t + percentOf(figure, late!.pct(b), 1, halfUp), 0n);
  const f = hundredths(floor(claim.date));
  return shown(charged < f && hundredths(claim.amount) >= 0n ? f : charged);
}

/** The sum of a person's figures: the figures of their claims that count (each at the rate, of the grade in force,
 *  on its date, with its late charge) added up; 'n/a' when their grade has no rate today and none of those claims has
 *  a figure. */
function personFigures(claims: Claim[], rates: Rate[] | undefined, person: Person, late: Late, floor: Dated): string {
  const counted = claims.filter(counts);
  const figures = counted.map((c) => claimDue(c, rateFor(rates, gradeOn(person, c.date), c.date), floor, late));
  if (rateFor(rates, gradeOn(person, late.today), late.today) === undefined && figures.every((f) => f === 'n/a')) return 'n/a';
  return sum(figures);
}

/** A person's settled-total and open-total: the figures (see personFigures) of their SETTLED claims that count
 *  (frozen when settled, corrections included), and of their OPEN and PENDING ones, each added up, skipping n/a.
 *  Together they make the sum of their figures, before the cap. */
export function personSplit(claims: Claim[], rates: Rate[] | undefined, person: Person, late: Late, floor: Dated): { settled: string; open: string } {
  const figure = (c: Claim) => claimDue(c, rateFor(rates, gradeOn(person, c.date), c.date), floor, late);
  const counted = claims.filter(counts);
  return {
    settled: sum(counted.filter((c) => c.status === 'SETTLED').map(figure)),
    open: sum(counted.filter((c) => c.status !== 'SETTLED').map(figure)),
  };
}

/** A person's due-total: the sum of their figures (see personFigures), at most the sheet's cap (null: none); 'n/a'
 *  is not capped. */
export function personDue(claims: Claim[], rates: Rate[] | undefined, person: Person, late: Late, floor: Dated, cap: string | null): string {
  const total = personFigures(claims, rates, person, late, floor);
  return total !== 'n/a' && cap !== null && hundredths(total) > hundredths(cap) ? shown(hundredths(cap)) : total;
}

/** A person's excess: how far the sum of their figures is above the sheet's cap; 0.00 when it is not (or with no
 *  cap, or an n/a due-total). */
export function personExcess(claims: Claim[], rates: Rate[] | undefined, person: Person, late: Late, floor: Dated, cap: string | null): string {
  const total = personFigures(claims, rates, person, late, floor);
  return total !== 'n/a' && cap !== null && hundredths(total) > hundredths(cap)
    ? shown(hundredths(total) - hundredths(cap))
    : '0.00';
}

/** Two-decimal values added up, skipping 'n/a'. */
export function sum(values: string[]): string {
  return shown(values.filter((v) => v !== 'n/a').reduce((s, v) => s + hundredths(v), 0n));
}

/** Two-decimal values added up, skipping 'n/a', leaving out the single highest and the single lowest when three
 *  or more are left. */
export function trimmedSum(values: string[]): string {
  const figures = values.filter((v) => v !== 'n/a').map(hundredths).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const kept = figures.length >= 3 ? figures.slice(1, -1) : figures;
  return shown(kept.reduce((s, v) => s + v, 0n));
}
