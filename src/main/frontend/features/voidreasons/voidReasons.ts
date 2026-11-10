import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A void reason on the sheet: while the sheet has any, a claim is voided only with one of them. */
export type VoidReason = { reason: string };

export const voidReasonsKey = (sheetId: string) => ['sheet', sheetId, 'void-reasons'] as const;

/** The void reasons on the sheet, in the order they were added. */
export function useVoidReasons(sheetId: string) {
  return useQuery({
    queryKey: voidReasonsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ reasons: VoidReason[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`)).reasons,
  });
}
