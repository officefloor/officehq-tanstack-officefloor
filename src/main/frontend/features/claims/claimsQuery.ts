import { getJson } from '../../api/http';
import { heldOn, type Person } from '../people/peopleQuery';
import { sheetKey } from '../sheet/sheetQuery';

export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** An open claim: OPEN, or PENDING (settled by a clerk above the clerk limit, awaiting a steward), handled like OPEN in every total. */
export const isOpen = (c: Claim) => c.status === 'OPEN' || c.status === 'PENDING';

/**
 * `amount` has two decimals ("12.50"); `date` is yyyy-mm-dd; `number` is given on opening and kept when voided (null for a draft);
 * `settledDue` is the due fixed on settling (null when not settled, or settled while no rate applied on the claim's date);
 * `openedBy` is the user who opened it (null when never opened), who cannot settle it;
 * `stated` is whether its person's last statement bound it, at `statedDue` (null when it had no figure then) — a PENDING claim is not bound while PENDING;
 * a negative `amount` is a correction, and `corrects` is the number of the settled claim it corrects (null for any other claim);
 * `dueBy` (yyyy-mm-dd) is fixed when the claim is made, from its person's own terms, else the terms of their grade on its date (moved past the sheet's holidays as they then were), and kept when they or its date change;
 * `log` is its status log, oldest first, one line per status it has had: "<STATUS> by <user> on <date> due=<due>";
 * `deferredUntil` (yyyy-mm-dd) is the date an OPEN claim was deferred to (null when never deferred), see `isDeferred`;
 * `proofs` is how many proofs it has been given: one above the sheet's proof threshold is settled or approved only with one;
 * `splitFrom` is the number of the claim it was split off (null for any other claim);
 * `lockedBy` is the user whose open editor locks it for everyone else (null when not locked);
 * `lateEntry` is whether it was added while the period its due-by falls in was soft-closed;
 * `remindedOn` (yyyy-mm-dd) is the last sheet's today a reminder was written for it (null when never reminded).
 */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: string | null;
  settledDue: string | null;
  openedBy: string | null;
  stated: boolean;
  statedDue: string | null;
  corrects: string | null;
  dueBy: string;
  log: string[];
  deferredUntil: string | null;
  proofs: number;
  splitFrom: string | null;
  lockedBy: string | null;
  lateEntry: boolean;
  remindedOn: string | null;
};

/** A correction: its due is its amount times the rate, with no floor and no late charge. */
export const isCorrection = (c: Claim) => Number(c.amount) < 0;

/**
 * An OPEN claim deferred to a date the sheet's `today` has not reached: it counts in no total, is not overdue and
 * cannot be settled. From that day on it is an OPEN claim like any other.
 */
export const isDeferred = (c: Claim, today: string) =>
  c.status === 'OPEN' && c.deferredUntil !== null && (today === '' || today < c.deferredUntil);

/**
 * Only open (OPEN or PENDING) and SETTLED claims count toward claimed, due and every total; drafts and voided
 * ones do not, nor do a person's open claims dated within one of their holds, or once the sheet's `today` reaches
 * their retirement date, unless a statement bound them, nor an OPEN claim deferred on the sheet's `today`.
 */
export const counts = (c: Claim, people: Person[], today: string) =>
  !isDeferred(c, today) &&
  (c.status === 'SETTLED' ||
  (isOpen(c) &&
    ((c.stated && c.status !== 'PENDING') || !leftOut(people.find((p) => p.id === c.personId), c.date, today))));

/** Whether the person's unbound open claim dated `date` is left out: within a hold, or retired by `today`. */
const leftOut = (person: Person | undefined, date: string, today: string) =>
  heldOn(person, date) || (!!person?.retiredFrom && !!today && person.retiredFrom <= today);

/** The first day (yyyy-mm-dd) of the 30 days up to and including the sheet's `today`. */
const activeFrom = (today: string) => new Date(Date.parse(`${today}T00:00:00Z`) - 29 * 86_400_000).toISOString().slice(0, 10);

/**
 * Whether the person is active: they have an open (OPEN or PENDING) claim that counts, dated within the 30 days up to
 * and including the sheet's `today` (never while `today` is not loaded).
 */
export function isActive(personId: number, claims: Claim[], people: Person[], today: string): boolean {
  if (today === '') return false;
  const from = activeFrom(today);
  return claims.some(
    (c) => c.personId === personId && isOpen(c) && counts(c, people, today) && from <= c.date && c.date <= today,
  );
}

/** Under the sheet's key, so re-loading the sheet re-reads its claims. */
export const claimsKey = (sheetId: string) => [...sheetKey(sheetId), 'claims'] as const;

/** Every claim on the sheet, newest first. */
export const claimsQuery = (sheetId: string) => ({
  queryKey: claimsKey(sheetId),
  queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`),
});

/** Amounts are added up in whole cents, so the sum shows exactly with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.reduce((total, c) => total + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
