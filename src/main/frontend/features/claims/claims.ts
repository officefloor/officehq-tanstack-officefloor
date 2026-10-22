import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { heldOn, retiredOn } from '../people/people';
import type { Person } from '../people/people';

/** Where a claim is in its life: DRAFT, then OPEN, then SETTLED; a clerk's settle above the clerk limit leaves it
 *  PENDING until a steward approves (SETTLED) or rejects (OPEN) it; a DRAFT, OPEN or PENDING claim may be VOID, and a steward may void a SETTLED one within 7 days of its settlement. */
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
 *  OPEN claim deferred until after it: it counts in no total, is not overdue and cannot be settled), and its proofs
 *  count (raised one at a time, not on a SETTLED or VOID claim; one whose due is above the proof threshold in force
 *  on its date cannot be settled or approved while it is 0 — the server refuses it), and the number
 *  of the claim it was split from (null when it was not split off one). */
export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  number: string | null; settledDue: string | null; openedBy: string | null; stated: boolean;
  statedDue: string | null; corrects: string | null; dueBy: string; log: string[];
  deferredUntil: string | null; deferred: boolean; proofs: number; splitFrom: string | null;
  lateEntry: boolean };

/** Whether a claim counts toward claimed, due-total and every total: only OPEN, PENDING and SETTLED ones do, except
 *  one deferred on the sheet's today. */
export function counts(c: Claim): boolean {
  return (c.status === 'OPEN' || c.status === 'PENDING' || c.status === 'SETTLED') && !c.deferred;
}

/** Whether a claim is still to be settled: OPEN or PENDING (it goes overdue). */
export function unsettled(c: Claim): boolean {
  return c.status === 'OPEN' || c.status === 'PENDING';
}

/** Whether a claim is left out of the live totals by its person (among those given by id): an OPEN claim dated
 *  within one of their holds, or an OPEN or PENDING one once their retirement date is reached on the sheet's today
 *  (yyyy-mm-dd), either not bound by a statement. */
function leftOut(c: Claim, byId: Map<number, Person>, today: string): boolean {
  if (c.stated) return false;
  const person = byId.get(c.personId);
  return (c.status === 'OPEN' && heldOn(person, c.date))
    || (unsettled(c) && retiredOn(person, today));
}

/** The claims that count in the live totals among these on the sheet's today (yyyy-mm-dd): those that count, except
 *  those their person (among the people given) leaves out (see leftOut); their other claims still count. */
export function live(claims: Claim[], people: Person[], today: string): Claim[] {
  const byId = new Map(people.map((p) => [p.id, p]));
  return claims.filter((c) => counts(c) && !leftOut(c, byId, today));
}

/** Whether a person is active on the sheet's today (yyyy-mm-dd; '' while loading: not active): they have an OPEN or
 *  PENDING claim that counts in the live totals (see live) dated within the 30 days up to and including it. */
export function active(person: Person, claims: Claim[], today: string): boolean {
  if (today === '') return false;
  const since = new Date(Date.parse(`${today}T00:00:00Z`) - 29 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  return live(claims.filter((c) => c.personId === person.id), [person], today)
    .some((c) => unsettled(c) && c.date >= since && c.date <= today);
}

/** A claim's status at the end of the day given (yyyy-mm-dd), as its status log records it: the last status given on
 *  or before that day (one given while the sheet had no today counts as given by then); null when every status was
 *  given after it (it was added later). A claim with no log has its status now. */
export function statusAt(c: Claim, day: string): ClaimStatus | null {
  if (c.log.length === 0) return c.status;
  let status: ClaimStatus | null = null;
  for (const line of c.log) {
    const m = /^(\S+) by .* on (\S*) due=\S+$/.exec(line);
    if (m && (m[2] === '' || m[2] <= day)) status = m[1] as ClaimStatus;
  }
  return status;
}

/** The claims that count in a period's due-totals among these: those whose status at the end of the day given (see
 *  statusAt) is OPEN, PENDING or SETTLED, left out as live leaves them out on the sheet's today (yyyy-mm-dd):
 *  deferred, or left out by their person (among the people given; see leftOut). */
export function countedAt(claims: Claim[], people: Person[], day: string, today: string): Claim[] {
  const byId = new Map(people.map((p) => [p.id, p]));
  return claims.filter((c) => {
    const at = statusAt(c, day);
    return (at === 'OPEN' || at === 'PENDING' || at === 'SETTLED') && !c.deferred && !leftOut(c, byId, today);
  });
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
