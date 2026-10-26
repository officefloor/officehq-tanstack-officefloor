import { getJson } from '../../api/http';
import { claimsKey } from '../claims/claimsQuery';

/** A void reason on the sheet, and how many VOID claims were voided with it. */
export type VoidReason = { reason: string; voided: number };

/** Under the claims' key, so a claim's change (a void) re-reads the counts, and re-loading the sheet re-reads the list. */
export const voidReasonsKey = (sheetId: string) => [...claimsKey(sheetId), 'void-reasons'] as const;

export const voidReasonsQuery = (sheetId: string) => ({
  queryKey: voidReasonsKey(sheetId),
  queryFn: () => getJson<VoidReason[]>(`/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`),
});
