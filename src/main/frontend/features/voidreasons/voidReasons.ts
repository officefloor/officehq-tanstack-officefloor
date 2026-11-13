import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** The query key for the sheet's void reasons; under sheetKey, so re-loading the sheet re-reads them. */
export const voidReasonsKey = (sheet: string) => [...sheetKey(sheet), 'void-reasons'] as const;

/**
 * The sheet's void reasons, in the order they were added. While the list is not empty, a claim is voided only with a
 * reason chosen from it.
 */
export function useVoidReasons() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: voidReasonsKey(sheet),
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/void-reasons`),
    enabled: !!sheet,
  });
}
