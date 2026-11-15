import { getJson, postJson } from '../../api/http';
import { counts, dueBy, isBound, isCorrection, isOverdue, type Claim } from '../claims/api';
import { sheetKey } from '../sheet/api';
import type { DatedOn } from '../settings/api';

/** A grade's rate on the sheet: a percent as two-decimal text, in force from its from-date
 *  (yyyy-MM-dd; null applies from the beginning of time) through its to-date (null: no end). A grade
 *  with no rate is not listed. */
export type Rate = { sheetId: string; grade: string; pct: string; from: string | null; to: string | null };
/** A blank `from` sets the rate that applies from the beginning of time; a blank `to` has no end. */
export type RateInput = { grade: string; pct: string; from: string; to: string };

const base = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/rates`;

export const ratesKey = (sheetId: string) => [...sheetKey(sheetId), 'rates'] as const;

/** The sheet's rates, by grade, then by from-date (none first). */
export function listRates(sheetId: string): Promise<Rate[]> {
  return getJson<Rate[]>(base(sheetId));
}

/** Set a grade's rate from a from-date, replacing the one with the same grade and from-date;
 *  refused (rejects) when the percent is missing, negative or has more than two decimals, or the
 *  from-date or to-date is not a date, or the to-date is before the from-date. */
export function setRate(sheetId: string, by: string, input: RateInput): Promise<Rate> {
  return postJson<Rate>(`${base(sheetId)}?by=${encodeURIComponent(by)}`, input);
}

/** The grade's rate in force on a date (yyyy-MM-dd): the one with the latest from-date on or
 *  before it; undefined (n/a) when the date is before every rate the grade has, or it has none, or
 *  that one ended (its to-date) before the date. */
export function rateOn(rates: Rate[] | undefined, grade: string, date: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found === undefined || (found.to !== null && found.to < date) ? undefined : found.pct;
}

/** The rate in force on a date for each grade that has one then. */
export function rateByGrade(rates: Rate[] | undefined, date: string): Map<string, string> {
  const byGrade = new Map<string, string>();
  for (const r of rates ?? []) {
    const pct = rateOn(rates, r.grade, date);
    if (pct !== undefined) byGrade.set(r.grade, pct);
  }
  return byGrade;
}

/** Two-decimal text (may be negative) as a whole number of hundredths. */
function hundredths(text: string): bigint {
  const t = text.trim();
  const negative = t.startsWith('-');
  const [whole, frac = ''] = (negative ? t.slice(1) : t).split('.');
  const value = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -value : value;
}

/** An amount times a percent, divided by 100, rounded to two decimals (half up when `halfUp`, else
 *  half to even), and at least the sheet's `floor` (a correction, a negative amount, has no floor);
 *  null (n/a) when there is no rate. Exact: worked in whole hundredths. */
export function dueOf(amount: string, pct: string | undefined, floor: string, halfUp: boolean): string | null {
  if (pct === undefined) return null;
  // amount (1/100) x pct (1/100) / 100 is in 1/1_000_000; to cents is / 10_000.
  const cents = roundPct(hundredths(amount) * hundredths(pct), halfUp);
  const least = hundredths(floor);
  return formatCents(cents < least && hundredths(amount) >= 0n ? least : cents);
}

/** What a claim's late charge goes by: the sheet's late percent by date (a charge goes by the one in force on each 30-day boundary) and its today (a claim's due-by is its own);
 *  and the sheet's policy date (its today when blank), which decides how a claim's live figures round. */
export type Late = { pct: DatedOn; today: string; policy: string };

/** Whether a claim's live figures round half up: its round date (its own, or for a correction the
 *  date of the claim it corrects) is before the policy date; else they round half to even. */
export const roundsHalfUp = (c: Claim, late: Late) => (c.roundDate ?? c.date) < late.policy;

/** The late charge on an open (OPEN or PENDING) claim's `figure`: for each full 30 days it is past its due-by on
 *  the sheet's today, the late percent in force on that boundary date (its due-by plus 30, 60, 90 ... days) of it,
 *  each rounded by the policy date to two decimals, added up (0 when not overdue, and on a correction). */
export function lateCharge(c: Claim, figure: string, late: Late): bigint {
  if (isCorrection(c) || !isOverdue(c, late.today)) return 0n;
  const start = Date.parse(`${dueBy(c)}T00:00:00Z`);
  const periods = Math.floor((Date.parse(`${late.today}T00:00:00Z`) - start) / 86_400_000 / 30);
  let total = 0n;
  for (let k = 1; k <= periods; k++) {
    const boundary = new Date(start + k * 30 * 86_400_000).toISOString().slice(0, 10);
    total += roundPct(hundredths(figure) * hundredths(late.pct(boundary)), roundsHalfUp(c, late));
  }
  return total;
}

/** A person's grade in force on a date (yyyy-MM-dd). */
export type GradeOn = (date: string) => string;

/** A claim's due: the one fixed when it was settled (its late charge then frozen in), else its
 *  figure — the one bound by its person's last statement (not while PENDING; n/a when it bound none), else live at the rate in force on the
 *  claim's date of the grade in force then (`gradeOn`), at least the `floor` in force then — with its late charge
 *  on the sheet's today on top. */
export function claimDue(c: Claim, rates: Rate[] | undefined, gradeOn: GradeOn, floor: DatedOn, late: Late): string | null {
  if (c.settledDue !== null) return c.settledDue;
  const figure = c.stated && c.status !== 'PENDING' ? c.statedDue : dueOf(c.amount, rateOn(rates, gradeOn(c.date), c.date), floor(c.date), roundsHalfUp(c, late));
  return figure === null ? null : formatCents(hundredths(figure) + lateCharge(c, figure, late));
}

/** A product of hundredths and a percent's hundredths (in 1/1_000_000) to cents, half up (away
 *  from zero) when `halfUp`, else half to even (symmetric about zero). */
function roundPct(product: bigint, halfUp: boolean): bigint {
  if (product < 0n) return -roundPct(-product, halfUp);
  const whole = product / 10_000n;
  const rest = product % 10_000n;
  return rest > 5_000n || (rest === 5_000n && (halfUp || whole % 2n === 1n)) ? whole + 1n : whole;
}

/** A person's due-total: the sum of their counting claims' due figures (n/a ones skipped), at
 *  most the sheet's `cap` (null: no cap); n/a
 *  when their grade today has no rate today (`pct`) and none of those claims keeps a settled or bound
 *  figure, or a live one at a grade no longer in force on the sheet's today or dated on or before it
 *  (its rate since ended). */
export function personDue(claims: Claim[], rates: Rate[] | undefined, gradeOn: GradeOn, pct: string | undefined,
  floor: DatedOn, late: Late, cap: string | null) {
  const own = claims.filter(counts);
  const now = gradeOn(late.today);
  const kept = (c: Claim) => c.settledDue !== null || isBound(c)
    || ((gradeOn(c.date) !== now || c.date <= late.today) && rateOn(rates, gradeOn(c.date), c.date) !== undefined);
  if (pct === undefined && !own.some(kept)) return null;
  const sum = sumDues(own.map((c) => claimDue(c, rates, gradeOn, floor, late)));
  return cap !== null && hundredths(sum) > hundredths(cap) ? formatCents(hundredths(cap)) : sum;
}

/** What a person's due figures add up to above the sheet's `cap` (null: no cap); 0.00 when they do
 *  not exceed it, or their due-total is n/a. */
export function excessOf(claims: Claim[], rates: Rate[] | undefined, gradeOn: GradeOn, pct: string | undefined,
  floor: DatedOn, late: Late, cap: string | null): string {
  const full = personDue(claims, rates, gradeOn, pct, floor, late, null);
  return full === null || cap === null || hundredths(full) <= hundredths(cap) ? '0.00'
    : formatCents(hundredths(full) - hundredths(cap));
}

/** The overdue claims among those that count (`claims`, the live ones): their count and the sum
 *  of their due figures (n/a ones skipped). */
export function overdue(claims: Claim[], today: string, dueOfClaim: (c: Claim) => string | null) {
  const late = claims.filter((c) => isOverdue(c, today));
  return { count: late.length, total: sumDues(late.map(dueOfClaim)) };
}

/** Add up two-decimal dues, skipping n/a. */
export function sumDues(dues: Array<string | null>): string {
  return formatCents(dues.reduce<bigint>((t, d) => (d === null ? t : t + hundredths(d)), 0n));
}

function formatCents(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  const frac = (abs % 100n).toString().padStart(2, '0');
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${frac}`;
}

/** A due as shown: n/a when there is none. */
export const showDue = (due: string | null) => due ?? 'n/a';
