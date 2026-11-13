import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { bound, counts } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { lateBoundary, latePeriods } from '../overdue/dueBy';
import { gradeOn } from '../people/people';
import type { GradeFrom, Hold } from '../people/people';
import { sheetKey, useCurrentSheet } from '../session/session';
import { claimGrace } from '../settings/settings';
import type { DueRules } from '../settings/settings';

/**
 * A grade's rate as the store holds it: a percent with two decimals, in force from its from-date
 * (yyyy-MM-dd; null: from the beginning of time) until the grade's next rate takes over, or through its
 * to-date (null: no end), whichever comes first. `sheet` is the sheet it is set on: the sheet's own rates come
 * first, then those it inherits from its parent, its parent's parent and so on (up to three levels up).
 */
export type Rate = { grade: string; rate: string; from: string | null; to: string | null; sheet: string };

/** The fields the rate form submits; a blank from-date is from the beginning of time, a blank to-date no end. */
export type RateInput = { grade: string; rate: string; from: string; to: string };

/** What a due shows when its grade has no rate. */
export const NA = 'n/a';

/** 0.00 or more, with at most two decimals. */
export const PERCENT = /^(\d{1,5}(\.\d{1,2})?|\.\d{1,2})$/;

/** The query key for the sheet's rates; under sheetKey, so re-loading the sheet re-reads them. */
export const ratesKey = (sheet: string) => [...sheetKey(sheet), 'rates'] as const;

