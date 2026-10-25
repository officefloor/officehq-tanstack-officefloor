import { useQuery } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { settingOn, type Settings } from '../roles/roles';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** A claim's status: DRAFT to start, then OPEN and SETTLED; a clerk settling one above the clerk limit leaves it
 *  PENDING, until a steward approves it (SETTLED) or rejects it (OPEN again); a DRAFT, OPEN or PENDING claim can be VOID. A DRAFT can be DELETED (it counts nowhere) and restored to DRAFT on the day it was deleted. A claim whose due is above the review threshold in force on its date goes to REVIEW when
 *  opened (no number yet; it counts in no total, cannot be edited, settled or split, and can be VOID), until a steward reviews it (OPEN, numbered then). */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID' | 'DELETED' | 'REVIEW';

/** A claim as the store holds it: the amount with two decimals, the date (YYYY-MM-DD), the note, the status and
 *  the number given when it was opened (the sheet's next, or the next in its period: P1-1, X-1; null for a claim never opened; a voided claim keeps it) and the due fixed when it was
 *  settled (null when it has none: its due is live), and who opened it (null when never opened); whether its person's last statement binds it, and the
 *  figure it binds it to (null: n/a); the number of the settled claim of the same person it corrects (null when it is not
 *  a correction — a correction is a claim with a negative amount); the date its rounding goes by (a correction's is the
 *  date of the claim it corrects, else its own). */
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
  /** Who last edited it (its form or its amount entry); null when never edited. */
  editedBy: string | null;
  stated: boolean;
  statedDue: string | null;
  corrects: string | null;
  roundsOn: string;
  /** Its due-by (YYYY-MM-DD): the business day its terms (fixed when it was created) count to after its date then, past the holidays now. */
  dueBy: string;
  /** Its status log, oldest first: one line per status it has had, `<STATUS> by <user> on <date> due=<due>`. */
  log: string[];
  /** The date it is deferred until (YYYY-MM-DD), or null when it was never deferred. */
  deferredUntil: string | null;
  /** How many proofs it has: raised by one at a time (not on a SETTLED or VOID claim). */
  proofs: number;
  /** How many of its proofs are receipts (a proof added with no kind chosen is a receipt). */
  proofsReceipt: number;
  /** How many of its proofs are photos. */
  proofsPhoto: number;
  /** The number of the claim it was split off from, or null when it was not made by a split. */
  splitFrom: string | null;
  /** Whether it was added while the period its due-by falls in was soft-closed. */
  lateEntry: boolean;
  remindedOn: string | null;
  /** The sheet's today when it last went to REVIEW (YYYY-MM-DD), or null when it never did. */
  reviewOn: string | null;
  /** Whether a steward waived its late charge: it bears no late charge from then on, whatever its overdue state. */
  waived: boolean;
  /** Whether it is held (only an OPEN or PENDING claim can be); a held claim still counts in every total. */
  held: boolean;
  /** Its external reference, trimmed (unique on the sheet, ignoring case), or null when it has none. */
  ref: string | null;
  /** Its tags, trimmed, lower-cased and sorted, without repeats; empty when it has none. */
  tags: string[];
  /** Every tag it has ever carried, sorted: the sheet's tags are those of all its claims. */
  tagsUsed: string[];
  /** The reason it was voided with, from the sheet's list, or null when it was voided without one (or is not VOID). */
  voidReason: string | null;
  /** Its comments, oldest first: `<user> on <date>: <text>`, dated the sheet's today when each was written. */
  comments: string[];
  /** Its lines, in the order added: `<label> <qty> x <unit>`. With lines, its amount is their quantities times unit
   *  amounts, added up. */
  lines: string[];
  /** The number of the bulk settle batch that settled it (or left it PENDING); null when settled one at a time. */
  settleBatch: number | null;
  /** The people it is shared with, in the order added: each person's id and share (two decimals). */
  shares: { personId: number; share: string }[];
  /** The user its review is assigned to (null when none). */
  reviewAssigned: string | null;
};

