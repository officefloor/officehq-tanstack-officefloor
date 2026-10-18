import { useSyncExternalStore } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson, postJson, putJson } from '../../api/http';
import { useCurrentSheet, useSheetToday } from '../session/session';
import { usePeople } from '../people/people';
import { useFloor, useLatePct, useRoundsHalfUp } from '../settings/settings';
import { claimDueCents } from '../rates/rates';

// Amounts travel as strings with two decimals ("13.50"); sums are added up in whole cents so no
// floating-point error shows.
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';
export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  /** Given when opened, the next on the sheet; null for a draft; a voided claim keeps it. */
  number: number | null;
  /** Fixed when settled (two decimals); null while live, or when no rate applied at settling. */
  settledDue: string | null;
  /** The user who opened it; null for a draft. */
  openedBy: string | null;
  /** For a correction (a negative amount), the number of the SETTLED claim it corrects; null otherwise. */
  corrects: number | null;
  /** Given instead of a number when opened while period numbering is on: <period name>-<n>, or X-<n> in no period. */
  periodNumber: string | null;
  /** Fixed when created: its date plus the terms of its person's grade in force on it (the sheet's when the grade has none). */
  dueBy: string;
  /** Its status log, oldest first: each status it has had, who made the change, the sheet's today then and its due
   *  as computed then (null when no rate applied). */
  log: ClaimLogLine[] };
export type ClaimLogLine = { status: ClaimStatus; by: string; date: string; due: string | null };
export type ClaimInput = { amount: string; date: string; note: string; corrects: string };

/** A number with up to two decimals; negative only for a correction. */
export const AMOUNT = /^-?\d+(\.\d{1,2})?$/;

/** A claim number. */
export const NUMBER = /^\d{1,9}$/;

/** Whether an amount is negative, which makes its claim a correction. */
export const isCorrection = (amount: string) => /^-/.test(amount.trim()) && /[1-9]/.test(amount);

/** A correction must name, by number, a SETTLED claim of the same person; any other claim names none. */
export function correctionError(claims: Claim[], personId: number, amount: string, corrects: string): string | undefined {
  const named = corrects.trim();
  if (named !== '' && !NUMBER.test(named)) return 'The claim corrected must be given by its number.';
  if (!isCorrection(amount)) {
    return named === '' ? undefined : 'Only a correction (a negative amount) names a claim it corrects.';
  }
  if (named === '') return 'A negative amount is a correction: name the settled claim it corrects.';
  const target = claims.find((c) => c.number === Number(named));
  if (!target || target.personId !== personId || target.status !== 'SETTLED') {
    return `A correction must name a settled claim of the same person; claim ${named} is not one.`;
  }
  return undefined;
}

