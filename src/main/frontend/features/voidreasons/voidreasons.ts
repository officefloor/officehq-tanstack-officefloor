import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A reason on the sheet's list of void reasons, with how many VOID claims were voided with it. */
export type VoidReason = { id: number; reason: string; by: string; voided: number };

export const voidReasonsKey = (sheetId: string) => ['sheet', sheetId, 'void-reasons'] as const;

/** The sheet's void reasons, in the order they were added. */
export function useVoidReasons(sheetId: string) {
  return useQuery({
    queryKey: voidReasonsKey(sheetId),
    queryFn: () => getJson<VoidReason[]>(`/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`),
  });
}
