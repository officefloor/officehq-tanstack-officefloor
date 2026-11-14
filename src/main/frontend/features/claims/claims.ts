import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { covers, liveHolds, retiredOn } from '../people/people';
import type { Hold } from '../people/people';
import { sheetKey, useCurrentSheet } from '../session/session';

/**
 * Where a claim is in its life: DRAFT -> OPEN -> SETTLED; a clerk's settle above the clerk limit makes it
 * PENDING, which a steward approves (SETTLED) or rejects (OPEN); DRAFT, OPEN or PENDING -> VOID; a DRAFT is deleted
 * (DELETED, counting nowhere) and restored to a DRAFT on the sheet's today it was deleted; an open whose due is above
 * the review threshold makes it REVIEW (counting nowhere, voidable), which a steward reviews (OPEN).
 */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID' | 'DELETED' | 'REVIEW';

/** Whether a claim is open: OPEN, or PENDING (handled like an open claim). */
export const isOpen = (c: Claim) => c.status === 'OPEN' || c.status === 'PENDING';

/**
 * Whether a claim is deferred on the sheet's `today`: OPEN, deferred until a date after it. A deferred claim counts
 * in no total, is not overdue, cannot be settled and is left unbound by a statement.
 */
export const deferred = (c: Claim, today: string) =>
  c.status === 'OPEN' && c.deferredUntil !== null && today !== '' && today < c.deferredUntil;

/** Whether a statement binds the claim now: it was stated (a PENDING claim too, showing the bound figure). */
export const bound = (c: Claim) => c.stated;

/**
 * A claim as the store holds it: the amount with two decimals, the date as yyyy-MM-dd; its number
 * is the sheet's next one when it was opened (1, 2, ...; under period numbering, the next within its period (the one containing its due-by): P1-1, X-1), null while a draft (a voided claim keeps it); its due is the
 * one fixed when it was settled (two decimals), null while it is live; openedBy is the user who
 * opened it, null while a draft; stated is whether its person's last statement bound it to a due,
 * statedDue that due as shown then, late charge and floor included (two decimals; null: n/a) — it then shows and
 * totals that, not its live due;
 * corrects is, for a correction (a negative amount), the number of the settled claim it corrects, null otherwise;
 * dueBy (yyyy-MM-dd) is fixed when it was created: the terms-th business day after its date for the terms of its person
 * (else their grade's, else the sheet's) in force on it, skipping Saturdays, Sundays and the sheet's holidays then on the list;
 * log is its status log, oldest first: each status it has had, the user who gave it, the sheet's today then and its due then;
 * deferredUntil (yyyy-MM-dd) is the date it was deferred until, null when it never was;
 * proofs is how many proofs it has been given (a claim whose due is above the proof threshold in force on its date — its
 * person's grade's own, else the sheet's — is
 * settled or approved only once it has one — while the sheet has proof kinds, one of each);
 * proofKinds is how many of its proofs are of each kind, receipt or photo (a proof added with no kind chosen is a receipt);
 * splitFrom is the number of the claim it was split off, null when it was not split off one;
 * lockedBy is the user whose open editor locks it for every other user, null when it is not locked;
 * lateEntry is whether it was added while the period containing its due-by was soft-closed;
 * reminded (yyyy-MM-dd) is the last day a reminder was written for it, null when it never was;
 * ref is its external reference (unique on the sheet, ignoring case), null when it has none;
 * tags are its tags (lower-cased, no repeats), sorted; empty when it has none;
 * voidReason is the reason from the sheet's void reasons it was voided with, null when none;
 * comments are its comments, oldest first: the user who wrote each, the sheet's today then and its text;
 * batch is the number of the bulk settle batch that settled it (SETTLED or PENDING), null when it was settled one at a time or never;
 * lines are its lines, in the order added: a DRAFT's label, quantity and unit amount; a claim with lines has their
 * quantities times their unit amounts, added up, as its amount;
 * waived is whether a steward waived its late charge: a waived claim bears none, whatever its overdue state;
 * held is whether the claim is held: a held open (or pending) claim counts as one a hold of its person covers;
 * shares are the other people it is shared with, in the order given: each holder's due-total counts their share of
 * its due, and its own person's the rest.
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
  /** How many of its proofs are of each kind (receipt, photo). */
  proofKinds: Record<string, number>;
  splitFrom: string | null;
  lockedBy: string | null;
  lateEntry: boolean;
  reminded: string | null;
  ref: string | null;
  tags: string[];
  voidReason: string | null;
  comments: ClaimComment[];
  batch: number | null;
  lines: ClaimLine[];
  /** When it was added, on the sequence rates are saved on: a forward-only rate applies only to claims added after it. */
  added: number;
  waived: boolean;
  held: boolean;
  shares: ClaimShare[];
  /** The user its review is assigned to (null: none); kept when it leaves REVIEW. */
  reviewAssigned: string | null;
};

