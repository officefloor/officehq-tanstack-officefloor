import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim template on the sheet: a claim made from it copies its amount (two decimals) and note. */
export type Template = { name: string; amount: string; note: string };

export const templatesKey = (sheetId: string) => ['sheet', sheetId, 'templates'] as const;

/** The claim templates on the sheet, by name. */
export function useTemplates(sheetId: string) {
  return useQuery({
    queryKey: templatesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ templates: Template[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`)).templates,
  });
}
