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
 * `dueBy` (yyyy-mm-dd) is fixed when the claim is made, from the terms of its person's grade on its date, and kept when they or its date change;
 * `log` is its status log, oldest first, one line per status it has had: "<STATUS> by <user> on <date> due=<due>";
 * `deferredUntil` (yyyy-mm-dd) is the date an OPEN claim was deferred to (null when never deferred), see `isDeferred`.
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
 * ones do not, nor do a person's open claims dated within one of their holds, unless a statement bound them,
 * nor an OPEN claim deferred on the sheet's `today`.
 */
export const counts = (c: Claim, people: Person[], today: string) =>
  !isDeferred(c, today) &&
  (c.status === 'SETTLED' ||
  (isOpen(c) &&
    ((c.stated && c.status !== 'PENDING') || !heldOn(people.find((p) => p.id === c.personId), c.date))));

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
