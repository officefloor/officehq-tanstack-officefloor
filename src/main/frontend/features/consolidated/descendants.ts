import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The ids of the sheet's descendants: the sheets naming it as their parent, then theirs, and so on. */
export function useDescendants(sheetId: string) {
  return useQuery({
    queryKey: ['sheet', sheetId, 'descendants'],
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/descendants`),
  });
}
