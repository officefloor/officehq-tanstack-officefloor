import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { dueCents, hundredths } from '../rates/useRates';

/** Where a claim is in its life: DRAFT → OPEN → SETTLED, and DRAFT or OPEN → VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A claim as the server sends it: `amount` with two decimals, `date` as yyyy-mm-dd. */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  /** Given when the claim is opened (a voided claim keeps it); null for a DRAFT, or a claim voided while a DRAFT. */
  number: number | null;
  /** The due fixed when the claim was settled, two decimals; null when it has none, so its due is live. */
  due: string | null;
  /** The user who opened the claim; null while it has not been opened. */
  openedBy: string | null;
  /** Whether the person's last statement binds the claim to the figure it had then. */
  stated: boolean;
  /** The figure the last statement bound the claim to, two decimals; null when unbound or bound to n/a. */
  statedDue: string | null;
};

export const claimsKey = (sheet: string) => ['sheet', sheet, 'claims'] as const;

/** Every claim on the sheet, newest first, re-read whenever the sheet is loaded. */
export function useClaims(sheet: string) {
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims`),
    staleTime: 0,
    gcTime: 0,
  });
}

/** An amount the server accepts: 0.00 or more, with up to two decimals. */
export const AMOUNT = /^\d+(\.\d{1,2})?$/;

/** The claims that count toward claimed, due-total and every total: OPEN and SETTLED ones. */
export function counted(claims: Claim[]): Claim[] {
  return claims.filter((c) => c.status === 'OPEN' || c.status === 'SETTLED');
}

/** The sum of these claims' amounts, with two decimals. Added up in cents, so it is exact. */
export function claimed(claims: Claim[]): string {
  const cents = claims.reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}

/** What is due on one claim, in cents: the due fixed when it was settled, or else the figure its
 *  person's last statement bound it to, or else live at `pct` (null, n/a, when there is no rate). */
export function claimDueCents(claim: Claim, pct: string | undefined): bigint | null {
  if (claim.due !== null) return hundredths(claim.due);
  if (claim.stated) return claim.statedDue !== null ? hundredths(claim.statedDue) : null;
  return dueCents(claim.amount, pct);
}


/** A person's due-total, in cents: the sum of their counted claims' figures, each already rounded —
 *  a settled claim's fixed due, else the figure a statement bound it to, else live at `pctOn(claim's date)`, the rate in force on the claim's
 *  date (at `pct`, the rate in force on the sheet's today, when no `pctOn` is given). 0 with
 *  nothing that counts. Null (n/a) when there is no rate today, unless some settled claim keeps a
 *  figure: then it is the sum of the figures there are. */
export function personDueCents(claims: Claim[], pct: string | undefined, pctOn?: (date: string) => string | undefined): bigint | null {
  let sum = 0n;
  let any = false;
  for (const c of counted(claims)) {
    const due = claimDueCents(c, pctOn ? pctOn(c.date) : pct);
    if (due === null) continue;
    sum += due;
    any = true;
  }
  return pct === undefined && !any ? null : sum;
}
