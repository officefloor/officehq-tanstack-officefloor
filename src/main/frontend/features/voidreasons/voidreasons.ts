import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { claimsKey } from '../claims/claims';

/** A void reason of the sheet: its `reason` (trimmed, at most once on the sheet) and the `count` of the sheet's VOID
 *  claims voided with it. While the sheet has any, voiding a claim needs one of them. */
export type VoidReason = { reason: string; count: number };

/** Under the claims' key, so every change to a claim (a void above all) reloads the counts. */
export const voidReasonsKey = (sheetId: string) => [...claimsKey(sheetId), 'void-reasons'];

/** The sheet's void reasons, in the order added. */
export function useVoidReasons(sheetId: string) {
  return useQuery({
    queryKey: voidReasonsKey(sheetId),
    queryFn: () => getJson<VoidReason[]>(`/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`),
  });
}
