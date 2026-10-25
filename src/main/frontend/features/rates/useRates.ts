import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type Rate = { grade: string; rate: string };

/** The key every feature reads the sheet's rates under. */
export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

/** The sheet's rate for each grade that has one. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
  });
}

/** A two-decimal number as a whole count of hundredths ("10.25" -> 1025n), exactly. */
export function toHundredths(text: string): bigint {
  const [whole, frac = ''] = text.trim().split('.');
  return BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
}

/** Hundredths shown with two decimals. */
export function showHundredths(n: bigint): string {
  const sign = n < 0n ? '-' : '';
  const abs = n < 0n ? -n : n;
  return `${sign}${abs / 100n}.${(abs % 100n).toString().padStart(2, '0')}`;
}

/**
 * Due, in cents, on an amount (in cents) at a rate (a percent, in hundredths): amount times rate
 * divided by 100, rounded half up to two decimals. Amounts and rates are never negative.
 */
export function dueCents(amountCents: bigint, rateHundredths: bigint): bigint {
  // cents * (rate/100 %) / 100 = cents * rateHundredths / 10000
  return (amountCents * rateHundredths * 2n + 10000n) / 20000n;
}
