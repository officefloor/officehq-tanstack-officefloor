import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, useSearchParam } from '../../url/useSearchParam';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

/** One of a person's grades, in force from its from-date (yyyy-MM-dd; null when it applies from the beginning of time). */
export type GradeFrom = { grade: Grade; from: string | null };

// grade is the one in force on the sheet's today; grades are every one recorded, by from-date.
// retiredFrom is null unless the person is retired; once set, they count as retired whatever the date.
/** One of a person's holds: from its from-date to its to-date, both included (yyyy-MM-dd; to is null when it never ends). */
export type Hold = { from: string; to: string | null };

// holds are every one added, in that order; their OPEN claims dated within a hold count in no live total, unless
// bound by a statement; their other claims still do.
export type Person = {
  id: number;
  name: string;
  grade: Grade;
  version: number;
  retiredFrom: string | null;
  holds: Hold[];
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

/** Whether the person has any hold. */
export const isHeld = (p: Person) => p.holds.length > 0;

/** Whether a date (yyyy-MM-dd) is within one of the person's holds. */
export const heldOn = (p: Person, date: string) =>
  p.holds.some((h) => date >= h.from && (h.to === null || date <= h.to));

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
