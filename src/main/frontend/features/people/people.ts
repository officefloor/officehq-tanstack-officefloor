import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

export type Person = { id: number; name: string; grade: Grade; version: number };

export const peopleKey = (sheet: string) => ['people', sheet] as const;

export const peoplePath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/people`;

/** Everyone on the sheet, in the order they were added. */
export function usePeople(sheet: string) {
  return useQuery({
    queryKey: peopleKey(sheet),
    queryFn: () => getJson<Person[]>(peoplePath(sheet)),
  });
}
