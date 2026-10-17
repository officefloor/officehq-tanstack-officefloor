import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import type { Claim } from '../claims/claims';
import { usePeople } from '../people/people';
import { useCurrentSheet, useSheetToday } from '../session/session';

// A rate is a percent with two decimals ("12.50") for a grade from a from-date on (null: from the
// beginning of time); a grade may have several. A date uses the rate whose from-date is the latest on
// or before it; a date before every rate of its grade, or a grade with no rate, has none (n/a).
// Due is worked out in whole cents and hundredths of a percent with BigInt, so no floating-point
// error shows: amount × rate / 100, rounded half up (away from zero) to two decimals.
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

/** A two-decimal string as a whole number of hundredths. */
export const hundredths = (value: string): bigint => {
  const [whole, frac = ''] = value.split('.');
  return BigInt(whole) * 100n + BigInt((frac + '00').slice(0, 2));
};

/** Hundredths shown with two decimals. */
export const show = (n: bigint): string => {
  const sign = n < 0n ? '-' : '';
  const abs = n < 0n ? -n : n;
  return `${sign}${abs / 100n}.${String(abs % 100n).padStart(2, '0')}`;
};

/** The due on an amount at a rate, in cents: amount × rate / 100, rounded half up. */
export function dueCents(amount: string, rate: string): bigint {
  const product = hundredths(amount) * hundredths(rate); // cents × hundredths of a percent
  const sign = product < 0n ? -1n : 1n;
  return sign * ((sign * product + 5000n) / 10000n);
}

/** A claim's due in cents: a settled claim's is the one fixed when it was settled; an open or draft
 *  claim's is live, at its grade's rate in force on the claim's date. Undefined for n/a. */
export function claimDueCents(claim: Claim, rate: string | undefined): bigint | undefined {
  if (claim.status === 'SETTLED') return claim.settledDue === null ? undefined : hundredths(claim.settledDue);
  return rate === undefined ? undefined : dueCents(claim.amount, rate);
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

/** The rate for a person's grade in force on a date (default: the sheet's today), looked up by person id. */
export function usePersonRate() {
  const people = usePeople().data ?? [];
  const rateOf = useGradeRate();
  return (personId: number, date?: string) => rateOf(people.find((p) => p.id === personId)?.grade, date);
}
