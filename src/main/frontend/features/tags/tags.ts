import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** The query key for the sheet's tags; under sheetKey, so re-loading the sheet re-reads them. */
export const tagsKey = (sheet: string) => [...sheetKey(sheet), 'tags'] as const;

/** Every tag ever given to a claim on the sheet, sorted — kept even once no claim carries it. */
export function useTags() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: tagsKey(sheet),
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/tags`),
    enabled: !!sheet,
  });
}
