import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim's status: DRAFT when made, then OPEN, then SETTLED; a DRAFT or OPEN claim may be VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/**
 * A claim: its amount with two decimals, its date (yyyy-MM-dd), its note, its status and its
 * number on the sheet (given when it is opened; '' for a draft or a voided claim).
 */
export type Claim = {
  id: string;
  personId: string;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: string;
};

/** Whether a claim counts toward claimed, due-total and every total: only OPEN and SETTLED do. */
export const counts = (c: Claim) => c.status === 'OPEN' || c.status === 'SETTLED';

export const claimsKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

/** The sheet's claims, newest first. */
export function useClaims(sheetId: string) {
  return useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ claims: Claim[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`)).claims,
  });
}

/** An amount as a claim takes it: 0.00 or more, with up to two decimals. */
export const AMOUNT = /^\d+(\.\d{1,2})?$/;

/**
 * The amounts of the claims that count (OPEN and SETTLED) added up, in whole cents so no rounding
 * creeps in, shown with two decimals.
 */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((n, c) => n + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
