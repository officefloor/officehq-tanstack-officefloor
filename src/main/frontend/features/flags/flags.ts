import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's flag in force on the sheet's today: the reason and the date it runs until (yyyy-mm-dd). */
export interface Flag {
  personId: number;
  reason: string;
  until: string;
}

export const flagsKey = (sheetId: string) => ['sheet', sheetId, 'flags'] as const;

/** The flags on the sheet in force on its today (today on or before their until date). */
export function useFlags(sheetId: string) {
  return useQuery({
    queryKey: flagsKey(sheetId),
    queryFn: () => getJson<Flag[]>(`/api/sheets/${encodeURIComponent(sheetId)}/flags`),
  });
}
