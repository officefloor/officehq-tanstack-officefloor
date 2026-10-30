// Due is worked in exact integers: an amount in cents times a rate in hundredths of a percent is
// due in millionths, then rounded half up (away from zero) to cents. No float ever touches it.

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
  const rounded = (abs + 5000n) / 10000n;
  return negative ? -rounded : rounded;
}

/** A due for display: two decimals, or n/a. */
export function showDue(due: bigint | null): string {
  return due === null ? NA : formatCents(due);
}

/** A claim's due: the one fixed when it was settled, else its amount at the current rate. */
export function claimDue(claim: { amount: string; due: string }, rate: string | undefined): bigint | null {
  return claim.due !== '' ? toCents(claim.due) : dueCents(toCents(claim.amount), rate);
}
