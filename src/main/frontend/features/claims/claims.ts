import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim's status: DRAFT when made, then OPEN, then SETTLED; a DRAFT or OPEN claim may be VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/**
 * A claim: its amount with two decimals, its date (yyyy-MM-dd), its note, its status and its
 * number on the sheet (given when it is opened; '' for a draft or a voided claim), and its due as
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
  corrects: string;
  roundDate: string;
};

/** Whether a claim is a correction: it has no floor and no late charge. */
export const isCorrection = (c: Claim) => c.corrects !== '';

/**
 * Whether a claim counts toward claimed, due-total and every total: only OPEN and SETTLED do, and
 * an OPEN one not while its person is on hold (`held`).
 */
export const counts = (c: Claim, held: boolean) => c.status === 'SETTLED' || (c.status === 'OPEN' && !held);

export const claimsKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

export const claimsQuery = (sheetId: string) => ({
  queryKey: claimsKey(sheetId),
  queryFn: async () =>
    (await getJson<{ claims: Claim[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`)).claims,
});

/** The sheet's claims, newest first. */
export function useClaims(sheetId: string) {
  return useQuery(claimsQuery(sheetId));
}

/** An amount as a claim takes it: 0.00 or more, with up to two decimals. */
export const AMOUNT = /^\d+(\.\d{1,2})?$/;

/** An amount as the claim form takes it: up to two decimals, negative for a correction. */
export const CLAIM_AMOUNT = /^-?\d+(\.\d{1,2})?$/;

/**
 * A claim number, as a correction names the claim it corrects: a sheet-wide number, or a number
 * within a period (<period name>-<n>, X-<n> for a claim in no period).
 */
export const CLAIM_NUMBER = /^(\d+|.+-\d+)$/;

/**
 * The amounts of the claims that count (OPEN and SETTLED, but not OPEN of a person on hold:
 * `held` tells by person id) added up, in whole cents so no rounding creeps in, shown with two
 * decimals.
 */
export function sumAmounts(claims: Claim[], held: (personId: string) => boolean): string {
  const cents = claims.filter((c) => counts(c, held(c.personId))).reduce((n, c) => n + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
