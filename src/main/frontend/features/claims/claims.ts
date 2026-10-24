import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** A claim as the store holds it: the amount with two decimals, the date (YYYY-MM-DD) and the note. */
export type Claim = { id: number; personId: number; amount: string; date: string; note: string };

export const claimsQueryKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

export const claimsPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/claims`;

export const personClaimsPath = (sheetId: string, personId: number) =>
  `/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/claims`;

/** Every claim on the sheet, newest first. */
export function useClaims() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: claimsQueryKey(sheet ?? ''),
    queryFn: () => getJson<Claim[]>(claimsPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** An amount ("12.50") in whole cents, so sums are exact. */
const cents = (amount: string) => Math.round(Number(amount) * 100);

/** A sum of claims' amounts, with two decimals. */
export function sumOf(claims: Claim[]): string {
  return (claims.reduce((n, c) => n + cents(c.amount), 0) / 100).toFixed(2);
}

/** The id of the claim whose editor is open (search param `editClaim`), or '' for none. */
export const useEditingClaim = () => useSearchParam('editClaim', asString);