const base = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}`;
export const claimsKey = (sheet: string) => ['sheet', sheet, 'claims'] as const;

/** The active sheet's claims, newest first. */
export function useClaims() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: claimsKey(sheet ?? ''),
    queryFn: () => getJson<{ claims: Claim[] }>(`${base(sheet!)}/claims`).then((r) => r.claims),
    enabled: !!sheet,
  });
}

export const addClaim = (sheet: string, user: string, personId: number, input: ClaimInput) =>
  postJson<Claim>(`${base(sheet)}/people/${personId}/claims`, input, user);

/** The quick-add bar: a DRAFT claim dated the sheet's today with an empty note, for the named person
 *  (created with grade A when not on the sheet). */
export const quickAddClaim = (sheet: string, user: string, input: { name: string; amount: string }) =>
  postJson<Claim>(`${base(sheet)}/quick-claims`, input, user);

export const updateClaim = (sheet: string, user: string, id: number, input: ClaimInput) =>
  putJson<Claim>(`${base(sheet)}/claims/${id}`, input, user);

// A claim starts DRAFT; it can be opened, then settled; a DRAFT or OPEN claim can be voided. A clerk
// settling a claim whose due is above the clerk limit sends it to PENDING instead: a steward approves it
// (SETTLED) or rejects it with a reason (back to OPEN). SETTLED and VOID are final. The server refuses a
// change not allowed; the screen refuses it first.
export type ClaimAction = 'open' | 'settle' | 'void' | 'approve' | 'reject';
export const CLAIM_ACTIONS: ClaimAction[] = ['open', 'settle', 'void', 'approve', 'reject'];
const ALLOWED: Record<ClaimAction, ClaimStatus[]> = {
  open: ['DRAFT'], settle: ['OPEN'], void: ['DRAFT', 'OPEN'], approve: ['PENDING'], reject: ['PENDING'],
};
export const canTake = (claim: Claim, action: ClaimAction) => ALLOWED[action].includes(claim.status);

/** Only a DRAFT can be edited; to change an OPEN claim, void it and add a new one. */
export const canEdit = (claim: Claim) => claim.status === 'DRAFT';

/** Four eyes: the user who opened a claim cannot settle it, nor approve it. */
export const selfSettle = (claim: Claim, user: string, action: ClaimAction) =>
  (action === 'settle' || action === 'approve') && claim.openedBy != null && claim.openedBy === user.trim();

/** Only a steward approves or rejects a PENDING claim. */
export const needsSteward = (action: ClaimAction) => action === 'approve' || action === 'reject';

/** Take an action on a claim; a rejection carries its reason. */
export const claimAction = (sheet: string, user: string, id: number, action: ClaimAction, reason = '') =>
  postJson<Claim>(`${base(sheet)}/claims/${id}/${action}`, action === 'reject' ? { reason } : {}, user);

/** A steward moves a DRAFT, OPEN or PENDING claim to another person on the sheet; it keeps its number and
 *  date. A SETTLED or VOID claim, and a correction (it names a settled claim of its own person), cannot move. */
export const canReassign = (claim: Claim) =>
  (claim.status === 'DRAFT' || live(claim)) && claim.corrects == null;

export const reassignClaim = (sheet: string, user: string, id: number, personId: number) =>
  postJson<Claim>(`${base(sheet)}/claims/${id}/reassign`, { personId }, user);

// The claims ticked for opening together, shared by every person's claim list and the bulk-open bar.
let selected: ReadonlySet<number> = new Set();
const listeners = new Set<() => void>();
const setSelected = (next: ReadonlySet<number>) => {
  selected = next;
  listeners.forEach((l) => l());
};
const subscribe = (l: () => void) => {
  listeners.add(l);
  return () => listeners.delete(l);
};

/** The selected claim ids, and a toggle and a clear for them. */
export function useSelectedClaims() {
  const ids = useSyncExternalStore(subscribe, () => selected);
  const toggle = (id: number, on: boolean) => {
    const next = new Set(selected);
    if (on) next.add(id);
    else next.delete(id);
    setSelected(next);
  };
  return { ids, toggle, clear: () => setSelected(new Set()) };
}

/** Open the selected claims together: each that can be opened is, numbered in list order; the rest are refused. */
export const bulkOpenClaims = (sheet: string, user: string, ids: number[]) =>
  postJson<{ opened: number; refused: number }>(`${base(sheet)}/bulk-open`, { ids }, user);

/** Whether a claim is live: OPEN, or PENDING (which counts as open in every total). */
export const live = (claim: Claim) => claim.status === 'OPEN' || claim.status === 'PENDING';

/** Whether a claim counts toward claimed, due-total and every total: only live (OPEN, PENDING) and SETTLED do. */
export const counts = (claim: Claim) => live(claim) || claim.status === 'SETTLED';

/** Whether a claim counts toward the live totals: as `counts`, except that the live claims of a person
 *  on hold count in none (their SETTLED claims still do). */
export function useCounts() {
  const held = new Set((usePeople().data ?? []).filter((p) => p.held).map((p) => p.id));
  return (claim: Claim) => counts(claim) && !(live(claim) && held.has(claim.personId));
}

const cents = (amount: string) => {
  const negative = amount.startsWith('-');
  const [whole, frac = ''] = (negative ? amount.slice(1) : amount).split('.');
  const n = Number(whole) * 100 + Number((frac + '00').slice(0, 2));
  return negative ? -n : n;
};

/** The amounts of the claims that count (by `counted`, from `useCounts`) added up, shown with two decimals. */
export function total(claims: Claim[], counted: (claim: Claim) => boolean): string {
  const sum = claims.filter(counted).reduce((n, c) => n + cents(c.amount), 0);
  const abs = Math.abs(sum);
  return `${sum < 0 ? '-' : ''}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`;
}

/** A date (yyyy-mm-dd) plus a number of days, as yyyy-mm-dd. */
export function addDays(date: string, days: number): string {
  const [y, m, d] = date.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10);
}

/** A claim's due-by, fixed when it was created. */
export function useDueBy() {
  return (claim: Claim) => claim.dueBy;
}

/** Whether a claim is overdue: it is live (OPEN or PENDING) and its due-by is before the sheet's today. */
export function useOverdue() {
  const dueBy = useDueBy();
  const today = useSheetToday();
  return (claim: Claim) => live(claim) && today !== undefined && dueBy(claim) < today;
}

/** Whole days from one date (yyyy-mm-dd) to another. */
export function daysBetween(from: string, to: string): number {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86400000);
}

/** A claim's due in cents (see `claimDueCents`) at a rate, with the sheet's floor and, while it is overdue,
 *  the late percent for each full 30 days past its due-by, rounded by the sheet's policy date. */
export function useClaimDue() {
  const halfUp = useRoundsHalfUp();
  const floor = useFloor();
  const latePct = useLatePct();
  const dueBy = useDueBy();
  const overdue = useOverdue();
  const today = useSheetToday();
  return (claim: Claim, rate: string | undefined) => {
    const spans = overdue(claim) && today !== undefined ? BigInt(Math.floor(daysBetween(dueBy(claim), today) / 30)) : 0n;
    return claimDueCents(claim, rate, floor, latePct, spans, halfUp(claim.date));
  };
}
