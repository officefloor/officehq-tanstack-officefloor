import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** A claim; `amount` is shown with two decimals ("10.25"); `number` is the one given when it was
 *  opened (a voided claim keeps it), "" for a draft; `due` is the due fixed when it was settled, else the
 *  figure the person's last statement bound it to, "" while its due is live (worked from the current
 *  rate) or bound to n/a (a PENDING claim is never bound: its due is live); `stated` is whether the last statement bound it. A claim with a negative
 *  amount is a correction: its due has no floor and no late charge. */
export type Claim = {
  id: string;
  personId: string;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: string;
  due: string;
  /** The user who opened the claim, "" if it was never opened. */
  openedBy: string;
  stated: boolean;
  /** The number of the SETTLED claim a correction (a negative amount) corrects, "" for any other claim. */
  corrects: string;
  /** The date the claim's rounding goes by: a correction's is the date of the claim it corrects. */
  roundingDate: string;
  /** The claim's due-by, fixed when it was created: its date plus the terms of its person's grade in force on that date. */
  dueBy: string;
  /** The claim's status log, oldest first: "<STATUS> by <user> on <date> due=<due>" per status it has had. */
  log: string[];
  /** The date the claim was last deferred until, "" if it was never deferred. */
  deferredUntil: string;
};

/** OPEN and PENDING claims are open: their due is live (unless bound), they go overdue and a hold
 *  takes them out of the totals. */
export const isOpen = (c: { status?: string }) => c.status === 'OPEN' || c.status === 'PENDING';

/** Only OPEN, PENDING and SETTLED claims count toward claimed, due-total and every total. */
export const counts = (c: Claim) => isOpen(c) || c.status === 'SETTLED';

/** Whether a statement binds the claim's figure now: a PENDING claim is never bound. */
export const isBound = (c: Claim) => c.stated && c.status !== 'PENDING';

/** Whether a claim is deferred on the sheet's today: OPEN and deferred until a later date. A deferred claim
 *  counts in no total, is not overdue, cannot be settled and is left unbound by a statement. */
export const isDeferred = (c: { status?: string; deferredUntil?: string }, today: string | undefined) =>
  c.status === 'OPEN' && !!c.deferredUntil && today !== undefined && today < c.deferredUntil;

/** Whether a claim counts in the live totals: it counts, is not deferred on the sheet's today, and is not
 *  open, unbound and dated within one of its person's holds (`heldOn`). */
export const countsLive = (c: Claim, heldOn: (date: string) => boolean, today: string | undefined) =>
  counts(c) && !isDeferred(c, today) && !(isOpen(c) && !isBound(c) && heldOn(c.date));

export const claimsKey = (sheet: string) => ['sheet', sheet, 'claims'] as const;

/** The sheet's claims, newest first. */
export function useClaims(sheet: string): Claim[] {
  const { data } = useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims`),
    enabled: sheet !== '',
  });
  return data ?? [];
}

/** The amounts of the claims that count added up, shown with two decimals. Summed in whole cents
 *  so no float drift shows. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
