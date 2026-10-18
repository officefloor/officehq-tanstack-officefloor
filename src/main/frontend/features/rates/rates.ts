import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import type { Claim } from '../claims/claims';
import { gradeOn, usePeople } from '../people/people';
import { useCurrentSheet, useSheetToday } from '../session/session';

// A rate is a percent with two decimals ("12.50") for a grade from a from-date on (null: from the
// beginning of time); a grade may have several. A date uses the rate whose from-date is the latest on
// or before it; a date before every rate of its grade, or a grade with no rate, has none (n/a).
// Due is worked out in whole cents and hundredths of a percent with BigInt, so no floating-point
// error shows: amount × rate / 100, rounded to two decimals half to even, or half up for a claim dated
// before the sheet's policy date.
export type Rate = { grade: string; rate: string; from: string | null };
export type RateInput = { grade: string; rate: string; from: string };

/** 0.00 or more, with up to two decimals. */
export const PERCENT = /^\d{1,5}(\.\d{1,2})?$/;

/** What a due shows when its grade has no rate. */
export const NA = 'n/a';

const base = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/rates`;
export const ratesKey = (sheet: string) => ['sheet', sheet, 'rates'] as const;

/** The active sheet's rates, by grade. */
export function useRates() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: ratesKey(sheet ?? ''),
    queryFn: () => getJson<{ rates: Rate[] }>(base(sheet!)).then((r) => r.rates),
    enabled: !!sheet,
  });
}

export const setRate = (sheet: string, user: string, input: RateInput) => postJson<Rate>(base(sheet), input, user);

/** A two-decimal string (may be negative) as a whole number of hundredths. */
export const hundredths = (value: string): bigint => {
  const negative = value.startsWith('-');
  const [whole, frac = ''] = (negative ? value.slice(1) : value).split('.');
  const n = BigInt(whole) * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -n : n;
};

/** Hundredths shown with two decimals. */
export const show = (n: bigint): string => {
  const sign = n < 0n ? '-' : '';
  const abs = n < 0n ? -n : n;
  return `${sign}${abs / 100n}.${String(abs % 100n).padStart(2, '0')}`;
};

/** The due on an amount at a rate, in cents: amount × rate / 100, rounded half to even (or half up). */
export function dueCents(amount: string, rate: string, halfUp = false): bigint {
  return percentOf(hundredths(amount), hundredths(rate), halfUp);
}

/** A percent (hundredths of a percent) of cents, in cents, rounded half to even (or half up). */
export function percentOf(cents: bigint, percent: bigint, halfUp = false): bigint {
  const product = cents * percent; // cents × hundredths of a percent
  const sign = product < 0n ? -1n : 1n;
  const abs = sign * product;
  const whole = abs / 10000n;
  const rest = abs % 10000n;
  const up = rest > 5000n || (rest === 5000n && (halfUp || whole % 2n === 1n));
  return sign * (up ? whole + 1n : whole);
}

/** A claim's due in cents: a settled claim's is the one fixed when it was settled; an open or draft
 *  claim's is live, at its grade's rate in force on the claim's date, and at least the sheet's floor
 *  (cents), plus the late charge: the late percent (hundredths of a percent) of that floored due for each
 *  full 30 days it is overdue (`lateSpans`), rounded like due. A correction (a negative amount) has neither
 *  the floor nor the late charge. Both are rounded half up when `halfUp` (the claim is dated before the policy date),
 *  else half to even. Undefined for n/a. */
export function claimDueCents(claim: Claim, rate: string | undefined, floor: bigint, latePct = 0n, lateSpans = 0n, halfUp = false): bigint | undefined {
  if (claim.status === 'SETTLED') return claim.settledDue === null ? undefined : hundredths(claim.settledDue);
  if (rate === undefined) return undefined;
  const due = dueCents(claim.amount, rate, halfUp);
  if (due < 0n || claim.amount.startsWith('-')) return due; // a correction: no floor, no late charge
  const floored = due < floor ? floor : due;
  return floored + percentOf(floored, latePct * lateSpans, halfUp);
}

/** The grade's rate (two decimals) in force on a date (yyyy-mm-dd), or undefined when none applies. */
export function rateOn(rates: Rate[], grade: string | undefined, date: string | undefined): string | undefined {
  if (date === undefined) return undefined;
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found?.rate;
}

/** The rate (two decimals) for a grade in force on a date (default: the sheet's today), or undefined. */
export function useGradeRate() {
  const rates = useRates().data ?? [];
  const today = useSheetToday();
  return (grade: string | undefined, date: string | undefined = today) => rateOn(rates, grade, date);
}

/** The rate in force on a date (default: the sheet's today) for the person's grade in force on that date,
 *  looked up by person id. */
export function usePersonRate() {
  const people = usePeople().data ?? [];
  const rateOf = useGradeRate();
  const today = useSheetToday();
  return (personId: number, date: string | undefined = today) =>
    rateOf(gradeOn(people.find((p) => p.id === personId), date), date);
}
