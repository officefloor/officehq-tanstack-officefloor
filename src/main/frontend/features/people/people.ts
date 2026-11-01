import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, useSearchParam } from '../../url/useSearchParam';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

/** One of a person's grades, in force from its from-date (yyyy-MM-dd; null when it applies from the beginning of time). */
export type GradeFrom = { grade: Grade; from: string | null };

// grade is the one in force on the sheet's today; grades are every one recorded, by from-date.
// retiredFrom is null unless the person is retired; once set, they count as retired whatever the date.
// While held, their OPEN claims count in no live total; their SETTLED ones still do.
export type Person = {
  id: number;
  name: string;
  grade: Grade;
  version: number;
  retiredFrom: string | null;
  held: boolean;
  grades: GradeFrom[];
};

/** The person's grade in force on a date (yyyy-MM-dd): the one whose from-date is the latest on or before it. */
export function gradeOn(person: Person, date: string): Grade | undefined {
  let found: GradeFrom | undefined;
  for (const g of person.grades) {
    if (g.from !== null && g.from > date) continue;
    if (found === undefined || (found.from ?? '') < (g.from ?? '')) found = g;
  }
  return found?.grade;
}

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
