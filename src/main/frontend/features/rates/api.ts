import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A grade's rate on the sheet: a percent as two-decimal text. A grade with no rate is not listed. */
export type Rate = { sheetId: string; grade: string; pct: string };
export type RateInput = { grade: string; pct: string };

const base = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/rates`;

export const ratesKey = (sheetId: string) => [...sheetKey(sheetId), 'rates'] as const;

/** The sheet's rates, by grade. */
export function listRates(sheetId: string): Promise<Rate[]> {
  return getJson<Rate[]>(base(sheetId));
}

/** Set a grade's rate; refused (rejects) when the percent is missing, negative or has more than
 *  two decimals. */
export function setRate(sheetId: string, by: string, input: RateInput): Promise<Rate> {
  return postJson<Rate>(`${base(sheetId)}?by=${encodeURIComponent(by)}`, input);
}

/** The rate for each grade that has one. */
export function rateByGrade(rates: Rate[] | undefined): Map<string, string> {
  return new Map((rates ?? []).map((r) => [r.grade, r.pct]));
}

/** Two-decimal text as a whole number of hundredths. */
function hundredths(text: string): bigint {
  const [whole, frac = ''] = text.trim().split('.');
  return BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
}

/** An amount times a percent, divided by 100, rounded half up to two decimals; null (n/a) when
 *  there is no rate. Exact: worked in whole hundredths. */
export function dueOf(amount: string, pct: string | undefined): string | null {
  if (pct === undefined) return null;
  // amount (1/100) x pct (1/100) / 100 is in 1/1_000_000; to cents is / 10_000, half up.
  const product = hundredths(amount) * hundredths(pct);
  const cents = (product + 5_000n) / 10_000n;
  return formatCents(cents);
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
