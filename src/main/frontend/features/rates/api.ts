import { getJson, postJson } from '../../api/http';
import { counts, isOverdue, type Claim } from '../claims/api';
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

/** Two-decimal text as a whole number of hundredths. */
function hundredths(text: string): bigint {
  const [whole, frac = ''] = text.trim().split('.');
  return BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
}

/** An amount times a percent, divided by 100, rounded half to even to two decimals, and at least
 *  the sheet's `floor`; null (n/a) when there is no rate. Exact: worked in whole hundredths. */
export function dueOf(amount: string, pct: string | undefined, floor: string): string | null {
  if (pct === undefined) return null;
  // amount (1/100) x pct (1/100) / 100 is in 1/1_000_000; to cents is / 10_000, half to even.
  const product = hundredths(amount) * hundredths(pct);
  const whole = product / 10_000n;
  const rest = product % 10_000n;
  const cents = rest > 5_000n || (rest === 5_000n && whole % 2n === 1n) ? whole + 1n : whole;
  const least = hundredths(floor);
  return formatCents(cents < least ? least : cents);
}

/** A claim's due: the one fixed when it was settled, else the one bound by its person's last
 *  statement, else live at the grade's rate in force on the claim's date, at least the `floor`. */
export const claimDue = (c: Claim, rates: Rate[] | undefined, grade: string, floor: string) =>
  c.settledDue ?? (c.stated ? c.statedDue : dueOf(c.amount, rateOn(rates, grade, c.date), floor));

/** A person's due-total: the sum of their counting claims' due figures (n/a ones skipped); n/a
 *  when their grade has no rate today (`pct`) and none of those claims keeps a settled or bound figure. */
export function personDue(claims: Claim[], rates: Rate[] | undefined, grade: string, pct: string | undefined,
  floor: string) {
  const own = claims.filter(counts);
  if (pct === undefined && !own.some((c) => c.settledDue !== null || (c.stated && c.statedDue !== null))) return null;
  return sumDues(own.map((c) => claimDue(c, rates, grade, floor)));
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
  const frac = (cents % 100n).toString().padStart(2, '0');
  return `${cents / 100n}.${frac}`;
}

/** A due as shown: n/a when there is none. */
export const showDue = (due: string | null) => due ?? 'n/a';
