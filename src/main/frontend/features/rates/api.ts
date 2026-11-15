import { getJson, postJson } from '../../api/http';
import { counts, isBound, isCorrection, isOpen, isOverdue, type Claim } from '../claims/api';
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

/** A claim's rate figure: an amount times a percent, divided by 100, rounded to two decimals (half up
 *  when `halfUp`, else half to even; no floor: that is applied last, by `claimDue`);
 *  null (n/a) when there is no rate. Exact: worked in whole hundredths. */
export function dueOf(amount: string, pct: string | undefined, halfUp: boolean): string | null {
  if (pct === undefined) return null;
  // amount (1/100) x pct (1/100) / 100 is in 1/1_000_000; to cents is / 10_000.
  return formatCents(roundPct(hundredths(amount) * hundredths(pct), halfUp));
}

/** What a claim's late charge goes by: the sheet's late percent by date (a charge goes by the one in force on each 30-day boundary) and its today (a claim's due-by is its own);
 *  the sheet's policy date (its today when blank), which decides how a claim's live figures round; and the date
 *  a claim's lateness counts from (`lateFrom`: its due-by plus the grace days of its person's grade in force on
 *  its date, as in force on that date). */
export type Late = { pct: DatedOn; today: string; policy: string; lateFrom: (c: Claim) => string };

/** Whether a claim's live figures round half up: its round date (its own, or for a correction the
 *  date of the claim it corrects) is before the policy date; else they round half to even. */
export const roundsHalfUp = (c: Claim, late: Late) => (c.roundDate ?? c.date) < late.policy;

/** The late charge on an open (OPEN or PENDING) claim's `figure`, compounding: for each full 30 days it is past its
 *  due-by plus its grace days (`late.lateFrom`) on the sheet's today, the late percent in force on that boundary date
 *  (that date plus 30, 60, 90 ... days) of the figure plus the charges of the boundaries before it,
 *  each rounded by the policy date to two decimals, added up (0 when not overdue, and on a correction). */
export function lateCharge(c: Claim, figure: string, late: Late): bigint {
  const from = late.lateFrom(c);
  if (isCorrection(c) || !isOverdue(c, late.today, from)) return 0n;
  const start = Date.parse(`${from}T00:00:00Z`);
  const periods = Math.floor((Date.parse(`${late.today}T00:00:00Z`) - start) / 86_400_000 / 30);
  let total = 0n;
  for (let k = 1; k <= periods; k++) {
    const boundary = new Date(start + k * 30 * 86_400_000).toISOString().slice(0, 10);
    total += roundPct((hundredths(figure) + total) * hundredths(late.pct(boundary)), roundsHalfUp(c, late));
  }
  return total;
}

/** A person's grade in force on a date (yyyy-MM-dd). */
export type GradeOn = (date: string) => string;

/** A claim's due: the one fixed when it was settled (its late charge then frozen in), else its
 *  figure — the one bound by its person's last statement (not while PENDING; n/a when it bound none), else live at the rate in force on the
 *  claim's date of the grade in force then (`gradeOn`) — with its late charge on the sheet's today on top, and
 *  at least the `floor` in force on its date, applied last (a correction, a negative amount, has no floor). */
export function claimDue(c: Claim, rates: Rate[] | undefined, gradeOn: GradeOn, floor: DatedOn, late: Late): string | null {
  if (c.settledDue !== null) return c.settledDue;
  const figure = c.stated && c.status !== 'PENDING' ? c.statedDue : dueOf(c.amount, rateOn(rates, gradeOn(c.date), c.date), roundsHalfUp(c, late));
  if (figure === null) return null;
  const due = hundredths(figure) + lateCharge(c, figure, late);
  const least = hundredths(floor(c.date));
  return formatCents(!isCorrection(c) && due < least ? least : due);
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

/** A person's due split by status among their claims that count (`claims`, the live ones): settled (their SETTLED
 *  claims, at their frozen figures, corrections included) and open (their OPEN and PENDING ones); n/a ones skipped.
 *  The two add up to their due-total before the cap. */
export function splitDues(claims: Claim[], dueOfClaim: (c: Claim) => string | null) {
  return {
    settled: sumDues(claims.filter((c) => c.status === 'SETTLED').map(dueOfClaim)),
    open: sumDues(claims.filter(isOpen).map(dueOfClaim)),
  };
}

/** The overdue claims among those that count (`claims`, the live ones) on the sheet's today (`charges.today`,
 *  each past its `charges.lateFrom`): their count and the sum of their due figures (n/a ones skipped). */
export function overdue(claims: Claim[], charges: Late, dueOfClaim: (c: Claim) => string | null) {
  const late = claims.filter((c) => isOverdue(c, charges.today, charges.lateFrom(c)));
  return { count: late.length, total: sumDues(late.map(dueOfClaim)) };
}

/** The due of the open (OPEN or PENDING) claims among those that count (`claims`, the live ones), split by
 *  their age on the sheet's `today` (the days from the claim's date): 0 to 30, 31 to 60, and 61 or more
 *  (one dated after today is 0 to 30); n/a ones skipped. */
export function agedDues(claims: Claim[], today: string, dueOfClaim: (c: Claim) => string | null) {
  const buckets: Array<Array<string | null>> = [[], [], []];
  const now = Date.parse(`${today}T00:00:00Z`);
  for (const c of claims.filter(isOpen)) {
    const age = today === '' ? 0 : Math.round((now - Date.parse(`${c.date}T00:00:00Z`)) / 86_400_000);
    buckets[age <= 30 ? 0 : age <= 60 ? 1 : 2].push(dueOfClaim(c));
  }
  return { upTo30: sumDues(buckets[0]), upTo60: sumDues(buckets[1]), over60: sumDues(buckets[2]) };
}

/** Add up two-decimal dues, skipping n/a. */
export function sumDues(dues: Array<string | null>): string {
  return formatCents(dues.reduce<bigint>((t, d) => (d === null ? t : t + hundredths(d)), 0n));
}

/** A sheet total of per-person figures: added up (n/a ones are no figure), leaving out the single
 *  highest and the single lowest figure when there are three or more. */
export function trimmedSum(figures: Array<string | null>): string {
  const cents = figures.filter((f): f is string => f !== null).map(hundredths).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const kept = cents.length >= 3 ? cents.slice(1, -1) : cents;
  return formatCents(kept.reduce((t, c) => t + c, 0n));
}

function formatCents(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  const frac = (abs % 100n).toString().padStart(2, '0');
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${frac}`;
}

/** A due as shown: n/a when there is none. */
export const showDue = (due: string | null) => due ?? 'n/a';
