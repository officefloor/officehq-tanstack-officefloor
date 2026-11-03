import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useReader } from '../../api/readNow';
import { heldOn, type Hold } from '../people/usePeople';

/** A claim's status: DRAFT -> OPEN -> SETTLED; a clerk's settle above the clerk limit goes to PENDING
 *  instead, which a steward approves (SETTLED) or rejects (OPEN again); a DRAFT, OPEN or PENDING claim
 *  can become VOID; an open above the review threshold goes to REVIEW instead (counted in no total, voidable), which a
 *  steward reviews to OPEN; a DRAFT can be DELETED (counted nowhere) and restored to DRAFT on the day it was deleted. */
export type ClaimStatus = 'DRAFT' | 'REVIEW' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID' | 'DELETED';

/** A claim: its amount with two decimals, its date (yyyy-mm-dd), its note, its status, its number on
 *  the sheet (given when it is opened: 1, 2, ... or, under period numbering, <period>-<n> or X-<n>; null while a draft and once void) and the due fixed when it was
 *  settled (null unless settled while its grade had a rate: its due is then live, from the rates), and
 *  who opened it (null until it is opened), whether its person's last statement bound it, and the figure
 *  it was bound to (null when not bound, or bound with no figure: its due then stays live), and, for a
 *  correction (a negative amount), the number of the settled claim of the same person it corrects, and
 *  its due-by (yyyy-mm-dd), fixed when it was created from the terms in force that day (the sheet's today then): its person's own, else their grade's that day (else the
 *  sheet's terms), and its status log: a line per status it has had, oldest first, as
 *  "<STATUS> by <user> on <date> due=<due>" with the sheet's today and its due at that moment, and whether
 *  its last statement bound it to n/a (the rate that would apply to it had ended: its due stays n/a), and
 *  the date it is deferred until (null when it was never deferred: see `isDeferred`), and how many proofs it has (and how many of them are photos: the rest are receipts),
 *  and, for a claim split off another, the number of the claim it was split from (null otherwise), and the user
 *  whose open editor locks it for everyone else (null when it is not locked), and the last day (the sheet's today
 *  then) a reminder was written for it (null when never reminded), and its tags (lower-case, sorted and comma separated
 *  with no spaces; null when none), and the reason it was voided with (one of the sheet's void reasons; null when none
 *  was given), and its comments, oldest first, as "<user> on <date>: <text>" with the sheet's today when written, and the
 *  number of the settle batch (a bulk settle) that settled it (null when settled one at a time, or not settled), and its
 *  lines, oldest first, as "<label> <qty> x <unit>" (a claim with lines has their total as its amount), and its added
 *  number: claims added later have higher ones; a forward-only rate applies only to claims added after it (see `rateFor`), and whether it is review-late (REVIEW, and its
 *  review day — the sheet's today when it went to REVIEW — plus the sheet's review SLA is before the sheet's today). */
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
  proofs: number;
  photos: number;
  splitFrom: string | null;
  lateEntry: boolean;
  reminded: string | null;
  ref: string | null;
  tags: string | null;
  voidReason: string | null;
  waived: boolean;
  held: boolean;
  log: string[];
  comments: string[];
  lines: string[];
  lockedBy: string | null;
  batch: number | null;
  added: number;
  reviewLate: boolean;
};

/** Whether a claim is open: OPEN, or PENDING (handled like OPEN everywhere). */
export function isOpen(c: Claim): boolean {
  return c.status === 'OPEN' || c.status === 'PENDING';
}

/** A claim's fixed figure: the due frozen when it was settled, else the one its last statement bound
 *  it to ('n/a' when it bound it to n/a); null when its due is live. */
export function fixedDue(c: Claim): string | null {
  if (c.due !== null) return c.due;
  if (!isBound(c)) return null;
  return c.statedNa ? 'n/a' : c.statedDue;
}

/** Whether a claim is shown as deferred: the date it is deferred until is after the sheet's today, whatever
 *  its status now (a deferral is kept when the claim leaves OPEN). Only `isDeferred` acts on totals. */
export function shownDeferred(c: Claim, today: string | undefined): boolean {
  return c.deferredUntil !== null && today !== undefined && today < c.deferredUntil;
}

/** Whether a claim is deferred now: it is OPEN and the date it is deferred until is after the sheet's
 *  today. Until today reaches that date it counts in no total, is not overdue and cannot be settled. */
export function isDeferred(c: Claim, today: string | undefined): boolean {
  return c.status === 'OPEN' && c.deferredUntil !== null && today !== undefined && today < c.deferredUntil;
}

