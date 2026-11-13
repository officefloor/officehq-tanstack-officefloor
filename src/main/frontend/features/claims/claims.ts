import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/**
 * Where a claim is in its life: DRAFT -> OPEN -> SETTLED; a clerk's settle above the clerk limit makes it
 * PENDING, which a steward approves (SETTLED) or rejects (OPEN); DRAFT, OPEN or PENDING -> VOID.
 */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** Whether a claim is open: OPEN, or PENDING (handled like an open claim). */
export const isOpen = (c: Claim) => c.status === 'OPEN' || c.status === 'PENDING';

/** Whether a statement binds the claim now: it was stated and is not PENDING (a pending claim shows its live figure). */
export const bound = (c: Claim) => c.stated && c.status !== 'PENDING';

/**
 * A claim as the store holds it: the amount with two decimals, the date as yyyy-MM-dd; its number
 * is the sheet's next one when it was opened (1, 2, ...; under period numbering, the next within its period: P1-1, X-1), null while a draft (a voided claim keeps it); its due is the
 * one fixed when it was settled (two decimals), null while it is live; openedBy is the user who
 * opened it, null while a draft; stated is whether its person's last statement bound it to a figure,
 * statedDue that figure (two decimals; null: n/a) — it then shows and totals that, not its live figure;
 * corrects is, for a correction (a negative amount), the number of the settled claim it corrects, null otherwise;
 * dueBy (yyyy-MM-dd) is fixed when it was created: its date plus the terms of its person's grade in force on it.
 */
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
};

/**
 * Whether a claim counts toward claimed, due-total and every live total: only OPEN, PENDING and SETTLED do,
 * and an open (or pending) one not while its person is held.
 */
export const counts = (c: Claim, held = false) => c.status === 'SETTLED' || (isOpen(c) && !held);

/** The ids of the people on hold, whose open claims count in no live total. */
export const heldIds = (people: { id: number; held: boolean }[]) => new Set(people.filter((p) => p.held).map((p) => p.id));

/** The fields the claim form submits; a blank date is the sheet's today; corrects names, by number, the claim a correction corrects. */
export type ClaimInput = { amount: string; date: string; note: string; corrects: string };

/** 0.00 or more, with at most two decimals. */
export const AMOUNT = /^(\d+(\.\d{1,2})?|\.\d{1,2})$/;

/** An amount the claim form takes: negative for a correction, with at most two decimals. */
export const SIGNED_AMOUNT = /^-?(\d+(\.\d{1,2})?|\.\d{1,2})$/;

/** The query key for the sheet's claims; under sheetKey, so re-loading the sheet re-reads them. */
export const claimsKey = (sheet: string) => [...sheetKey(sheet), 'claims'] as const;

/** The sheet's claims, newest first. */
export function useClaims() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims`),
    enabled: !!sheet,
  });
}

/** The sum of the counted claims' amounts, with two decimals; added in cents so it is exact. `held` holds the people on hold. */
export function claimed(claims: Claim[], held: Set<number>): string {
  const cents = claims.filter((c) => counts(c, held.has(c.personId))).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
