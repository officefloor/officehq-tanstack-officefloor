import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

// Who is signed in and which sheet is active live in the URL (`user`, `sheet`), so any feature
// reads them directly. There is no password: signing in is typing a name.
export const useUser = () => useSearchParam('user', asString);
export const useSheetId = () => useSearchParam('sheet', asString);

export type Sheet = { id: string };

/** The active sheet as the store holds it. Keyed ['sheet', id]: every per-sheet key starts so. */
export function useSheet(sheetId: string) {
  return useQuery({
    queryKey: ['sheet', sheetId],
    queryFn: () => getJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}`),
    enabled: sheetId !== '',
  });
}
