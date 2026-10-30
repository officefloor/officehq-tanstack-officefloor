// Due is worked in exact integers: an amount in cents times a rate in hundredths of a percent is
// due in millionths, then rounded half to even to cents. No float ever touches it.

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

/** The due on an amount of cents at a rate ("12.50"), in cents; null when there is no rate. */
export function dueCents(cents: bigint, rate: string | undefined): bigint | null {
  if (rate === undefined) return null;
  const millionths = cents * toCents(rate);
  const negative = millionths < 0n;
  const abs = negative ? -millionths : millionths;
  const whole = abs / 10000n;
  const rest = abs % 10000n;
  const rounded = rest > 5000n || (rest === 5000n && whole % 2n === 1n) ? whole + 1n : whole;
  return negative ? -rounded : rounded;
}

/** A due for display: two decimals, or n/a. */
export function showDue(due: bigint | null): string {
  return due === null ? NA : formatCents(due);
}

/** A claim's due: the one fixed when it was settled or bound by a statement (a claim bound to n/a
 *  stays n/a), else its amount at the current rate. */
export function claimDue(claim: { amount: string; due: string; stated?: boolean }, rate: string | undefined): bigint | null {
  if (claim.due !== '') return toCents(claim.due);
  return claim.stated ? null : dueCents(toCents(claim.amount), rate);
}

/** A person's due-total: the sum of the figures of their claims that count. With no rate for their
 *  grade today it is n/a, unless a settled claim keeps a figure; then it is the sum of the figures
 *  there are. A claim whose figure is n/a adds nothing. */
export function dueTotal(
  claims: { amount: string; due: string; date: string; stated?: boolean }[],
  rateOn: (date: string) => string | undefined,
  rateToday: string | undefined,
): bigint | null {
  if (rateToday === undefined && !claims.some((c) => c.due !== '')) return null;
  return claims.reduce((sum, c) => sum + (claimDue(c, rateOn(c.date)) ?? 0n), 0n);
}
