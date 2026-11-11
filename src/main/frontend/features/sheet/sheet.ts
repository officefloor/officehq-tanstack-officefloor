import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type Sheet = { id: string };

/** Every per-sheet query key starts with ['sheet', id], so loading a sheet re-reads all of it. */
export const sheetKey = (id: string) => ['sheet', id] as const;

export function useSheet(id: string) {
  return useQuery({
    queryKey: sheetKey(id),
    queryFn: () => getJson<Sheet>(`/api/sheets/${encodeURIComponent(id)}`),
    enabled: id !== '',
  });
}
