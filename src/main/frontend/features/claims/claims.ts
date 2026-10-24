import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** A claim's status: DRAFT to start, then OPEN and SETTLED; a DRAFT or OPEN claim can be VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A claim as the store holds it: the amount with two decimals, the date (YYYY-MM-DD), the note, the status and
 *  the number given when it was opened (null for a claim never opened; a voided claim keeps it) and the due fixed when it was
 *  settled (null when it has none: its due is live), and who opened it (null when never opened); whether its person's last statement binds it, and the
 *  figure it binds it to (null: n/a). */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: number | null;
  due: string | null;
  openedBy: string | null;
  stated: boolean;
  statedDue: string | null;
};

/** Whether a claim counts toward claimed and the totals: only OPEN and SETTLED claims do. */
export const counts = (c: Claim) => c.status === 'OPEN' || c.status === 'SETTLED';

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

/** A sum of the counted (OPEN and SETTLED) claims' amounts, with two decimals. */
export function sumOf(claims: Claim[]): string {
  return (claims.filter(counts).reduce((n, c) => n + cents(c.amount), 0) / 100).toFixed(2);
}

/** The id of the claim whose editor is open (search param `editClaim`), or '' for none. */
export const useEditingClaim = () => useSearchParam('editClaim', asString);
