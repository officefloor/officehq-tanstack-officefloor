import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim template of the sheet: its `name` (trimmed, at most once on the sheet), and the `amount` (text with two
 *  decimals) and `note` a claim made from it takes at that moment. */
export type Template = { name: string; amount: string; note: string };

export const templatesKey = (sheetId: string) => ['sheet', sheetId, 'templates'];

/** The sheet's claim templates, in the order first saved. */
export function useTemplates(sheetId: string) {
  return useQuery({
    queryKey: templatesKey(sheetId),
    queryFn: () => getJson<Template[]>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`),
  });
}