/** A share of a claim: the id of the person holding it and the percent of the claim's due they hold (two decimals). */
export type ClaimShare = { personId: number; share: string };

/** A line of a claim: its label, its quantity (a whole number, at least 1) and its unit amount (two decimals). */
export type ClaimLine = { label: string; qty: number; unit: string };

/** A comment on a claim: the user who wrote it, the sheet's today then (yyyy-MM-dd) and its text. */
export type ClaimComment = { by: string; date: string; text: string };

/** A line of a claim's status log: the status, the user, the sheet's today (yyyy-MM-dd) and the due then (two decimals, or n/a). */
export type ClaimLogLine = { status: ClaimStatus; by: string; date: string; due: string };

/**
 * Whether a claim counts toward due-total and every live total but claimed (see countsClaimed): only OPEN, PENDING and SETTLED do,
 * and an open (or pending) one not when one of its person's live `holds` (their retirement) covers its date and no statement binds it
 * (a held claim, or one a hold of its person covers, still counts),
 * nor while it is deferred on the sheet's `today`.
 */
export const counts = (c: Claim, holds: Hold[], today: string) =>
  c.status === 'SETTLED' ||
  (isOpen(c) && !deferred(c, today) && (bound(c) || !holds.some((h) => covers(h, c.date))));

/** Whether a claim counts toward claimed: as {@link counts}, but a hold (the person's or the claim's) does not take it out. */
export const countsClaimed = (c: Claim, today: string) =>
  c.status === 'SETTLED' || (isOpen(c) && !deferred(c, today));

/** Each person's live holds on the sheet's `today`, by id: their open claims dated within one count in no live total. */
export const holdsById = (people: { id: number; holds: Hold[]; retiredFrom: string | null }[], today: string) =>
  new Map(people.map((p) => [p.id, liveHolds(p, today)]));

/** The fields the claim form submits; a blank date is the sheet's entry date, else its today; corrects names, by number, the claim a correction corrects; ref is its external reference (blank: none); tags its tags, comma separated (blank: none). */
export type ClaimInput = { amount: string; date: string; note: string; corrects: string; ref: string; tags: string };

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

/** The query key for the sheet's last settle batch; under sheetKey, so re-loading the sheet re-reads it. */
export const settleBatchesKey = (sheet: string) => [...sheetKey(sheet), 'settle-batches'] as const;

/** The sheet's last settle batch number (a bulk settle that settled at least one claim); null when none yet. */
export function useLastBatch() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: settleBatchesKey(sheet),
    queryFn: () => getJson<{ last: number | null }>(`/api/sheets/${encodeURIComponent(sheet)}/settle-batches`),
    enabled: !!sheet,
  });
}

/**
 * The sum of the counted claims' amounts (held ones too), leaving out corrections and the claims of people whose retirement
 * date has been reached on `today` (unless a statement binds them), with two decimals; added in cents so it is exact.
 */
export function claimed(claims: Claim[], today: string, people: { id: number; retiredFrom: string | null }[]): string {
  const retired = new Set(people.filter((p) => retiredOn(p, today)).map((p) => p.id));
  const cents = claims.filter((c) => !c.corrects && (!retired.has(c.personId) || bound(c)) && countsClaimed(c, today)).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
