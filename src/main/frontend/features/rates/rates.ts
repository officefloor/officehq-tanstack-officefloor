import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Grade } from '../people/people';
import { counts } from '../claims/claims';
import type { Claim } from '../claims/claims';

/**
 * A grade's rate: a percent with two decimals, from a date (yyyy-MM-dd; '' when it applies from the
 * beginning of time).
 */
export type Rate = { grade: Grade; pct: string; from: string };

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

/** The sheet's rates, by grade then from-date (none first). */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ rates: Rate[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`)).rates,
  });
}

/**
 * A grade's rate in force on a date (yyyy-MM-dd): the one whose from-date is the latest on or
 * before it. Null when the date is before every rate the grade has (or it has none).
 */
export function rateOn(rates: Rate[], grade: Grade, date: string): string | null {
  let best: Rate | null = null;
  for (const r of rates) {
    if (r.grade !== grade || r.from > date) continue;
    if (best === null || r.from > best.from) best = r;
  }
  return best?.pct ?? null;
}

/** The sheet's rate for a grade on a date, or null when none is in force (or the rates are not read yet). */
export function useGradeRate(sheetId: string, grade: Grade, date: string): string | null {
  return rateOn(useRates(sheetId).data ?? [], grade, date);
}

/** A percent as a rate takes it: 0.00 or more, with up to two decimals. */
export const PCT = /^\d+(\.\d{1,2})?$/;

export const NA = 'n/a';

/** A two-decimal number as a whole count of hundredths. */
function hundredths(value: string): bigint {
  const [whole, frac = ''] = value.trim().split('.');
  return BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
}

function showCents(cents: bigint): string {
  return `${cents / 100n}.${(cents % 100n).toString().padStart(2, '0')}`;
}

/**
 * An amount times a rate, divided by 100, rounded half to even to two decimals; n/a with no rate.
 * Worked in whole numbers so no floating-point error creeps in (amounts and rates are never
 * negative).
 */
export function due(amount: string, pct: string | null): string {
  if (pct === null) return NA;
  // cents × basis points is the due in millionths of a unit: 10000 of them to the cent.
  const x = hundredths(amount) * hundredths(pct);
  const cents = x / 10000n;
  const rest = x % 10000n;
  return showCents(rest > 5000n || (rest === 5000n && cents % 2n === 1n) ? cents + 1n : cents);
}

/** Due values added up, skipping n/a. */
export function sumDue(values: string[]): string {
  return showCents(values.filter((v) => v !== NA).reduce((n, v) => n + hundredths(v), 0n));
}

/**
 * A claim's due: the one fixed when it was settled, else the figure its person's last statement
 * bound it to, else its amount at the rate in force on its date.
 */
export function claimDue(claim: Claim, pct: string | null): string {
  if (claim.due !== '') return claim.due;
  return claim.stated ? claim.statedDue : due(claim.amount, pct);
}

/**
 * A person's due-total: the figures of their claims that count (each at the rate in force on its
 * date), added up, skipping n/a. With no rate for their grade today it is n/a, unless a settled
 * claim that counts keeps a fixed figure (settled, or bound by a statement).
 */
export function personDueTotal(rates: Rate[], grade: Grade, today: string, claims: Claim[]): string {
  const counted = claims.filter(counts);
  if (rateOn(rates, grade, today) === null && !counted.some((c) => c.due !== '' || c.stated)) return NA;
  return sumDue(counted.map((c) => claimDue(c, rateOn(rates, grade, c.date))));
}

