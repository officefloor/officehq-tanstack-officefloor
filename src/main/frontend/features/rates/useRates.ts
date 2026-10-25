import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade's rate, in force from its from-date (yyyy-mm-dd); null applies from the beginning of time. */
export type Rate = { grade: string; rate: string; from: string | null };

/** The key every feature reads the sheet's rates under. */
export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

/** The sheet's rates: each grade may have several, with different from-dates. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
  });
}

/**
 * The grade's rate in force on a date (yyyy-mm-dd): the one whose from-date is the latest on or
 * before it, a rate without a from-date counting as the earliest. Undefined when none applies.
 */
export function rateOn(rates: readonly Rate[], grade: string, date: string): Rate | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    // ISO dates compare as text; no from-date ('') sorts before every date.
    if (found === undefined || (r.from ?? '') > (found.from ?? '')) found = r;
  }
  return found;
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
 * divided by 100, rounded half to even to two decimals (1.025 -> 1.02, 1.035 -> 1.04). Amounts and
 * rates are never negative.
 */
export function dueCents(amountCents: bigint, rateHundredths: bigint): bigint {
  // cents * (rate/100 %) / 100 = cents * rateHundredths / 10000
  const exact = amountCents * rateHundredths;
  const whole = exact / 10000n;
  const rest = exact % 10000n;
  if (rest > 5000n || (rest === 5000n && whole % 2n === 1n)) return whole + 1n;
  return whole;
}

/** A claim's due figure in cents: fixed when it was settled; while a statement binds it, the figure it was bound to;
 *  otherwise its amount times its person's grade's rate
 *  in force on the claim's date, rounded half to even. Null when there is no figure (no rate of the grade then). */
export function claimDueCents(rates: readonly Rate[], grade: string | undefined,
  claim: { amount: string; date: string; settledDue: string | null; stated: boolean; statedDue: string | null }): bigint | null {
  if (claim.settledDue != null) return toHundredths(claim.settledDue);
  if (claim.stated) return claim.statedDue == null ? null : toHundredths(claim.statedDue);
  const found = grade === undefined ? undefined : rateOn(rates, grade, claim.date);
  return found === undefined ? null : dueCents(toHundredths(claim.amount), toHundredths(found.rate));
}
