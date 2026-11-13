import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { covers, liveHolds } from '../people/people';
import type { Hold } from '../people/people';
import { sheetKey, useCurrentSheet } from '../session/session';

/**
 * Where a claim is in its life: DRAFT -> OPEN -> SETTLED; a clerk's settle above the clerk limit makes it
 * PENDING, which a steward approves (SETTLED) or rejects (OPEN); DRAFT, OPEN or PENDING -> VOID.
 */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** Whether a claim is open: OPEN, or PENDING (handled like an open claim). */
export const isOpen = (c: Claim) => c.status === 'OPEN' || c.status === 'PENDING';

/**
 * Whether a claim is deferred on the sheet's `today`: OPEN, deferred until a date after it. A deferred claim counts
 * in no total, is not overdue, cannot be settled and is left unbound by a statement.
 */
export const deferred = (c: Claim, today: string) =>
  c.status === 'OPEN' && c.deferredUntil !== null && today !== '' && today < c.deferredUntil;

/** Whether a statement binds the claim now: it was stated and is not PENDING (a pending claim shows its live figure). */
export const bound = (c: Claim) => c.stated && c.status !== 'PENDING';

/**
 * A claim as the store holds it: the amount with two decimals, the date as yyyy-MM-dd; its number
 * is the sheet's next one when it was opened (1, 2, ...; under period numbering, the next within its period (the one containing its due-by): P1-1, X-1), null while a draft (a voided claim keeps it); its due is the
 * one fixed when it was settled (two decimals), null while it is live; openedBy is the user who
 * opened it, null while a draft; stated is whether its person's last statement bound it to a figure,
 * statedDue that figure (two decimals; null: n/a) — it then shows and totals that, not its live figure;
 * corrects is, for a correction (a negative amount), the number of the settled claim it corrects, null otherwise;
 * dueBy (yyyy-MM-dd) is fixed when it was created: the terms-th business day after its date for the terms of its person
 * (else their grade's, else the sheet's) in force on it, skipping Saturdays, Sundays and the sheet's holidays then on the list;
 * log is its status log, oldest first: each status it has had, the user who gave it, the sheet's today then and its due then;
 * deferredUntil (yyyy-MM-dd) is the date it was deferred until, null when it never was;
 * proofs is how many proofs it has been given (a claim whose due is above the proof threshold in force on its date is
 * settled or approved only once it has one);
 * splitFrom is the number of the claim it was split off, null when it was not split off one;
 * lockedBy is the user whose open editor locks it for every other user, null when it is not locked;
 * lateEntry is whether it was added while the period containing its due-by was soft-closed;
 * reminded (yyyy-MM-dd) is the last day a reminder was written for it, null when it never was.
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
  log: ClaimLogLine[];
  deferredUntil: string | null;
  proofs: number;
  splitFrom: string | null;
  lockedBy: string | null;
  lateEntry: boolean;
  reminded: string | null;
};

/** A line of a claim's status log: the status, the user, the sheet's today (yyyy-MM-dd) and the due then (two decimals, or n/a). */
export type ClaimLogLine = { status: ClaimStatus; by: string; date: string; due: string };

/**
 * Whether a claim counts toward claimed, due-total and every live total: only OPEN, PENDING and SETTLED do,
 * and an open (or pending) one not when one of its person's `holds` covers its date and no statement binds it,
 * nor while it is deferred on the sheet's `today`.
 */
export const counts = (c: Claim, holds: Hold[], today: string) =>
  c.status === 'SETTLED' ||
  (isOpen(c) && !deferred(c, today) && (bound(c) || !holds.some((h) => covers(h, c.date))));

/** Each person's live holds on the sheet's `today`, by id: their open claims dated within one count in no live total. */
export const holdsById = (people: { id: number; holds: Hold[]; retiredFrom: string | null }[], today: string) =>
  new Map(people.map((p) => [p.id, liveHolds(p, today)]));

/** The fields the claim form submits; a blank date is the sheet's entry date, else its today; corrects names, by number, the claim a correction corrects. */
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

/** The sum of the counted claims' amounts, with two decimals; added in cents so it is exact. `holds` holds each person's holds. */
export function claimed(claims: Claim[], holds: Map<number, Hold[]>, today: string): string {
  const cents = claims.filter((c) => counts(c, holds.get(c.personId) ?? [], today)).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
