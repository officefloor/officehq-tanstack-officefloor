import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts, type Claim } from '../claims/claims';
import { lateSteps, roundsHalfUp, type Late } from '../overdue/late';
import { gradeOn, type Person } from '../people/people';

/** A grade's rate on the sheet: `rate` is a percent as text with two decimals (never a float), in force from
 *  `from` (yyyy-MM-dd; null: from the beginning of time). */
export type Rate = { sheetId: string; grade: string; rate: string; from: string | null };

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'];

/** The sheet's rates, by grade then from-date. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
  });
}

/** The grade's rate in force on a date (yyyy-MM-dd): the one with the latest from-date on or before it, a rate
 *  with no from-date applying from the beginning of time; undefined when none applies. */
export function rateFor(rates: Rate[] | undefined, grade: string, on: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade || (r.from !== null && r.from > on)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found?.rate;
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
 *  `halfUp`), and at least the floor (two decimals) unless it is a correction (a negative amount); 'n/a' when there
 *  is no rate. */
export function due(amount: string, rate: string | undefined, floor: string, halfUp = false): string {
  if (rate === undefined) return 'n/a';
  const d = percentOf(amount, rate, 1, halfUp);
  const f = hundredths(floor);
  return shown(d < f && hundredths(amount) >= 0n ? f : d);
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

/** A claim's due: the figure fixed when it was settled, otherwise the one its person's last statement bound it to,
 *  otherwise its amount at the rate in force on its date, at least the sheet's floor. Given the sheet's late
 *  charge, an overdue claim's figure gains the late percent of it for every full 30 days overdue (never bound by a
 *  statement; frozen in when settled). Both round by the sheet's policy date (see Late). */
export function claimDue(claim: Claim, rate: string | undefined, floor: string, late?: Late): string {
  if (claim.due !== null) return claim.due;
  const halfUp = roundsHalfUp(claim, late);
  const figure = claim.stated ? (claim.statedDue ?? 'n/a') : due(claim.amount, rate, floor, halfUp);
  if (late === undefined || figure === 'n/a') return figure;
  const steps = lateSteps(claim, late);
  return steps === 0 ? figure : shown(hundredths(figure) + percentOf(figure, late.pct, steps, halfUp));
}

/** A person's due-total: the figures of their claims that count (each at the rate, of the grade in force, on its
 *  date, with its late charge) added up; 'n/a' when their grade has no rate today and none of those claims has a
 *  figure. */
export function personDue(claims: Claim[], rates: Rate[] | undefined, person: Person, late: Late, floor: string): string {
  const counted = claims.filter(counts);
  const figures = counted.map((c) => claimDue(c, rateFor(rates, gradeOn(person, c.date), c.date), floor, late));
  if (rateFor(rates, gradeOn(person, late.today), late.today) === undefined && figures.every((f) => f === 'n/a')) return 'n/a';
  return sum(figures);
}

/** Two-decimal values added up, skipping 'n/a'. */
export function sum(values: string[]): string {
  return shown(values.filter((v) => v !== 'n/a').reduce((s, v) => s + hundredths(v), 0n));
}
