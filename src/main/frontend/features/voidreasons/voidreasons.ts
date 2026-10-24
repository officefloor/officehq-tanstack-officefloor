import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A sheet's void reason as the store holds it: a trimmed text, on the sheet at most once. While the sheet has any,
 *  a claim is voided only with one of them. */
export type VoidReason = { id: number; reason: string };

export const voidReasonsQueryKey = (sheetId: string) => ['sheet', sheetId, 'void-reasons'] as const;

export const voidReasonsPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`;

/** The sheet's void reasons, in the order they were added. */
export function useVoidReasons() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: voidReasonsQueryKey(sheet ?? ''),
    queryFn: () => getJson<VoidReason[]>(voidReasonsPath(sheet ?? '')),
    enabled: !!sheet,
  });
}