/** Whether a claim counts toward claimed and every live total, given the people's holds by person id
 *  that act on totals (only retirements': see holdsById) and the sheet's today: only open (OPEN or PENDING) and SETTLED
 *  claims do, and an open claim dated within one of those holds does not, unless a statement binds it, nor does a deferred
 *  one. A hold of its own or of its person's no longer takes it out (a hold acts only on pool cuts). */
export function counts(c: Claim, holds: ReadonlyMap<number, Hold[]>, today: string | undefined): boolean {
  return c.status === 'SETTLED'
    || (isOpen(c) && !isDeferred(c, today) && (isBound(c) || !heldOn(holds.get(c.personId), c.date)));
}

/** Whether a claim counts toward claimed: as counts, except that a hold (its own or one of its person's) does not
 *  take it out; given only the holds that do, by person id (see claimedHoldsById). */
export function countsClaimed(c: Claim, holds: ReadonlyMap<number, Hold[]>, today: string | undefined): boolean {
  return counts({ ...c, held: false }, holds, today);
}

/** A claim's status at the end of a day (yyyy-mm-dd), as its status log records it: the last line dated
 *  on or before it; null when the claim did not yet exist then. */
export function statusOn(c: Claim, day: string): ClaimStatus | null {
  let status: ClaimStatus | null = null;
  for (const line of c.log) {
    const m = /^(\w+) by .* on (\d{4}-\d{2}-\d{2}) due=/.exec(line);
    if (m && m[2] <= day) status = m[1] as ClaimStatus;
  }
  return status;
}

/** Whether a claim counts toward a period ending on a day: by the live rules (holds, deferral, binding),
 *  applied to the status it had at the end of that day; not when it did not yet exist then. A hold leaves
 *  out only a claim still open now: one since settled or voided counts by the status it had then. */
export function countsOn(c: Claim, day: string, holds: ReadonlyMap<number, Hold[]>, today: string | undefined): boolean {
  const status = statusOn(c, day);
  return status !== null && counts({ ...c, status, held: c.held && isOpen(c) }, isOpen(c) ? holds : new Map(), today);
}

/** Whether its person's last statement binds a claim now: it was stated (a PENDING claim is bound like any other). */
export function isBound(c: Claim): boolean {
  return c.stated;
}

export function claimsKey(sheetId: string) {
  return ['sheet', sheetId, 'claims'];
}

/** Every claim on the sheet, newest first. */
export function useClaims(sheetId: string) {
  const read = useReader();
  return useQuery({
    queryKey: read.key(claimsKey(sheetId)),
    queryFn: async () => {
      const res = await read.get<{
        claims: Omit<Claim, 'log' | 'comments' | 'lines' | 'lockedBy' | 'batch' | 'added' | 'reviewLate'>[];
        logs: Record<string, string[]>;
        locks: Record<string, string>;
        comments: Record<string, string[]>;
        lines: Record<string, string[]>;
        batches: Record<string, number>;
        added: Record<string, number>;
        reviewLate: Record<string, boolean>;
      }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`);
      return res.claims.map((c): Claim => ({
        ...c,
        log: res.logs[String(c.id)] ?? [],
        comments: res.comments[String(c.id)] ?? [],
        lines: res.lines[String(c.id)] ?? [],
        lockedBy: res.locks[String(c.id)] ?? null,
        batch: res.batches[String(c.id)] ?? null,
        added: res.added?.[String(c.id)] ?? 0,
        reviewLate: res.reviewLate?.[String(c.id)] ?? false,
      }));
    },
  });
}

/** Every tag ever used on the sheet, sorted. Kept under the claims' key, so whatever re-reads the claims
 *  re-reads these too. */
export function useTagsUsed(sheetId: string) {
  return useQuery({
    queryKey: [...claimsKey(sheetId), 'tags'],
    queryFn: async () =>
      (await getJson<{ tags: string[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`)).tags,
  });
}

/** The number of the sheet's last settle batch (null when there has been none). Kept under the claims' key,
 *  so whatever re-reads the claims re-reads it too. */
export function useLastBatch(sheetId: string) {
  return useQuery({
    queryKey: [...claimsKey(sheetId), 'last-batch'],
    queryFn: async () =>
      (await getJson<{ lastBatch: number | null }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`)).lastBatch,
  });
}

/** A claim's tags as the screen shows them: sorted, comma and space separated ('' when none). */
export function shownTags(tags: string | null): string {
  return tags === null ? '' : tags.split(',').join(', ');
}

/** The sum of the amounts of those of these claims that count toward claimed and correct no other claim (given the holds that keep claims out of claimed by person id and
 *  the sheet's today), with two decimals (added up in cents, so no rounding drift). */
export function claimed(claims: Claim[], holds: ReadonlyMap<number, Hold[]>, today: string | undefined): string {
  const cents = claims.filter((c) => c.corrects === null && countsClaimed(c, holds, today)).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
