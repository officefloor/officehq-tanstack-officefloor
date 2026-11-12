import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';
import { type Claim, counts, isBound, isCorrection } from '../claims/claims';
import { type Grades, gradeOn } from '../people/people';

/** A grade's rate: `pct` is a percent with two decimals; `from` (yyyy-MM-dd) is the date it applies
 *  from, null for from the beginning of time; `to` the date it applies through, null for no end. A
 *  grade may have several; one with no rate has none. */
export type Rate = { grade: string; pct: string; from: string | null; to: string | null };

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
 *  before it, or undefined when none of the grade's rates applies then, or that rate ended (its to-date) before it. */
export function rateOf(rates: Rate[] | undefined, grade: string, on: string | undefined): string | undefined {
  if (on === undefined) return undefined;
  let best: Rate | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade || (r.from !== null && r.from > on)) continue;
    if (best === undefined || (best.from ?? '') < (r.from ?? '')) best = r;
  }
  return best === undefined || (best.to !== null && best.to < on) ? undefined : best.pct;
}

/** Whether the grade's rate in force on the date ends before `today`: its to-date, or the day before the
 *  grade's next rate starts, whichever is first, is before it. */
export function endsBefore(rates: Rate[] | undefined, grade: string, on: string, today: string): boolean {
  let best: Rate | undefined;
  let next: string | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade) continue;
    if (r.from !== null && r.from > on) {
      if (next === undefined || r.from < next) next = r.from;
    } else if (best === undefined || (best.from ?? '') < (r.from ?? '')) {
      best = r;
    }
  }
  if (best === undefined) return false;
  return (best.to !== null && best.to < today) || (next !== undefined && next <= today);
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

/** amount × pct ÷ 100, rounded half to even (half up when `halfUp`) to two decimals; n/a when there is no rate. */
export function due(amount: string, pct: string | undefined, halfUp = false): string {
  if (pct === undefined) return NA;
  // amount (hundredths) × pct (hundredths) is in units of 1/10000 cent-percent: ÷ 10000 gives cents.
  const product = hundredths(amount) * hundredths(pct);
  const abs = product < 0n ? -product : product;
  const rest = abs % 10000n;
  let cents = abs / 10000n;
  if (rest > 5000n || (rest === 5000n && (halfUp || cents % 2n === 1n))) cents += 1n;
  return format(product < 0n ? -cents : cents);
}

/** A live due: amount at pct (see `due`), raised to the sheet's floor (two decimals) when below it;
 *  n/a when there is no rate. */
export function liveDue(amount: string, pct: string | undefined, floor: string | undefined, halfUp = false): string {
  const d = due(amount, pct, halfUp);
  if (d === NA || floor === undefined || hundredths(d) >= hundredths(floor)) return d;
  return format(hundredths(floor));
}

/** The sum of these dues, skipping n/a; two decimals. */
export function dueTotal(dues: string[]): string {
  return format(dues.filter((d) => d !== NA).reduce((sum, d) => sum + hundredths(d), 0n));
}

/** What a claim's late charge follows: the sheet's late percent (two decimals) and how many full
 *  30 days the claim is overdue; and whether the claim's live figures round half up (else half to even). */
export type Late = { pct: string | undefined; periods: (claim: Claim) => number; halfUp?: (claim: Claim) => boolean };

/** The late charge on a figure: the late percent of it for every full 30 days overdue, rounded like due. */
function lateCharge(claim: Claim, figure: string, late: Late | undefined): string | undefined {
  const periods = late?.pct === undefined || isCorrection(claim) ? 0 : late.periods(claim);
  if (periods <= 0 || figure === NA) return undefined;
  return due(figure, format(hundredths(late!.pct!) * BigInt(periods)), late?.halfUp?.(claim) ?? false);
}

/** Whether the claim keeps a figure whatever the rates: fixed when settled, or bound by a statement to a
 *  figure (bound to n/a keeps no figure). */
const fixed = (claim: Claim) => claim.due !== null || (isBound(claim) && claim.statedDue !== null);

/** A claim's due: the due fixed when it was settled (its late charge frozen in), else its figure — the
 *  figure its person's last statement bound it to (not while PENDING), else its live due: its amount at the rate in force
 *  on the claim's date of its person's grade in force on that date, at least the sheet's floor — plus its late charge when overdue.
 *  A correction's live due has no floor and it has no late charge.
 *  A statement never binds the charge: it keeps following the sheet's today. */
export function claimDue(
  claim: Claim,
  rates: Rate[] | undefined,
  grades: Grades,
  floor: string | undefined,
  late?: Late,
): string {
  if (claim.due !== null) return claim.due;
  const pct = rateOf(rates, gradeOn(grades, claim.date), claim.date);
  const halfUp = late?.halfUp?.(claim) ?? false;
  const figure = isBound(claim)
    ? claim.statedDue ?? NA
    : isCorrection(claim) ? due(claim.amount, pct, halfUp) : liveDue(claim.amount, pct, floor, halfUp);
  const charge = lateCharge(claim, figure, late);
  return charge === undefined ? figure : format(hundredths(figure) + hundredths(charge));
}

/** A person's due-total: the sum of the figures of their claims that count, at most the sheet's cap
 *  (two decimals; null or undefined for none). n/a when their grade
 *  in force on the sheet's today has no rate then, unless a claim that counts keeps a fixed (settled or
 *  stated) figure, or a figure at another grade (the one in force on its date), or at a rate that ended before today;
 *  n/a is not capped. */
export function personDue(
  claims: Claim[],
  rates: Rate[] | undefined,
  grades: Grades,
  today: string | undefined,
  floor: string | undefined,
  late?: Late,
  cap?: string | null,
): string {
  const total = uncappedDue(claims, rates, grades, today, floor, late);
  return total === NA || cap == null || hundredths(total) <= hundredths(cap) ? total : format(hundredths(cap));
}

/** A person's excess: how far the sum of their figures is above the sheet's cap; 0.00 when it is not, or
 *  there is no cap, or their due-total is n/a. */
export function personExcess(
  claims: Claim[],
  rates: Rate[] | undefined,
  grades: Grades,
  today: string | undefined,
  floor: string | undefined,
  late?: Late,
  cap?: string | null,
): string {
  const total = uncappedDue(claims, rates, grades, today, floor, late);
  return total === NA || cap == null || hundredths(total) <= hundredths(cap)
    ? format(0n)
    : format(hundredths(total) - hundredths(cap));
}

/** A person's due-total before the cap (see `personDue`). */
function uncappedDue(
  claims: Claim[],
  rates: Rate[] | undefined,
  grades: Grades,
  today: string | undefined,
  floor: string | undefined,
  late?: Late,
): string {
  const mine = claims.filter(counts);
  const now = gradeOn(grades, today);
  const elsewhere = (c: Claim) => {
    const grade = gradeOn(grades, c.date);
    return rateOf(rates, grade, c.date) !== undefined
      && (grade !== now || (today !== undefined && endsBefore(rates, grade, c.date, today)));
  };
  if (rateOf(rates, now, today) === undefined && !mine.some((c) => fixed(c) || elsewhere(c))) {
    return NA;
  }
  return dueTotal(mine.map((c) => claimDue(c, rates, grades, floor, late)));
}
