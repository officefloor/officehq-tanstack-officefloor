import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade's rate, in force from its from-date (yyyy-mm-dd; null applies from the beginning of time) through its
 *  to-date (null: with no end). */
export type Rate = { grade: string; rate: string; from: string | null; to: string | null };

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
 * before it, a rate without a from-date counting as the earliest. Undefined when none applies, or the date is
 * after that one's to-date.
 */
export function rateOn(rates: readonly Rate[], grade: string, date: string): Rate | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    // ISO dates compare as text; no from-date ('') sorts before every date.
    if (found === undefined || (r.from ?? '') > (found.from ?? '')) found = r;
  }
  return found !== undefined && found.to != null && found.to < date ? undefined : found;
}

/** A two-decimal number as a whole count of hundredths ("10.25" -> 1025n, "-1.03" -> -103n), exactly. */
export function toHundredths(text: string): bigint {
  const trimmed = text.trim();
  if (trimmed.startsWith('-')) return -toHundredths(trimmed.slice(1));
  const [whole, frac = ''] = trimmed.split('.');
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
 * divided by 100, rounded to two decimals half to even (1.025 -> 1.02, 1.035 -> 1.04), or half up when
 * `halfUp` (1.025 -> 1.03). Rates are never negative; a correction's negative amount rounds the same way as
 * its size (-1.025 -> -1.02, half up -1.03).
 */
export function dueCents(amountCents: bigint, rateHundredths: bigint, halfUp = false): bigint {
  if (amountCents < 0n) return -dueCents(-amountCents, rateHundredths, halfUp);
  // cents * (rate/100 %) / 100 = cents * rateHundredths / 10000
  const exact = amountCents * rateHundredths;
  const whole = exact / 10000n;
  const rest = exact % 10000n;
  if (rest > 5000n || (rest === 5000n && (halfUp || whole % 2n === 1n))) return whole + 1n;
  return whole;
}

/** The late charge's terms as of the sheet's today: its late percent (in hundredths) in force on a claim's date; and
 *  the sheet's policy date (its today when blank) with the date of each numbered claim, which decide how a claim's
 *  live figures round. */
export type Late = { today: string; pctOn: (date: string) => bigint; policyDate: string;
  dateOfNumber: ReadonlyMap<string, string> };

/** Whether the claim's live figures round half up: it (or, for a correction, the claim it corrects) is dated before
 *  the sheet's policy date; on or after it they round half to even. Half to even until the sheet's today is known. */
export function roundsHalfUp(late: Late | undefined, claim: { date: string; corrects?: string | null }): boolean {
  if (late === undefined) return false;
  const on = (claim.corrects != null ? late.dateOfNumber.get(claim.corrects) : undefined) ?? claim.date;
  return on < late.policyDate;
}

/** A date (yyyy-mm-dd) plus a number of days, as yyyy-mm-dd. */
export function addDays(date: string, days: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

/** The days from one date (yyyy-mm-dd) to another. */
function daysBetween(from: string, to: string): number {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000);
}

/** Whether a claim is a correction: its amount is below 0.00. A correction has no floor and no late charge. */
export function isCorrection(claim: { amount: string }): boolean {
  return claim.amount.trim().startsWith('-') && toHundredths(claim.amount.trim().slice(1)) > 0n;
}

/** Whether a claim is still live and payable: OPEN, or PENDING a steward's approval of its settle. */
export function isOpen(claim: { status: string }): boolean {
  return claim.status === 'OPEN' || claim.status === 'PENDING';
}

/** Whether the person's last statement binds the claim to its `statedDue`: it was stated, and is not PENDING (a PENDING
 *  claim shows and totals its live figure; rejected back to OPEN, it is bound again). */
export function isBound(claim: { stated: boolean; status?: string }): boolean {
  return claim.stated && claim.status !== 'PENDING';
}

/** A claim's late charge in cents on its figure: for an OPEN (or PENDING) claim past its due-by (fixed when it was added), the
 *  late percent in force on its date of the figure for every full 30 days it is overdue, rounded half to even; otherwise (or for a correction) 0. */
export function lateCents(late: Late | undefined, claim: { amount: string; date: string; status: string; dueBy: string; corrects?: string | null },
  figureCents: bigint): bigint {
  if (late === undefined || !isOpen(claim) || isCorrection(claim)) return 0n;
  const overdue = daysBetween(claim.dueBy, late.today);
  const periods = overdue > 0 ? BigInt(Math.floor(overdue / 30)) : 0n;
  return periods === 0n ? 0n : dueCents(figureCents * periods, late.pctOn(claim.date), roundsHalfUp(late, claim));
}

/** A claim's due figure in cents: fixed when it was settled; while a statement binds it, the figure it was bound to;
 *  otherwise (or while it is PENDING) its amount times its person's grade's rate
 *  in force on the claim's date, rounded half to even (half up when `halfUp`), and at least the sheet's floor in force on the claim's date (in cents) unless it is a correction. Null when there is
 *  no figure (no rate of the grade then). */
export function claimFigureCents(rates: readonly Rate[], grade: string | undefined,
  claim: { amount: string; date: string; status?: string; settledDue: string | null; stated: boolean; statedDue: string | null },
  floorOn: (date: string) => bigint, halfUp = false): bigint | null {
  if (claim.settledDue != null) return toHundredths(claim.settledDue);
  if (isBound(claim)) return claim.statedDue == null ? null : toHundredths(claim.statedDue);
  const found = grade === undefined ? undefined : rateOn(rates, grade, claim.date);
  if (found === undefined) return null;
  const correction = isCorrection(claim);
  const amount = toHundredths(correction ? claim.amount.trim().slice(1) : claim.amount);
  const due = dueCents(correction ? -amount : amount, toHundredths(found.rate), halfUp);
  const floor = floorOn(claim.date);
  return correction || due >= floor ? due : floor;
}

/** A claim's due in cents: its figure (claimFigureCents) with its late charge on top while it is overdue. The
 *  charge is never bound by a statement; a settled claim's due already has it frozen in. Null without a figure. */
export function claimDueCents(rates: readonly Rate[], grade: string | undefined,
  claim: { amount: string; date: string; status: string; settledDue: string | null; stated: boolean; statedDue: string | null;
    dueBy: string; corrects?: string | null },
  floorOn: (date: string) => bigint, late: Late | undefined): bigint | null {
  const figure = claimFigureCents(rates, grade, claim, floorOn, roundsHalfUp(late, claim));
  return figure == null ? null : figure + lateCents(late, claim, figure);
}
