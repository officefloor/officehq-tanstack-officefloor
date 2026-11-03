import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The sheet's descendants (the sheets naming it as their parent, theirs, and so on), re-read from the store.
 *  Their own cached reads are dropped first, so each is re-read from the store too. */
export function useDescendants(sheetId: string) {
  const queryClient = useQueryClient();
  return useQuery({
    queryKey: ['sheet', sheetId, 'descendants'],
    queryFn: async () => {
      const { sheets } = await getJson<{ sheets: string[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/descendants`);
      for (const id of sheets) queryClient.removeQueries({ queryKey: ['sheet', id] });
      return sheets;
    },
  });
}
