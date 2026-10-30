import { isDeferred, isOpen } from '../claims/useClaims';
import { lateFrom } from '../claims/overdue';
import type { Grace } from '../claims/useGrace';

// Due is worked in exact integers: an amount in cents times a rate in hundredths of a percent is
// due in millionths, then rounded to cents — by the sheet's rounding mode in force on the claim's date, else half up
// for a claim dated before the sheet's policy date, half to even for one dated on or after it. No float ever touches it.

export const NA = 'n/a';

/** "10.25" -> 1025n. */
export function toCents(amount: string): bigint {
  const negative = amount.trim().startsWith('-');
  const [whole, frac = ''] = amount.trim().replace(/^[-+]/, '').split('.');
  const cents = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -cents : cents;
}

/** 1025n -> "10.25". */
export function formatCents(cents: bigint): string {
  const negative = cents < 0n;
  const abs = negative ? -cents : cents;
  const frac = (abs % 100n).toString().padStart(2, '0');
  return `${negative ? '-' : ''}${abs / 100n}.${frac}`;
}

/** The due on an amount of cents at a rate ("12.50"), in cents, rounded half up or else half to
 *  even; null when there is no rate. */
export function dueCents(cents: bigint, rate: string | undefined, halfUp = false): bigint | null {
  if (rate === undefined) return null;
  const millionths = cents * toCents(rate);
  const negative = millionths < 0n;
  const abs = negative ? -millionths : millionths;
  const whole = abs / 10000n;
  const rest = abs % 10000n;
  const rounded = rest > 5000n || (rest === 5000n && (halfUp || whole % 2n === 1n)) ? whole + 1n : whole;
  return negative ? -rounded : rounded;
}

/** A due for display: two decimals, or n/a. */
export function showDue(due: bigint | null): string {
  return due === null ? NA : formatCents(due);
}

/** A live due raised to a floor ("1.00"); n/a stays n/a. */
export function floored(due: bigint | null, floor: string): bigint | null {
  if (due === null) return null;
  const min = toCents(floor);
  return due < min ? min : due;
}

/** The value of a dated setting in force on a date (a claim's: its date), e.g. "1.00". */
export type On = (date: string) => string;

/** What a late charge needs: the sheet's late percent in force on a date (a boundary's, "10.00"), its today and each
 *  claim's grace days (none when left out). */
export type Late = { pct: On; today: string | undefined; grace?: Grace };

const DAY_MS = 86_400_000;

/** How the sheet's claims round: its policy date (blank: the sheet's today; undefined while loading) and every
 *  rounding mode recorded (half-up, half-even; "" for none), oldest from-date first. */
export type Policy = { date: string | undefined; rounding: { from: string; value: string }[] };

/** Whether a claim's live figures round half up: by the rounding mode in force on its rounding date (a correction's is
 *  the date of the claim it corrects) when one is, else when that date is before the sheet's policy date. */
export function roundsHalfUp(claim: { roundingDate?: string; date: string }, policy: Policy | undefined): boolean {
  const on = claim.roundingDate || claim.date;
  const mode = (policy?.rounding ?? []).filter((d) => d.from <= on).pop()?.value ?? '';
  if (mode !== '') return mode === 'half-up';
  return policy?.date !== undefined && policy.date !== '' && on < policy.date;
}

/** A claim with a negative amount is a correction: its due has no floor and no late charge. */
export const isCorrection = (claim: { amount?: string }) => claim.amount !== undefined && toCents(claim.amount) < 0n;

/** The late charge on a figure in cents: an OPEN (or PENDING) claim whose due-by (fixed when it was created) is
 *  before today (and that is not deferred) gains, for each full 30 days it is overdue, the late percent in force on that
 *  boundary date (its due-by plus its grace days plus 30, 60, 90 ... days) of its figure plus the charges of the boundaries
 *  before it (compounding), each rounded like due, the charges added up. Never bound by a statement; settling freezes it into the settled due. */
export function lateCents(claim: { amount?: string; date: string; status?: string; roundingDate?: string; dueBy?: string; deferredUntil?: string; personId?: string }, figure: bigint, late: Late | undefined, policy?: Policy): bigint {
  if (late === undefined || late.today === undefined || !claim.dueBy || !isOpen(claim) || isDeferred(claim, late.today) || isCorrection(claim)) return 0n;
  const from = lateFrom(claim, late.grace);
  const days = Math.round((Date.parse(late.today) - Date.parse(from)) / DAY_MS);
  const periods = days > 0 ? Math.floor(days / 30) : 0;
  const halfUp = roundsHalfUp(claim, policy);
  let charge = 0n;
  for (let n = 1; n <= periods; n++) {
    const boundary = new Date(Date.parse(from) + n * 30 * DAY_MS).toISOString().slice(0, 10);
    charge += dueCents(figure + charge, late.pct(boundary), halfUp) ?? 0n;
  }
  return charge;
}

/** A claim's due: the one fixed when it was settled, frozen as it is; else the figure a statement bound it to (a claim bound
 *  to n/a stays n/a; a PENDING claim is never bound) or its amount at the current rate, with an overdue claim's late charge
 *  (each 30-day boundary at the late percent in force on it) worked on that figure alone and added on top, and the floor in
 *  force on its date applied last: the greater of the two. A correction is never floored and gains no late charge. Live
 *  figures round by the sheet's rounding mode or policy date (see roundsHalfUp). */
export function claimDue(
  claim: { amount: string; due: string; date: string; status?: string; stated?: boolean; roundingDate?: string; dueBy?: string; deferredUntil?: string; personId?: string },
  rate: string | undefined,
  floor: On,
  late?: Late,
  policy?: Policy,
): bigint | null {
  if (claim.due !== '' && claim.status === 'SETTLED') return toCents(claim.due);
  const figure = claim.due !== '' ? toCents(claim.due) : claim.stated && claim.status !== 'PENDING' ? null : dueCents(toCents(claim.amount), rate, roundsHalfUp(claim, policy));
  if (figure === null) return null;
  const charged = figure + lateCents(claim, figure, late, policy);
  return isCorrection(claim) ? charged : floored(charged, floor(claim.date));
}

/** A person's due-total: the sum of the figures of their claims that count. With no rate for their
 *  grade today it is n/a, unless a claim keeps a figure (a settled one, or one worked at the grade
 *  in force on its date); then it is the sum of the figures there are. A claim whose figure is n/a
 *  adds nothing. */
export function dueTotal(
  claims: { amount: string; due: string; date: string; status?: string; stated?: boolean; roundingDate?: string; dueBy?: string; personId?: string }[],
  rateOn: (date: string) => string | undefined,
  rateToday: string | undefined,
  floor: On,
  late?: Late,
  policy?: Policy,
): bigint | null {
  const figures = claims.map((c) => claimDue(c, rateOn(c.date), floor, late, policy));
  if (rateToday === undefined && figures.every((f) => f === null)) return null;
  return figures.reduce<bigint>((sum, f) => sum + (f ?? 0n), 0n);
}

/** A due-total held to the sheet's cap ("" for none): the cap when it is above it. n/a is not capped. */
export function capped(total: bigint | null, cap: string): bigint | null {
  if (total === null || cap === '') return total;
  const max = toCents(cap);
  return total > max ? max : total;
}

/** How far a due-total is above the sheet's cap; 0 when it is not, or is n/a. */
export function excess(total: bigint | null, cap: string): bigint {
  const held = capped(total, cap);
  return total === null || held === null ? 0n : total - held;
}
