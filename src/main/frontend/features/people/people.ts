import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, useSearchParam } from '../../url/useSearchParam';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

// retiredFrom is null unless the person is retired; once set, they count as retired whatever the date.
// While held, their OPEN claims count in no live total; their SETTLED ones still do.
export type Person = { id: number; name: string; grade: Grade; version: number; retiredFrom: string | null; held: boolean };

export const isRetired = (p: Person) => p.retiredFrom !== null;

export const peopleKey = (sheet: string) => ['people', sheet] as const;

export const peoplePath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/people`;

/** Everyone on the sheet, in the order they were added. */
export function usePeople(sheet: string) {
  return useQuery({
    queryKey: peopleKey(sheet),
    queryFn: () => getJson<Person[]>(peoplePath(sheet)),
  });
}

/** Whether retired people are listed: off until turned on, and kept in the URL so loading a sheet leaves it be. */
export const useShowRetired = () => useSearchParam('retired', asFlag);
