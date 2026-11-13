import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** Where a claim is in its life: DRAFT -> OPEN -> SETTLED; DRAFT or OPEN -> VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/**
 * A claim as the store holds it: the amount with two decimals, the date as yyyy-MM-dd; its number
 * is the sheet's next one when it was opened, null while a draft and once voided; its due is the
 * one fixed when it was settled (two decimals), null while it is live.
 */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: number | null;
  due: string | null;
};

/** Whether a claim counts toward claimed, due-total and every total: only OPEN and SETTLED do. */
export const counts = (c: Claim) => c.status === 'OPEN' || c.status === 'SETTLED';

/** The fields the claim form submits; a blank date is the sheet's today. */
export type ClaimInput = { amount: string; date: string; note: string };

/** 0.00 or more, with at most two decimals. */
export const AMOUNT = /^(\d+(\.\d{1,2})?|\.\d{1,2})$/;

/** The query key for the sheet's claims; under sheetKey, so re-loading the sheet re-reads them. */
export const claimsKey = (sheet: string) => [...sheetKey(sheet), 'claims'] as const;

/** The sheet's claims, newest first. */
export function useClaims() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims`),
    enabled: !!sheet,
  });
}

/** The sum of the counted claims' amounts, with two decimals; added in cents so it is exact. */
export function claimed(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
