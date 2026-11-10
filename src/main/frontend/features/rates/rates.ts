import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Grade } from '../people/people';
import type { Claim } from '../claims/claims';

/** A grade's rate: a percent with two decimals. */
export type Rate = { grade: Grade; pct: string };

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

/** The sheet's rates, by grade. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ rates: Rate[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`)).rates,
  });
}

/** The sheet's rate for a grade, or null when it has none (or the rates are not read yet). */
export function useGradeRate(sheetId: string, grade: Grade): string | null {
  return (useRates(sheetId).data ?? []).find((r) => r.grade === grade)?.pct ?? null;
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
 * An amount times a rate, divided by 100, rounded half up to two decimals; n/a with no rate.
 * Worked in whole numbers so no floating-point error creeps in (amounts and rates are never
 * negative, so half up is half away from zero).
 */
export function due(amount: string, pct: string | null): string {
  if (pct === null) return NA;
  // cents × basis points is the due in millionths of a unit: 10000 of them to the cent.
  const x = hundredths(amount) * hundredths(pct);
  return showCents((x + 5000n) / 10000n);
}

/** Due values added up, skipping n/a. */
export function sumDue(values: string[]): string {
  return showCents(values.filter((v) => v !== NA).reduce((n, v) => n + hundredths(v), 0n));
}

/** A claim's due: the one fixed when it was settled, else its amount at the rate now. */
export function claimDue(claim: Claim, pct: string | null): string {
  return claim.due !== '' ? claim.due : due(claim.amount, pct);
}