/** Whether a claim is open: OPEN or PENDING (it goes overdue, and a hold takes it out of the live totals). */
export const isOpen = (c: Claim) => c.status === 'OPEN' || c.status === 'PENDING';

/** Whether a claim shows and totals the figure its person's last statement bound it to: it is bound (a PENDING
 *  claim like any other). */
export const isBound = (c: Claim) => c.stated;

/** Whether a claim is deferred on the sheet's today: it is OPEN and deferred until a date after it. Until then it
 *  counts in no total, is not overdue, cannot be settled and is left unbound by a statement. */
export const isDeferred = (c: Claim, today: string | undefined) =>
  c.status === 'OPEN' && c.deferredUntil !== null && today !== undefined && today < c.deferredUntil;

/** Whether a claim counts toward claimed and the totals: only OPEN, PENDING and SETTLED claims do, but not a
 *  deferred one. */
export const counts = (c: Claim, today: string | undefined) => (isOpen(c) || c.status === 'SETTLED') && !isDeferred(c, today);

/** A hold: from a date on, to a date (YYYY-MM-DD, inclusive; null: open-ended). */
export type Hold = { from: string; to: string | null };

/** Each person's holds, and the date they are retired from (null: not retired), by person id. */
export type Holds = Map<number, { holds: Hold[]; retiredFrom: string | null }>;

/** The holds and retirement dates of the people, by person id. */
export const holdsOf = (people: { id: number; holds: Hold[]; retiredFrom?: string | null }[] | undefined): Holds =>
  new Map((people ?? []).map((p) => [p.id, { holds: p.holds, retiredFrom: p.retiredFrom ?? null }]));

/** Whether the claim, when OPEN or PENDING, is out of the live totals on the sheet's today: its person's retirement
 *  date is reached (today on or after it). A hold (the claim's or its person's) does not take it out; it only
 *  denies its person a pool cut. */
const heldOn = (c: Claim, holds: Holds, today: string | undefined) => {
  const p = holds.get(c.personId);
  return !!p && p.retiredFrom !== null && today !== undefined && p.retiredFrom <= today;
};

/** Whether a claim counts toward the live totals: as `counts`, except its person's OPEN and PENDING claims once
 *  their retirement date is reached, unless a statement binds them. */
export const countsLive = (c: Claim, holds: Holds, today: string | undefined) => counts(c, today) && !(isOpen(c) && !isBound(c) && heldOn(c, holds, today));

/** The status a claim had at the end of a day (YYYY-MM-DD), as its status log records it (each line dated the sheet's
 *  today when the change was made); null when it had not been added by then. */
export function statusOn(c: Claim, day: string): ClaimStatus | null {
  let status: ClaimStatus | null = null;
  for (const line of c.log ?? []) {
    const m = /^(\S+) by .* on (\d{4}-\d{2}-\d{2}) due=\S+$/.exec(line);
    if (m && m[2] <= day) status = m[1] as ClaimStatus;
  }
  return status;
}

/** Whether a claim counts toward a period's live totals: as `countsLive`, taking the status it had at the end of
 *  the period's last day — not at all when it had not been added by then; a hold takes out only a claim that is
 *  still OPEN or PENDING and unbound now. */
export const countsAsOf = (c: Claim, holds: Holds, today: string | undefined, end: string) => {
  const status = statusOn(c, end);
  return status !== null && counts({ ...c, status }, today) && !(isOpen(c) && !isBound(c) && heldOn(c, holds, today));
};

export const claimsQueryKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

