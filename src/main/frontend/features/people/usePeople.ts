import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type Person = { id: number; name: string; grade: string };

export const GRADES = ['A', 'B', 'C'] as const;

export function peopleKey(sheetId: string) {
  return ['sheet', sheetId, 'people'];
}

/** The sheet's people, in the order they were added. */
export function usePeople(sheetId: string) {
  return useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: async () =>
      (await getJson<{ people: Person[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/people`)).people,
  });
}
