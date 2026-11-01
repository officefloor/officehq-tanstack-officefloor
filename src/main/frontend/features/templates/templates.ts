import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim template on the sheet: its name, its amount (two decimals) and its note. */
export type ClaimTemplate = { name: string; amount: string; note: string };

export const templatesKey = (sheet: string) => ['templates', sheet] as const;

export const templatesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/templates`;

/** The sheet's claim templates, in the order first saved. */
export function useTemplates(sheet: string) {
  return useQuery({
    queryKey: templatesKey(sheet),
    queryFn: () => getJson<ClaimTemplate[]>(templatesPath(sheet)),
  });
}
