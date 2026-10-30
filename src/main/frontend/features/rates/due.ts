import { isOpen } from '../claims/useClaims';

// Due is worked in exact integers: an amount in cents times a rate in hundredths of a percent is
// due in millionths, then rounded to cents — half up for a claim dated before the sheet's policy date,
// half to even for one dated on or after it. No float ever touches it.

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

/** A live due raised to the sheet's floor ("1.00"); n/a stays n/a. */
export function floored(due: bigint | null, floor: string): bigint | null {
  if (due === null) return null;
  const min = toCents(floor);
  return due < min ? min : due;
}

/** What a late charge needs: the sheet's late percent ("10.00") and its today. */
export type Late = { pct: string; today: string | undefined };

const DAY_MS = 86_400_000;

/** Whether a claim's live figures round half up: its rounding date (a correction's is the date of
 *  the claim it corrects) is before the sheet's policy date (blank: the sheet's today). */
export const roundsHalfUp = (claim: { roundingDate?: string; date: string }, policy: string | undefined) =>
  policy !== undefined && policy !== '' && (claim.roundingDate || claim.date) < policy;

/** A claim with a negative amount is a correction: its due has no floor and no late charge. */
export const isCorrection = (claim: { amount?: string }) => claim.amount !== undefined && toCents(claim.amount) < 0n;

/** The late charge on a figure in cents: an OPEN (or PENDING) claim whose due-by (fixed when it was created) is
 *  before today gains the late percent of its figure for every full 30 days it is overdue, rounded
 *  like due. Never bound by a statement; settling freezes it into the settled due. */
export function lateCents(claim: { amount?: string; date: string; status?: string; roundingDate?: string; dueBy?: string }, figure: bigint, late: Late | undefined, policy?: string): bigint {
  if (late === undefined || late.today === undefined || !claim.dueBy || !isOpen(claim) || isCorrection(claim)) return 0n;
  const days = Math.round((Date.parse(late.today) - Date.parse(claim.dueBy)) / DAY_MS);
  const periods = days > 0 ? Math.floor(days / 30) : 0;
  return dueCents(figure * BigInt(periods), late.pct, roundsHalfUp(claim, policy)) ?? 0n;
}

/** A claim's due: the one fixed when it was settled or bound by a statement (a claim bound to n/a
 *  stays n/a; a PENDING claim is never bound), else its amount at the current rate, raised to the sheet's floor; an overdue claim's
 *  late charge is added on top. A correction is never floored and gains no late charge. Live figures
 *  round by the sheet's policy date (see roundsHalfUp). */
export function claimDue(
  claim: { amount: string; due: string; date: string; status?: string; stated?: boolean; roundingDate?: string; dueBy?: string },
  rate: string | undefined,
  floor: string,
  late?: Late,
  policy?: string,
): bigint | null {
  const live = dueCents(toCents(claim.amount), rate, roundsHalfUp(claim, policy));
  const figure = claim.due !== '' ? toCents(claim.due) : claim.stated && claim.status !== 'PENDING' ? null : isCorrection(claim) ? live : floored(live, floor);
  return figure === null ? null : figure + lateCents(claim, figure, late, policy);
}

/** A person's due-total: the sum of the figures of their claims that count. With no rate for their
 *  grade today it is n/a, unless a claim keeps a figure (a settled one, or one worked at the grade
 *  in force on its date); then it is the sum of the figures there are. A claim whose figure is n/a
 *  adds nothing. */
export function dueTotal(
  claims: { amount: string; due: string; date: string; status?: string; stated?: boolean; roundingDate?: string; dueBy?: string }[],
  rateOn: (date: string) => string | undefined,
  rateToday: string | undefined,
  floor: string,
  late?: Late,
  policy?: string,
): bigint | null {
  const figures = claims.map((c) => claimDue(c, rateOn(c.date), floor, late, policy));
  if (rateToday === undefined && figures.every((f) => f === null)) return null;
  return figures.reduce<bigint>((sum, f) => sum + (f ?? 0n), 0n);
}
