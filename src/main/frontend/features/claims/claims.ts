import { useQuery } from '@tanstack/react-query';
import { getJson, postJson, putJson } from '../../api/http';
import { useCurrentSheet, useSheetToday } from '../session/session';
import { usePeople } from '../people/people';
import { useFloor, useLatePct, useRoundsHalfUp, useTerms } from '../settings/settings';
import { claimDueCents } from '../rates/rates';

// Amounts travel as strings with two decimals ("13.50"); sums are added up in whole cents so no
// floating-point error shows.
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';
export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  /** Given when opened, the next on the sheet; null for a draft; a voided claim keeps it. */
  number: number | null;
  /** Fixed when settled (two decimals); null while live, or when no rate applied at settling. */
  settledDue: string | null;
  /** The user who opened it; null for a draft. */
  openedBy: string | null;
  /** For a correction (a negative amount), the number of the SETTLED claim it corrects; null otherwise. */
  corrects: number | null };
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

// A claim starts DRAFT; it can be opened, then settled; a DRAFT or OPEN claim can be voided.
// SETTLED and VOID are final. The server refuses a change not allowed; the screen refuses it first.
export type ClaimAction = 'open' | 'settle' | 'void';
export const CLAIM_ACTIONS: ClaimAction[] = ['open', 'settle', 'void'];
const ALLOWED: Record<ClaimAction, ClaimStatus[]> = { open: ['DRAFT'], settle: ['OPEN'], void: ['DRAFT', 'OPEN'] };
export const canTake = (claim: Claim, action: ClaimAction) => ALLOWED[action].includes(claim.status);

/** Only a DRAFT can be edited; to change an OPEN claim, void it and add a new one. */
export const canEdit = (claim: Claim) => claim.status === 'DRAFT';

/** Four eyes: the user who opened a claim cannot settle it. */
export const selfSettle = (claim: Claim, user: string, action: ClaimAction) =>
  action === 'settle' && claim.openedBy != null && claim.openedBy === user.trim();

export const claimAction = (sheet: string, user: string, id: number, action: ClaimAction) =>
  postJson<Claim>(`${base(sheet)}/claims/${id}/${action}`, {}, user);

/** Whether a claim counts toward claimed, due-total and every total: only OPEN and SETTLED do. */
export const counts = (claim: Claim) => claim.status === 'OPEN' || claim.status === 'SETTLED';

/** Whether a claim counts toward the live totals: as `counts`, except that the OPEN claims of a person
 *  on hold count in none (their SETTLED claims still do). */
export function useCounts() {
  const held = new Set((usePeople().data ?? []).filter((p) => p.held).map((p) => p.id));
  return (claim: Claim) => counts(claim) && !(claim.status === 'OPEN' && held.has(claim.personId));
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

/** A claim's due-by: its date plus the sheet's terms in force (days). */
export function useDueBy() {
  const terms = useTerms();
  return (claim: Claim) => addDays(claim.date, terms);
}

/** Whether a claim is overdue: it is OPEN and its due-by is before the sheet's today. */
export function useOverdue() {
  const dueBy = useDueBy();
  const today = useSheetToday();
  return (claim: Claim) => claim.status === 'OPEN' && today !== undefined && dueBy(claim) < today;
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
