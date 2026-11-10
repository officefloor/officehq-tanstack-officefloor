import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

export type Person = { id: string; name: string; grade: Grade };

export const peopleKey = (sheetId: string) => ['sheet', sheetId, 'people'] as const;

/** The sheet's people, in the order they were added. */
export function usePeople(sheetId: string) {
  return useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: async () =>
      (await getJson<{ people: Person[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/people`)).people,
  });
}