/** The sheet's rates, by grade and then from-date (from the start first), then those it inherits, nearest parent first. */
export function useRates() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: ratesKey(sheet),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheet)}/rates`),
    enabled: !!sheet,
  });
}

/**
 * The grade's rate (two decimals) in force on the date: the one with the latest from-date on or
 * before it, a rate with no from-date applying from the start; undefined when none applies, or when that
 * one's to-date is before the date. With inherited rates, the first sheet (own, then the nearest parent up) with
 * a rate in force gives it.
 */
export function rateFor(rates: Rate[], grade: string | undefined, date: string): string | undefined {
  const sheets = [...new Set(rates.map((r) => r.sheet))];
  for (const sheet of sheets) {
    const rate = ownRateFor(rates.filter((r) => r.sheet === sheet), grade, date);
    if (rate !== undefined) return rate;
  }
  return undefined;
}

/** The grade's rate in force on the date among one sheet's rates, as {@link rateFor}. */
function ownRateFor(rates: Rate[], grade: string | undefined, date: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (!found || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  if (found?.to != null && found.to < date) return undefined;
  return found?.rate;
}

/** A two-decimal value as whole hundredths, exactly. */
const hundredths = (value: string) => Math.round(Number(value) * 100);

/** How a half is rounded: half up (away from zero) or half to even. */
export type Rounding = 'half-up' | 'half-even';

/**
 * How a claim's live figures round: half up when it is dated before the sheet's policy date, half to
 * even on or after it. A correction rounds by the date of the claim it corrects, not its own.
 */
export function roundingFor(claim: Claim, rules: DueRules): Rounding {
  const date = claim.corrects !== null ? (rules.claimDates.get(claim.corrects) ?? claim.date) : claim.date;
  return date < rules.policyDate ? 'half-up' : 'half-even';
}

/**
 * An amount times a percent rate, divided by 100, rounded to two decimals (half to even unless told
 * otherwise); n/a with no rate. Worked in whole hundredths so it is exact: cents × basis points / 10000.
 */
export function due(amount: string, rate: string | undefined, rounding: Rounding = 'half-even'): string {
  if (rate === undefined) return NA;
  return percentOf(hundredths(amount), hundredths(rate), rounding);
}

/** Cents times basis points / 10000, rounded to two decimals as told. */
function percentOf(amountCents: number, basisPoints: number, rounding: Rounding): string {
  const product = amountCents * basisPoints;
  const whole = Math.floor(Math.abs(product) / 10000);
  const rest = Math.abs(product) - whole * 10000;
  const up = rest > 5000 || (rest === 5000 && (rounding === 'half-up' || whole % 2 === 1));
  const cents = Math.sign(product) * (whole + (up ? 1 : 0));
  return (cents / 100).toFixed(2);
}

/** A due raised to the floor (both two decimals); n/a stays n/a. */
export function floored(value: string, floor: string): string {
  return value !== NA && hundredths(value) < hundredths(floor) ? Number(floor).toFixed(2) : value;
}

/**
 * A claim's figure: the figure its statement bound it to (not while PENDING), else its amount at the rate in force on its
 * date for the person's grade in force on that date, rounded by the policy date — no floor.
 */
function claimFigure(claim: Claim, rates: Rate[], grades: GradeFrom[], rules: DueRules): string {
  if (bound(claim)) return claim.statedDue ?? NA;
  return due(claim.amount, rateFor(rates, gradeOn(grades, claim.date), claim.date), roundingFor(claim, rules));
}

/**
 * The late charge on a figure, compounded: for every full 30 days the claim is overdue, the late percent in force on that
 * boundary date (its due-by plus the grace days of its person's grade on its date plus 30, 60, 90 ... days) of the figure
 * plus the charges of the boundaries before it, rounded like the claim's due, the charges added up (a boundary at 0.00
 * adds nothing); 0.00 when it is not overdue, and always for a correction.
 */
export function lateCharge(claim: Claim, figure: string, grades: GradeFrom[], rules: DueRules): string {
  if (claim.corrects !== null) return '0.00';
  const grace = claimGrace(claim, grades, rules);
  const periods = latePeriods(claim, rules.today, grace);
  const charges: string[] = [];
  for (let period = 1; period <= periods; period++) {
    const pct = hundredths(rules.latePctOn(lateBoundary(claim, period, grace)));
    if (pct !== 0) charges.push(percentOf(hundredths(total([figure, ...charges])), pct, roundingFor(claim, rules)));
  }
  return total(charges);
}

/**
 * A claim's due: the one fixed when it was settled (any late charge and floor frozen in), else the greater of the
 * floor in force on its date and its figure plus the late charge on it while it is overdue — a correction has no
 * floor and no charge. Neither the charge nor the floor is bound by a statement: they follow the sheet's today.
 */
export function claimDue(claim: Claim, rates: Rate[], grades: GradeFrom[], rules: DueRules): string {
  if (claim.due !== null) return claim.due;
  const figure = claimFigure(claim, rates, grades, rules);
  if (figure === NA || claim.corrects !== null) return figure;
  return floored(total([figure, lateCharge(claim, figure, grades, rules)]), rules.floorOn(claim.date));
}

/** Whether a claim keeps a figure whatever the rates: fixed when settled, or bound by a statement. */
const keepsFigure = (c: Claim) => c.due !== null || (bound(c) && c.statedDue !== null);

/**
 * A person's due-total: the figures of their claims that count, added up (0.00 with none). With no
 * rate in force on the sheet's today for their grade on that day it is n/a, unless a claim has a figure (kept
 * when settled or stated, or at the rate for the grade in force on its date): then it is the figures there are,
 * added up. Their open claims a hold covers (not bound by a statement) do not count. It is at most the
 * sheet's cap (n/a is not capped).
 */
export function personDue(
  claims: Claim[],
  rates: Rate[],
  grades: GradeFrom[],
  holds: Hold[],
  rules: DueRules,
): string {
  const sum = personSum(claims, rates, grades, holds, rules);
  if (sum === NA || rules.cap === '' || hundredths(sum) <= hundredths(rules.cap)) return sum;
  return rules.cap;
}

/** A person's excess: by how much their due-total is above the sheet's cap; 0.00 when it is not (or is n/a). */
export function personExcess(
  claims: Claim[],
  rates: Rate[],
  grades: GradeFrom[],
  holds: Hold[],
  rules: DueRules,
): string {
  const sum = personSum(claims, rates, grades, holds, rules);
  if (sum === NA || rules.cap === '') return '0.00';
  return (Math.max(0, hundredths(sum) - hundredths(rules.cap)) / 100).toFixed(2);
}

/** A person's due-total before the cap: their counting claims' figures added up, or n/a. */
function personSum(
  claims: Claim[],
  rates: Rate[],
  grades: GradeFrom[],
  holds: Hold[],
  rules: DueRules,
): string {
  const counted = claims.filter((c) => counts(c, holds, rules.today));
  const figures = counted.map((c) => claimDue(c, rates, grades, rules));
  const anyFigure = counted.some(keepsFigure) || figures.some((f) => f !== NA);
  if (rateFor(rates, gradeOn(grades, rules.today), rules.today) === undefined && !anyFigure) {
    return NA;
  }
  return total(figures);
}

/** Dues added up, skipping n/a; with two decimals, added in cents so it is exact. */
export function total(dues: string[]): string {
  const cents = dues.filter((d) => d !== NA).reduce((sum, d) => sum + hundredths(d), 0);
  return (cents / 100).toFixed(2);
}

/**
 * A sheet's due-total or overdue-total: the people's figures added up, skipping n/a — leaving out the single
 * highest and the single lowest figure when three or more people have one.
 */
export function trimmedTotal(dues: string[]): string {
  const cents = dues.filter((d) => d !== NA).map(hundredths).sort((a, b) => a - b);
  const kept = cents.length >= 3 ? cents.slice(1, -1) : cents;
  return (kept.reduce((sum, c) => sum + c, 0) / 100).toFixed(2);
}

/**
 * A person's settled-total and open-total: the dues of their SETTLED claims that count (at their frozen figures,
 * corrections included), and of their OPEN and PENDING ones that count, each added up (skipping n/a, not capped) —
 * together their due-total before the cap.
 */
export function personSplit(
  claims: Claim[],
  rates: Rate[],
  grades: GradeFrom[],
  holds: Hold[],
  rules: DueRules,
): { settled: string; open: string } {
  const counted = claims.filter((c) => counts(c, holds, rules.today));
  const dues = (keep: (c: Claim) => boolean) =>
    total(counted.filter(keep).map((c) => claimDue(c, rates, grades, rules)));
  return { settled: dues((c) => c.status === 'SETTLED'), open: dues((c) => c.status !== 'SETTLED') };
}

/**
 * What a candidate rate for a grade would do: of the OPEN and PENDING claims whose person is on that grade on the
 * claim's date, how many would show a different due were that the grade's only rate on every date, and the
 * differences (new due minus current due, n/a as 0.00) added up. A claim bound by a statement keeps its figure.
 */
export function ratePreview(
  claims: Claim[],
  gradesOf: (personId: number) => GradeFrom[],
  rates: Rate[],
  grade: string,
  rate: string,
  rules: DueRules,
): { count: number; diff: string } {
  const candidate: Rate[] = [...rates.filter((r) => r.grade !== grade), { grade, rate, from: null, to: null, sheet: '' }];
  const cents = (d: string) => (d === NA ? 0 : hundredths(d));
  let count = 0;
  let diff = 0;
  for (const c of claims) {
    if (c.status !== 'OPEN' && c.status !== 'PENDING') continue;
    const grades = gradesOf(c.personId);
    if (gradeOn(grades, c.date) !== grade) continue;
    const now = claimDue(c, rates, grades, rules);
    const then = claimDue(c, candidate, grades, rules);
    if (now === then) continue;
    count++;
    diff += cents(then) - cents(now);
  }
  return { count, diff: (diff / 100).toFixed(2) };
}
