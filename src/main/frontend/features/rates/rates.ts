import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { sheetKey, useCurrentSheet } from '../session/session';

/**
 * A grade's rate as the store holds it: a percent with two decimals, in force from its from-date
 * (yyyy-MM-dd; null: from the beginning of time) until the grade's next rate takes over.
 */
export type Rate = { grade: string; rate: string; from: string | null };

/** The fields the rate form submits; a blank from-date is from the beginning of time. */
export type RateInput = { grade: string; rate: string; from: string };

/** What a due shows when its grade has no rate. */
export const NA = 'n/a';

/** 0.00 or more, with at most two decimals. */
export const PERCENT = /^(\d{1,5}(\.\d{1,2})?|\.\d{1,2})$/;

/** The query key for the sheet's rates; under sheetKey, so re-loading the sheet re-reads them. */
export const ratesKey = (sheet: string) => [...sheetKey(sheet), 'rates'] as const;

/** The sheet's rates, by grade and then from-date (from the start first). */
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
 * before it, a rate with no from-date applying from the start; undefined when none applies.
 */
export function rateFor(rates: Rate[], grade: string, date: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (!found || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found?.rate;
}

/** A two-decimal value as whole hundredths, exactly. */
const hundredths = (value: string) => Math.round(Number(value) * 100);

/**
 * An amount times a percent rate, divided by 100, rounded half to even to two decimals; n/a with no
 * rate. Worked in whole hundredths so it is exact: cents × basis points / 10000.
 */
export function due(amount: string, rate: string | undefined): string {
  if (rate === undefined) return NA;
  const product = hundredths(amount) * hundredths(rate);
  const whole = Math.floor(Math.abs(product) / 10000);
  const rest = Math.abs(product) - whole * 10000;
  const up = rest > 5000 || (rest === 5000 && whole % 2 === 1);
  const cents = Math.sign(product) * (whole + (up ? 1 : 0));
  return (cents / 100).toFixed(2);
}

/** A live due raised to the floor (both two decimals); n/a stays n/a. */
export function floored(value: string, floor: string): string {
  return value !== NA && hundredths(value) < hundredths(floor) ? Number(floor).toFixed(2) : value;
}

/**
 * A claim's figure: the due fixed when it was settled, else the figure its statement bound it to,
 * else its amount at the rate in force on its date for the grade, at least the sheet's floor.
 */
export function claimDue(claim: Claim, rates: Rate[], grade: string, floor: string): string {
  if (claim.due !== null) return claim.due;
  if (claim.stated) return claim.statedDue ?? NA;
  return floored(due(claim.amount, rateFor(rates, grade, claim.date)), floor);
}

/** Whether a claim keeps a figure whatever the rates: fixed when settled, or bound by a statement. */
const keepsFigure = (c: Claim) => c.due !== null || (c.stated && c.statedDue !== null);

/**
 * A person's due-total: the figures of their claims that count, added up (0.00 with none). With no
 * rate in force on the sheet's today for their grade it is n/a, unless a settled or stated claim
 * keeps a figure: then it is the figures there are, added up. While they are held, their open claims do not count.
 */
export function personDue(
  claims: Claim[],
  rates: Rate[],
  grade: string,
  today: string,
  held: boolean,
  floor: string,
): string {
  const counted = claims.filter((c) => counts(c, held));
  const figures = counted.map((c) => claimDue(c, rates, grade, floor));
  if (rateFor(rates, grade, today) === undefined && !counted.some(keepsFigure)) {
    return NA;
  }
  return total(figures);
}

/** Dues added up, skipping n/a; with two decimals, added in cents so it is exact. */
export function total(dues: string[]): string {
  const cents = dues.filter((d) => d !== NA).reduce((sum, d) => sum + hundredths(d), 0);
  return (cents / 100).toFixed(2);
}
