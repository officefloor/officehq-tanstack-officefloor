import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { heldOn } from '../people/people';
import type { Hold } from '../people/people';

/**
 * A claim's status: DRAFT when made, then OPEN, then SETTLED; a clerk's settle above the clerk
 * limit makes it PENDING until a steward approves it (SETTLED) or rejects it (OPEN again). A DRAFT,
 * OPEN or PENDING claim may be VOID; a SETTLED one too, by a steward within 7 days of its settlement.
 * A claim whose due is above the review threshold in force on its date goes to REVIEW instead of OPEN when
 * it is opened, unnumbered and counting in no total, until a steward reviews it (OPEN); it
 * may be VOID too. A DRAFT may be DELETED (it counts nowhere), and restored to a DRAFT on the sheet's today it was deleted.
 */
export type ClaimStatus = 'DRAFT' | 'REVIEW' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID' | 'DELETED';

/**
 * A claim: its amount with two decimals, its date (yyyy-MM-dd), its note, its status and its
 * number on the sheet (given when it is settled or approved; '' until then), and its due as
 * fixed when it was settled ('' while its due is live: unsettled, or settled with no rate), and
 * who opened it ('' until it is opened); whether its person's last statement bound it, and the
 * figure it was bound to ('' when not stated; n/a when it had none); and, for a correction (a
 * negative amount), the number of the settled claim it corrects ('' for any other claim); and the
 * date (yyyy-MM-dd) its figures round by (a correction's is the date of the claim it corrects).
 */
export type Claim = {
  id: string;
  personId: string;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: string;
  due: string;
  openedBy: string;
  stated: boolean;
  statedDue: string;
  /** The late charge its person's last statement bound it to, with its figure ('' when none is bound: it follows the sheet's today). */
  statedLate: string;
  corrects: string;
  roundDate: string;
  /** Its due-by (yyyy-MM-dd), fixed when it was made: the terms-th business day after its date (skipping weekends and the sheet's holidays then), its terms its person's own, else its grade's then. */
  dueBy: string;
  /**
   * Its status log, oldest first: one line per status it has had, as
   * `<STATUS> by <user> on <date> due=<due>` (the sheet's today and its due at that moment).
   */
  log: string[];
  /** The date (yyyy-MM-dd) it is deferred until; '' when it was never deferred. */
  deferredUntil: string;
  /** How many proofs it has had added. */
  proofs: number;
  /** How many of its proofs are receipts. */
  proofReceipts: number;
  /** How many of its proofs are photos. */
  proofPhotos: number;
  /** The number of the claim it was split off; '' when it was not split off one. */
  splitFrom: string;
  /** The user whose open editor locks it on the sheet's today; '' when no one's does. */
  lockedBy: string;
  /** Whether it was added while its period was soft-closed. */
  lateEntry: boolean;
  /** The last sheet's today (yyyy-MM-dd) a reminder was written for it; '' when it was never reminded. */
  remindedOn: string;
  /** Whether a steward waived its late charge: it bears none from then on. */
  waived: boolean;
  /** Whether it is held: it still counts in every total; a hold only matters to pool cuts. */
  held: boolean;
  /** Its external reference, unique on the sheet regardless of case; '' when it has none. */
  ref: string;
  /** Its tags: trimmed, lower-cased, without repeats, sorted; empty when it has none. */
  tags: string[];
  /** The reason it was voided with, from the sheet's list; '' when none. */
  voidReason: string;
  /** Its comments, oldest first, as `<user> on <date>: <text>` (the sheet's today when written). */
  comments: string[];
  /** The number of the bulk settle batch that settled it; '' when none did (settled one at a time, or not settled). */
  batch: string;
  /** Its lines, oldest first, as `<label> <qty> x <unit>`; while it has any, its amount is their total. */
  lines: string[];
  /** The sheet's today (yyyy-MM-dd) when it went to REVIEW; '' when it never did. */
  reviewOn: string;
  /**
   * Its shares with other people on the sheet, oldest first: each holder's person id and their percent
   * (two decimals). A holder's part is its amount times their share / 100 times the rate of the holder's
   * own grade in force on its date / 100, rounded like the claim (none with no rate); a settled claim's parts
   * stay frozen at its due times their share / 100, rounded half to even. Its own person keeps the rest.
   */
  shares: ClaimShare[];
  /** The user a steward assigned it to review while in REVIEW; '' when none. Kept once it leaves REVIEW. */
  reviewUser: string;
  /** The sheet's today (yyyy-MM-dd) when it last went to PENDING; '' when it never did. */
  pendingOn: string;
};

/** A holder's share of a claim: their person id and their percent, two decimals. */
export type ClaimShare = { personId: string; share: string };

/** Whether a claim is a correction: it has no floor and no late charge. */
export const isCorrection = (c: Claim) => c.corrects !== '';

/** Whether a claim is open: OPEN, or PENDING a steward's approval. */
export const isOpen = (c: Claim) => c.status === 'OPEN' || c.status === 'PENDING';

/**
 * Whether a claim is late pending: it is PENDING and its pending day plus the pending SLA in days
 * ('' for none: never late) is before the sheet's today.
 */
export function isPendingLate(c: Claim, today: string, pendingSla: string): boolean {
  if (c.status !== 'PENDING' || c.pendingOn === '' || pendingSla === '' || today === '') return false;
  const [y, m, d] = c.pendingOn.split('-').map(Number);
  const due = new Date(Date.UTC(y, m - 1, d + Number(pendingSla))).toISOString().slice(0, 10);
  return due < today;
}

/**
 * Whether a claim is late for review: it is in REVIEW and its review day plus the review SLA in days
 * ('' for none: never late) is before the sheet's today.
 */
