import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, asString, useSearchParam } from '../../url/useSearchParam';

export const GRADES = ['A', 'B', 'C'] as const;

/** A grade a person has from a date (yyyy-MM-dd) until their next one's; a null `from` applies from the beginning of
 *  time (the first grade the person was given). */
export type PersonGrade = { grade: string; from: string | null };

/** A person on the sheet. `name` is unique on the sheet (case ignored), kept without outer spaces; `grades` are the
 *  grades recorded, by from-date, and `grade` (A, B or C) the one in force on the sheet's today.
 *  `version` goes up by one on each stored edit; an edit sends the version it was made from. `retiredFrom` (yyyy-MM-dd)
 *  is the date the person is retired from, null when not retired: a person with one counts as retired at once. `holds` are
 *  the person's holds, by from-date: an OPEN or PENDING claim of theirs dated within one, and not bound by a statement,
 *  counts in no live total. `held` is whether they have any hold. */
export type Person = {
  id: number;
  sheetId: string;
  name: string;
  grade: string;
  version: number;
  retiredFrom: string | null;
  held: boolean;
  grades: PersonGrade[];
  holds: PersonHold[];
};

/** A hold on a person from a date until a date (yyyy-MM-dd, both included); a null `to` is open-ended. */
export type PersonHold = { from: string; to: string | null };

/** Whether a hold of the person covers the date (yyyy-MM-dd). */
export const heldOn = (person: Pick<Person, 'holds'>, on: string) =>
  (person.holds ?? []).some((h) => h.from <= on && (h.to === null || on <= h.to));

/** The person's grade in force on a date (yyyy-MM-dd): the one with the latest from-date on or before it. */
export function gradeOn(person: Pick<Person, 'grade' | 'grades'>, on: string): string {
  let found: PersonGrade | undefined;
  for (const g of person.grades ?? []) {
    if (g.from !== null && g.from > on) continue;
    if (found === undefined || found.from === null || (g.from !== null && g.from > found.from)) found = g;
  }
  return found?.grade ?? person.grade;
}

export const peopleKey = (sheetId: string) => ['sheet', sheetId, 'people'];

/** The sheet's people, in the order they were added. */
export function usePeople(sheetId: string) {
  return useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`),
  });
}

/** The id of the person whose editor is open (in the person form), or '' when the form adds. */
export const useEditingPerson = () => useSearchParam('person', asString);

/** Whether retired people are listed. Off when the screen is first opened; loading a sheet leaves it as it is. */
export const useShowRetired = () => useSearchParam('retired', asFlag);