export const claimsPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/claims`;

export const personClaimsPath = (sheetId: string, personId: number) =>
  `/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/claims`;

/** Every claim on the sheet, newest first. */
export function useClaims() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: claimsQueryKey(sheet ?? ''),
    queryFn: () => getJson<Claim[]>(claimsPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** An amount ("12.50") in whole cents, so sums are exact. */
const cents = (amount: string) => Math.round(Number(amount) * 100);

/** Whether a claim counts toward claimed: as `countsLive`, except a hold (the claim's or its person's) does not take
 *  it out; only its person's retirement date being reached does. */
const countsClaimed = (c: Claim, holds: Holds, today: string | undefined) => {
  const p = holds.get(c.personId);
  const retired = !!p && p.retiredFrom !== null && today !== undefined && p.retiredFrom <= today;
  return counts(c, today) && !(isOpen(c) && !isBound(c) && retired);
};

/** A sum of the counted (OPEN, PENDING and SETTLED) claims' amounts, with two decimals; corrections (claims that
 *  correct another) and unbound OPEN claims of a person whose retirement date is reached are left out. A hold does
 *  not take a claim out of it. */
export function sumOf(claims: Claim[], held: Holds, today: string | undefined): string {
  return (claims.filter((c) => c.corrects === null && countsClaimed(c, held, today)).reduce((n, c) => n + cents(c.amount), 0) / 100).toFixed(2);
}

/** A claim's due-by (YYYY-MM-DD), fixed when it was created. */
export const dueByOf = (c: Claim): string => c.dueBy;

/** The date a claim's lateness counts from (YYYY-MM-DD): its due-by plus the grace days (setting grace-<grade>) in
 *  force on its date of its person's grade on its date (`grade`); its due-by when that grade has none. */
export function lateFromOf(c: Claim, grade: string, settings: Settings | undefined): string {
  const grace = settingOn(settings, `grace-${grade}`, c.date, '');
  if (grace === '') return dueByOf(c);
  return new Date(Date.parse(`${dueByOf(c)}T00:00:00Z`) + Number(grace) * 86_400_000).toISOString().slice(0, 10);
}

/** Whether a claim is overdue: it is OPEN or PENDING, not deferred, and its due-by plus any grace of its person's grade
 *  on its date (`grade`; see {@link lateFromOf}) is before the sheet's today. */
export const isOverdue = (c: Claim, today: string | undefined, grade: string, settings: Settings | undefined) =>
  isOpen(c) && !isDeferred(c, today) && today !== undefined && lateFromOf(c, grade, settings) < today;

/** Whether a claim is late in review: it is REVIEW and its review day plus the sheet's review SLA (setting
 *  review-sla; none: never late) is before the sheet's today. */
export function isReviewLate(c: Claim, today: string | undefined, settings: Settings | undefined): boolean {
  const sla = settings?.reviewSla ?? '';
  if (c.status !== 'REVIEW' || c.reviewOn === null || sla === '' || today === undefined) return false;
  return new Date(Date.parse(`${c.reviewOn}T00:00:00Z`) + Number(sla) * 86_400_000).toISOString().slice(0, 10) < today;
}

/** The id of the claim whose editor is open (search param `editClaim`), or '' for none. */
export const useEditingClaim = () => useSearchParam('editClaim', asString);

/** The ids of the selected claims (search param `selectedClaims`, comma-separated), a toggle for one, and a
 *  clear. The toggle works from the URL as it is when it runs, so quick clicks in a row each keep the others'. */
export function useSelectedClaims() {
  const [ids] = useSearchParam('selectedClaims', asIds);
  const navigate = useNavigate();
  const update = (change: (ids: Set<number>) => void) =>
    void navigate({
      to: '.',
      search: (prev: Record<string, unknown>) => {
        const next = { ...prev };
        const set = new Set(asIds(prev.selectedClaims));
        change(set);
        if (set.size === 0) delete next.selectedClaims;
        else next.selectedClaims = [...set].join(',');
        return next;
      },
      replace: true,
    });
  return {
    selected: new Set(ids),
    toggle: (id: number, on: boolean) => update((set) => (on ? set.add(id) : set.delete(id))),
    clear: () => update((set) => set.clear()),
  };
}

const asIds = (raw: unknown): number[] =>
  raw === undefined || raw === null || raw === ''
    ? []
    : String(raw).split(',').map(Number).filter((n) => Number.isInteger(n));
