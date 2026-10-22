import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim template on the sheet: its name, the amount (two decimals) and note a claim made from it is given. */
export type Template = { name: string; amount: string; note: string; by: string };

export const templatesKey = (sheetId: string) => ['sheet', sheetId, 'templates'] as const;

/** The sheet's claim templates, by name. */
export function useTemplates(sheetId: string) {
  return useQuery({
    queryKey: templatesKey(sheetId),
    queryFn: () => getJson<Template[]>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`),
  });
}
