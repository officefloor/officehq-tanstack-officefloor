import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** A claim template: its name, the amount (two decimals) and note a claim made from it gets. */
export type ClaimTemplate = { name: string; amount: string; note: string };

/** The query key for the sheet's claim templates; under sheetKey, so re-loading the sheet re-reads them. */
export const templatesKey = (sheet: string) => [...sheetKey(sheet), 'templates'] as const;

/** The sheet's claim templates, by name. */
export function useTemplates() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: templatesKey(sheet),
    queryFn: () => getJson<ClaimTemplate[]>(`/api/sheets/${encodeURIComponent(sheet)}/templates`),
    enabled: !!sheet,
  });
}
