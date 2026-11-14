import { getJson, postJson } from '../../api/http';
import { counts, dueBy, isCorrection, isOverdue, type Claim } from '../claims/api';
import { sheetKey } from '../sheet/api';

/** A grade's rate on the sheet: a percent as two-decimal text, in force from its from-date
 *  (yyyy-MM-dd; null applies from the beginning of time). A grade with no rate is not listed. */
export type Rate = { sheetId: string; grade: string; pct: string; from: string | null };
/** A blank `from` sets the rate that applies from the beginning of time. */
export type RateInput = { grade: string; pct: string; from: string };

const base = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/rates`;

export const ratesKey = (sheetId: string) => [...sheetKey(sheetId), 'rates'] as const;

/** The sheet's rates, by grade, then by from-date (none first). */
export function listRates(sheetId: string): Promise<Rate[]> {
  return getJson<Rate[]>(base(sheetId));
}

/** Set a grade's rate from a from-date, replacing the one with the same grade and from-date;
 *  refused (rejects) when the percent is missing, negative or has more than two decimals, or the
 *  from-date is not a date. */
export function setRate(sheetId: string, by: string, input: RateInput): Promise<Rate> {
  return postJson<Rate>(`${base(sheetId)}?by=${encodeURIComponent(by)}`, input);
}

/** The grade's rate in force on a date (yyyy-MM-dd): the one with the latest from-date on or
 *  before it; undefined (n/a) when the date is before every rate the grade has, or it has none. */
export function rateOn(rates: Rate[] | undefined, grade: string, date: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found?.pct;
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

/** An amount times a percent, divided by 100, rounded half to even to two decimals, and at least
 *  the sheet's `floor` (a correction, a negative amount, has no floor); null (n/a) when there is no
 *  rate. Exact: worked in whole hundredths. */
export function dueOf(amount: string, pct: string | undefined, floor: string): string | null {
  if (pct === undefined) return null;
  // amount (1/100) x pct (1/100) / 100 is in 1/1_000_000; to cents is / 10_000, half to even.
  const cents = roundPct(hundredths(amount) * hundredths(pct));
  const least = hundredths(floor);
  return formatCents(cents < least && hundredths(amount) >= 0n ? least : cents);
}

/** What a claim's late charge goes by: the sheet's late percent, its terms (days) and its today. */
export type Late = { pct: string; terms: number; today: string };

/** The late charge on an OPEN claim's `figure`: the late percent of it for every full 30 days it is
 *  past its due-by on the sheet's today, rounded half to even to two decimals (0 when not overdue, and
 *  on a correction). */
export function lateCharge(c: Claim, figure: string, late: Late): bigint {
  if (isCorrection(c) || !isOverdue(c, late.terms, late.today)) return 0n;
  const days = (Date.parse(`${late.today}T00:00:00Z`) - Date.parse(`${dueBy(c, late.terms)}T00:00:00Z`)) / 86_400_000;
  return roundPct(hundredths(figure) * hundredths(late.pct) * BigInt(Math.floor(days / 30)));
}

/** A person's grade in force on a date (yyyy-MM-dd). */
export type GradeOn = (date: string) => string;

/** A claim's due: the one fixed when it was settled (its late charge then frozen in), else its
 *  figure — the one bound by its person's last statement, else live at the rate in force on the
 *  claim's date of the grade in force then (`gradeOn`), at least the `floor` — with its late charge
 *  on the sheet's today on top. */
export function claimDue(c: Claim, rates: Rate[] | undefined, gradeOn: GradeOn, floor: string, late: Late): string | null {
  if (c.settledDue !== null) return c.settledDue;
  const figure = c.stated ? c.statedDue : dueOf(c.amount, rateOn(rates, gradeOn(c.date), c.date), floor);
  return figure === null ? null : formatCents(hundredths(figure) + lateCharge(c, figure, late));
}

/** A product of hundredths and a percent's hundredths (in 1/1_000_000) to cents, half to even
 *  (symmetric about zero). */
function roundPct(product: bigint): bigint {
  if (product < 0n) return -roundPct(-product);
  const whole = product / 10_000n;
  const rest = product % 10_000n;
  return rest > 5_000n || (rest === 5_000n && whole % 2n === 1n) ? whole + 1n : whole;
}

/** A person's due-total: the sum of their counting claims' due figures (n/a ones skipped); n/a
 *  when their grade today has no rate today (`pct`) and none of those claims keeps a settled or bound
 *  figure, or a live one at a grade no longer in force on the sheet's today. */
export function personDue(claims: Claim[], rates: Rate[] | undefined, gradeOn: GradeOn, pct: string | undefined,
  floor: string, late: Late) {
  const own = claims.filter(counts);
  const now = gradeOn(late.today);
  const kept = (c: Claim) => c.settledDue !== null || (c.stated && c.statedDue !== null)
    || (gradeOn(c.date) !== now && rateOn(rates, gradeOn(c.date), c.date) !== undefined);
  if (pct === undefined && !own.some(kept)) return null;
  return sumDues(own.map((c) => claimDue(c, rates, gradeOn, floor, late)));
}

/** The overdue claims among those that count (`claims`, the live ones): their count and the sum
 *  of their due figures (n/a ones skipped). */
export function overdue(claims: Claim[], terms: number, today: string, dueOfClaim: (c: Claim) => string | null) {
  const late = claims.filter((c) => isOverdue(c, terms, today));
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
