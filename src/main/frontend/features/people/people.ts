import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

// `version` is raised by each save; an edit sends the version it was made from. `statementCount`
// is how many statements have been taken for them; `statedTotal` and `statementDate` are the last
// one's due-total and date ('' before the first).
export type Person = {
  id: string;
  name: string;
  grade: Grade;
  version: number;
  statementCount: number;
  statedTotal: string;
  statementDate: string;
};

export const peopleKey = (sheetId: string) => ['sheet', sheetId, 'people'] as const;

export const peopleQuery = (sheetId: string) => ({
  queryKey: peopleKey(sheetId),
  queryFn: async () =>
    (await getJson<{ people: Person[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/people`)).people,
});

/** The sheet's people, in the order they were added. */
export function usePeople(sheetId: string) {
  return useQuery(peopleQuery(sheetId));
}
