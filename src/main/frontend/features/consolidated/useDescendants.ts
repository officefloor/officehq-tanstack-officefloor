import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export const descendantsKey = (sheet: string) => ['sheet', sheet, 'descendants'] as const;

/** The ids of the sheet's descendants: its children (the sheets naming it as their parent), theirs,
 *  and so on, nearest first. Re-read whenever the sheet is loaded. A parent is not dated, so an
 *  as-of view sees the same ones. */
export function useDescendants(sheet: string) {
  return useQuery({
    queryKey: descendantsKey(sheet),
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/descendants`),
    staleTime: 0,
    gcTime: 0,
  });
}
