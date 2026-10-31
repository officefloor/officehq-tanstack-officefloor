import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim template as the server sends it: its name, amount (two decimals) and note. */
export type ClaimTemplate = { name: string; amount: string; note: string };

export const templatesKey = (sheet: string) => ['sheet', sheet, 'claim-templates'] as const;

/** The sheet's claim templates, by name, re-read whenever the sheet is loaded. */
export function useTemplates(sheet: string) {
  return useQuery({
    queryKey: templatesKey(sheet),
    queryFn: () => getJson<ClaimTemplate[]>(`/api/sheets/${encodeURIComponent(sheet)}/claim-templates`),
    staleTime: 0,
    gcTime: 0,
  });
}
