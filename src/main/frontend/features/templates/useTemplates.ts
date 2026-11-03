import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim template: its name, amount (two decimals) and note. */
export type Template = { name: string; amount: string; note: string };

export function templatesKey(sheetId: string) {
  return ['sheet', sheetId, 'templates'];
}

/** The sheet's claim templates, by name. */
export function useTemplates(sheetId: string) {
  return useQuery({
    queryKey: templatesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ templates: Template[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`)).templates,
  });
}
