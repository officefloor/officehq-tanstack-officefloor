import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import type { Person } from '../people/people';

/** Where a claim is in its life: DRAFT, then OPEN, then SETTLED; a clerk's settle above the clerk limit leaves it
 *  PENDING until a steward approves (SETTLED) or rejects (OPEN) it; a DRAFT, OPEN or PENDING claim may be VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** A person's claim: the amount with two decimals, the date (yyyy-mm-dd), a note (may be empty), its status,
 *  its number (sheet-wide digits, or <period name>-<n> / X-<n> when numbered within its period; null while a draft; a voided claim keeps the one it had) and, the due fixed when it was settled
 *  (null when not settled, or its grade had no rate then: its due follows the rates), and who opened it
 *  (null while a draft), whether its person's last statement bound it (it counted then) and the figure it bound
 *  it to (null when not bound, or n/a then: its due stays live), and for a correction (a negative amount) the
 *  number of the SETTLED claim of the same person it corrects (null otherwise), and its due-by (yyyy-mm-dd), fixed
 *  when it was created from the terms of its person's grade in force on its date (else the sheet's terms). */
export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  number: string | null; settledDue: string | null; openedBy: string | null; stated: boolean;
  statedDue: string | null; corrects: string | null; dueBy: string };

/** Whether a claim counts toward claimed, due-total and every total: only OPEN, PENDING and SETTLED ones do. */
export function counts(c: Claim): boolean {
  return c.status === 'OPEN' || c.status === 'PENDING' || c.status === 'SETTLED';
}

/** Whether a claim is still to be settled: OPEN or PENDING (it goes overdue, and a hold takes it out of the totals). */
export function unsettled(c: Claim): boolean {
  return c.status === 'OPEN' || c.status === 'PENDING';
}

/** The claims that count in the live totals among these: those that count, except the OPEN or PENDING claims of a person
 *  (among the people given) who is on hold; their SETTLED claims still count. */
export function live(claims: Claim[], people: Person[]): Claim[] {
  const held = new Set(people.filter((p) => p.held).map((p) => p.id));
  return claims.filter((c) => counts(c) && !(unsettled(c) && held.has(c.personId)));
}

/** A claim's due-by (yyyy-mm-dd), as fixed when it was created. */
export function dueBy(claim: Claim): string {
  return claim.dueBy;
}

/** Whether a claim is overdue: it is OPEN or PENDING and its due-by is before the sheet's today. */
export function overdue(claim: Claim, today: string): boolean {
  return unsettled(claim) && today !== '' && dueBy(claim) < today;
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
