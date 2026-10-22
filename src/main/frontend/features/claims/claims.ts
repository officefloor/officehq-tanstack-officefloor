import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { heldOn } from '../people/people';
import type { Person } from '../people/people';

/** Where a claim is in its life: DRAFT, then OPEN, then SETTLED; a clerk's settle above the clerk limit leaves it
 *  PENDING until a steward approves (SETTLED) or rejects (OPEN) it; a DRAFT, OPEN or PENDING claim may be VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** Every claim status, in life order: the statuses claims are counted by, each shown even when no claim has it. */
export const STATUSES: readonly ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'];

/** A person's claim: the amount with two decimals, the date (yyyy-mm-dd), a note (may be empty), its status,
 *  its number (sheet-wide digits, or <period name>-<n> / X-<n> when numbered within its period; null while a draft; a voided claim keeps the one it had) and, the due fixed when it was settled
 *  (null when not settled, or its grade had no rate then: its due follows the rates), and who opened it
 *  (null while a draft), whether its person's last statement bound it (it counted then) and the figure it bound
 *  it to (null when not bound, or n/a then: its due stays live), and for a correction (a negative amount) the
 *  number of the SETTLED claim of the same person it corrects (null otherwise), and its due-by (yyyy-mm-dd), fixed
 *  when it was created from the terms of its person's grade in force on its date (else the sheet's terms), and its
 *  status log: one line per status it has had, oldest first, as <STATUS> by <user> on <date> due=<due>, and the date
 *  it is deferred until (yyyy-mm-dd; null when never deferred) and whether it is deferred on the sheet's today (an
 *  OPEN claim deferred until after it: it counts in no total, is not overdue and cannot be settled). */
export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  number: string | null; settledDue: string | null; openedBy: string | null; stated: boolean;
  statedDue: string | null; corrects: string | null; dueBy: string; log: string[];
  deferredUntil: string | null; deferred: boolean };

/** Whether a claim counts toward claimed, due-total and every total: only OPEN, PENDING and SETTLED ones do, except
 *  one deferred on the sheet's today. */
export function counts(c: Claim): boolean {
  return (c.status === 'OPEN' || c.status === 'PENDING' || c.status === 'SETTLED') && !c.deferred;
}

/** Whether a claim is still to be settled: OPEN or PENDING (it goes overdue). */
export function unsettled(c: Claim): boolean {
  return c.status === 'OPEN' || c.status === 'PENDING';
}

/** The claims that count in the live totals among these: those that count, except an OPEN claim dated within a hold
 *  of its person (among the people given) and not bound by a statement; their other claims still count. */
export function live(claims: Claim[], people: Person[]): Claim[] {
  const byId = new Map(people.map((p) => [p.id, p]));
  return claims.filter((c) => counts(c) && !(c.status === 'OPEN' && !c.stated && heldOn(byId.get(c.personId), c.date)));
}

/** A claim's due-by (yyyy-mm-dd), as fixed when it was created. */
export function dueBy(claim: Claim): string {
  return claim.dueBy;
}

/** Whether a claim is overdue: it is OPEN or PENDING, not deferred, and its due-by is before the sheet's today. */
export function overdue(claim: Claim, today: string): boolean {
  return unsettled(claim) && !claim.deferred && today !== '' && dueBy(claim) < today;
}

export const claimsKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

/** Every claim on the sheet, newest first. */
export function useClaims(sheetId: string) {
  return useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`),
  });
}

/** The id of the claim whose editor is open ('' when each person's claim form adds a new claim). */
export function useEditingClaim() {
  return useSearchParam('editClaim', asString);
}

/** The amounts of the claims that count (OPEN, PENDING and SETTLED) added up exactly (in cents) and shown
 *  with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((total, c) => total + Math.round(Number(c.amount) * 100), 0);
  const abs = Math.abs(cents);
  return `${cents < 0 ? '-' : ''}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`;
}
