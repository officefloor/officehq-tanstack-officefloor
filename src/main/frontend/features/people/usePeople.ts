import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person; `version` goes up by one each time they are saved. Their statements: how many have been
 *  taken, and the last one's due-total and date (null before the first). */
export type Person = {
  id: number;
  name: string;
  grade: string;
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
};

export const GRADES = ['A', 'B', 'C'] as const;

export function peopleKey(sheetId: string) {
  return ['sheet', sheetId, 'people'];
}

/** Read the sheet's people from the store. */
export async function fetchPeople(sheetId: string): Promise<Person[]> {
  return (await getJson<{ people: Person[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/people`)).people;
}

/** The sheet's people, in the order they were added. */
export function usePeople(sheetId: string) {
  return useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: () => fetchPeople(sheetId),
  });
}