export function isReviewLate(c: Claim, today: string, reviewSla: string): boolean {
  if (c.status !== 'REVIEW' || c.reviewOn === '' || reviewSla === '' || today === '') return false;
  const [y, m, d] = c.reviewOn.split('-').map(Number);
  const due = new Date(Date.UTC(y, m - 1, d + Number(reviewSla))).toISOString().slice(0, 10);
  return due < today;
}

/**
 * Whether a claim's stated figure is in force: its person's last statement bound it (OPEN or
 * PENDING alike).
 */
export const isBound = (c: Claim) => c.stated;

/**
 * Whether a claim is deferred on the sheet's `today`: not bound by a statement, and deferred until a
 * date after today. From that date on it is a claim like any other. While a statement binds it, its
 * deferral waits until the binding ends, and then takes effect whatever its status since (one settled
 * while bound drops out of the totals until that date too).
 */
export const isDeferred = (c: Claim, today: string) =>
  !isBound(c) && c.deferredUntil !== '' && today !== '' && today < c.deferredUntil;

/**
 * Whether the claim's row shows it deferred: it was deferred until a date after the sheet's
 * `today`, whatever its status since (a deferred claim voided stays shown deferred), and no
 * statement binds it. Only `isDeferred` decides what counts.
 */
export const showsDeferred = (c: Claim, today: string) =>
  !isBound(c) && c.deferredUntil !== '' && today !== '' && today < c.deferredUntil;

/**
 * Whether a claim counts toward claimed, due-total and every total: only OPEN, PENDING and SETTLED
 * do, and an OPEN or PENDING one not when dated within one of `holds` (a retired person's, see
 * `liveHolds`; a hold of its own or its person's does not take it out), unless a statement binds it;
 * never one deferred on `today`.
 */
export const counts = (c: Claim, holds: Hold[], today: string) =>
  !isDeferred(c, today) && (c.status === 'SETTLED' || (isOpen(c) && (isBound(c) || !heldOn(holds, c.date))));

/**
 * Whether a claim's amount counts toward claimed and the claimed-total: as `counts`, but a hold
 * (its own or its person's) does not take it out; only `holds` given (a retired person's) do.
 */
export const countsClaimed = (c: Claim, holds: Hold[], today: string) =>
  !isDeferred(c, today) && (c.status === 'SETTLED' || (isOpen(c) && (isBound(c) || !heldOn(holds, c.date))));

/**
 * A claim's status at the end of `day` (yyyy-MM-dd), as its log records it: the status of its last
 * line dated on or before it; null when it had none then (it was added after).
 */
export function statusOn(c: Claim, day: string): ClaimStatus | null {
  let status: ClaimStatus | null = null;
  for (const line of c.log) {
    const m = /^(\w+) by .* on (\d{4}-\d{2}-\d{2}) due=\S*$/.exec(line);
    if (m && m[2] <= day) status = m[1] as ClaimStatus;
  }
  return status;
}

/**
 * Whether a claim counted at the end of `day`: as `counts`, with the status its log records then
 * (never when it was added after), its statement binding and deferral as they are on `today`.
 */
export const countedOn = (c: Claim, day: string, holds: Hold[], today: string) => {
  const status = statusOn(c, day);
  if (status === null || isDeferred(c, today)) return false;
  return status === 'SETTLED' || ((status === 'OPEN' || status === 'PENDING') && (isBound(c) || !heldOn(holds, c.date)));
};

export const claimsKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

export const claimsQuery = (sheetId: string) => ({
  queryKey: claimsKey(sheetId),
  queryFn: async () =>
    (await getJson<{ claims: Claim[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`)).claims,
});

export const tagsKey = (sheetId: string) => [...claimsKey(sheetId), 'tags'] as const;

/** Every tag ever used on the sheet, sorted (re-read whenever its claims are). */
export function useTagsUsed(sheetId: string) {
  return useQuery({
    queryKey: tagsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ tags: string[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`)).tags,
  });
}

export const lastBatchKey = (sheetId: string) => [...claimsKey(sheetId), 'last-batch'] as const;

/** The number of the sheet's last bulk settle batch; '' when there has been none (re-read whenever its claims are). */
export function useLastBatch(sheetId: string) {
  return useQuery({
    queryKey: lastBatchKey(sheetId),
    queryFn: async () =>
      (await getJson<{ lastBatch: string }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`)).lastBatch,
  });
}

/** The sheet's claims, newest first. */
export function useClaims(sheetId: string) {
  return useQuery(claimsQuery(sheetId));
}

/** An amount as a claim takes it: 0.00 or more, with up to two decimals. */
export const AMOUNT = /^\d+(\.\d{1,2})?$/;

/** An amount as the claim form takes it: up to two decimals, negative for a correction. */
export const CLAIM_AMOUNT = /^-?\d+(\.\d{1,2})?$/;

/**
 * A claim number, as a correction names the claim it corrects: a sheet-wide number (one given
 * before numbering restarted each fiscal year), a number within a fiscal year (<FY>-<n>), or a number
 * within a period (<period name>-<n>, X-<n> for a claim in no period).
 */
export const CLAIM_NUMBER = /^(\d+|.+-\d+)$/;

/**
 * The amounts of the claims that count (OPEN, PENDING and SETTLED, held or not, but not unbound open ones of a
 * retired person: `holds` tells by person id (see `retiredHolds`), nor ones deferred on `today`), leaving out corrections, added up, in whole cents so no rounding creeps in, shown with two
 * decimals.
 */
export function sumAmounts(claims: Claim[], holds: (personId: string) => Hold[], today: string): string {
  const cents = claims.filter((c) => !isCorrection(c) && countsClaimed(c, holds(c.personId), today)).reduce((n, c) => n + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
