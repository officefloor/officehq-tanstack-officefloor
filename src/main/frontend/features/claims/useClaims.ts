import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { heldOn, type Hold } from '../people/usePeople';

/** A claim's status: DRAFT -> OPEN -> SETTLED; a clerk's settle above the clerk limit goes to PENDING
 *  instead, which a steward approves (SETTLED) or rejects (OPEN again); a DRAFT, OPEN or PENDING claim
 *  can become VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** A claim: its amount with two decimals, its date (yyyy-mm-dd), its note, its status, its number on
 *  the sheet (given when it is opened: 1, 2, ... or, under period numbering, <period>-<n> or X-<n>; null while a draft and once void) and the due fixed when it was
 *  settled (null unless settled while its grade had a rate: its due is then live, from the rates), and
 *  who opened it (null until it is opened), whether its person's last statement bound it, and the figure
 *  it was bound to (null when not bound, or bound with no figure: its due then stays live), and, for a
 *  correction (a negative amount), the number of the settled claim of the same person it corrects, and
 *  its due-by (yyyy-mm-dd), fixed when it was created from the terms of its person's grade then (else the
 *  sheet's terms), and its status log: a line per status it has had, oldest first, as
 *  "<STATUS> by <user> on <date> due=<due>" with the sheet's today and its due at that moment, and whether
 *  its last statement bound it to n/a (the rate that would apply to it had ended: its due stays n/a), and
 *  the date it is deferred until (null when it was never deferred: see `isDeferred`). */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: string | null;
  due: string | null;
  openedBy: string | null;
  stated: boolean;
  statedDue: string | null;
  corrects: string | null;
  dueBy: string;
  statedNa: boolean;
  deferredUntil: string | null;
  log: string[];
};

/** Whether a claim is open: OPEN, or PENDING (handled like OPEN everywhere but its statement binding). */
export function isOpen(c: Claim): boolean {
  return c.status === 'OPEN' || c.status === 'PENDING';
}

/** A claim's fixed figure: the due frozen when it was settled, else the one its last statement bound
 *  it to (a PENDING claim is not bound; 'n/a' when it bound it to n/a); null when its due is live. */
export function fixedDue(c: Claim): string | null {
  if (c.due !== null) return c.due;
  if (!isBound(c)) return null;
  return c.statedNa ? 'n/a' : c.statedDue;
}

/** Whether a claim is deferred now: it is OPEN and the date it is deferred until is after the sheet's
 *  today. Until today reaches that date it counts in no total, is not overdue and cannot be settled. */
export function isDeferred(c: Claim, today: string | undefined): boolean {
  return c.status === 'OPEN' && c.deferredUntil !== null && today !== undefined && today < c.deferredUntil;
}

/** Whether a claim counts toward claimed and every live total, given the people's holds by person id
 *  and the sheet's today: only open (OPEN or PENDING) and SETTLED claims do, and an open claim dated within
 *  one of its person's holds does not, unless a statement binds it, nor does a deferred one. */
export function counts(c: Claim, holds: ReadonlyMap<number, Hold[]>, today: string | undefined): boolean {
  return c.status === 'SETTLED'
    || (isOpen(c) && !isDeferred(c, today) && (isBound(c) || !heldOn(holds.get(c.personId), c.date)));
}

/** Whether its person's last statement binds a claim now: it was stated and is not PENDING. */
export function isBound(c: Claim): boolean {
  return c.stated && c.status !== 'PENDING';
}

export function claimsKey(sheetId: string) {
  return ['sheet', sheetId, 'claims'];
}

/** Every claim on the sheet, newest first. */
export function useClaims(sheetId: string) {
  return useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: async () => {
      const res = await getJson<{ claims: Omit<Claim, 'log'>[]; logs: Record<string, string[]> }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims`,
      );
      return res.claims.map((c): Claim => ({ ...c, log: res.logs[String(c.id)] ?? [] }));
    },
  });
}

/** The sum of the amounts of those of these claims that count (given the people's holds by person id and
 *  the sheet's today), with two decimals (added up in cents, so no rounding drift). */
export function claimed(claims: Claim[], holds: ReadonlyMap<number, Hold[]>, today: string | undefined): string {
  const cents = claims.filter((c) => counts(c, holds, today)).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
