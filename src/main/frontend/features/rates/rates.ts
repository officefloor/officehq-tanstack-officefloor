import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { usePeople } from '../people/people';
import { useCurrentSheet } from '../session/session';

// A rate is a percent with two decimals ("12.50"), one per grade; a grade with no rate has none.
// Due is worked out in whole cents and hundredths of a percent with BigInt, so no floating-point
// error shows: amount × rate / 100, rounded half up (away from zero) to two decimals.
export type Rate = { grade: string; rate: string };
export type RateInput = { grade: string; rate: string };

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

/** The rate (two decimals) for a person's grade, or undefined when the grade has none. */
export function useGradeRate() {
  const rates = useRates().data ?? [];
  return (grade: string | undefined) => rates.find((r) => r.grade === grade)?.rate;
}

/** The rate for a person's grade, looked up by person id. */
export function usePersonRate() {
  const people = usePeople().data ?? [];
  const rateOf = useGradeRate();
  return (personId: number) => rateOf(people.find((p) => p.id === personId)?.grade);
}
