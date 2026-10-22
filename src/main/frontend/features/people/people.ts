import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, asString, useSearchParam } from '../../url/useSearchParam';

/** A person on a sheet: a name and a grade (A, B or C: the one in force on the sheet's today), the version stored (one more on each edit), how many
 *  statements have been taken of them, and the last one's due-total (null when n/a, or none taken) and date
 *  (the sheet's today then; null when none taken), the date they are retired from (null: not retired), whether
 *  they have any hold, their dated grades, earliest first, and their holds in the order they were put on (their OPEN
 *  claims dated within one, not bound by a statement, count in no live total), and their own terms in days (null: none). */
export type Person = { id: number; name: string; grade: string; version: number; statementCount: number;
  statedTotal: string | null; statementDate: string | null; retiredFrom: string | null; held: boolean;
  grades: Grade[]; holds: Hold[]; terms: number | null };

/** A hold on a person, from its from-date to its to-date, both included (yyyy-mm-dd, or null: unbounded on that side). */
export type Hold = { from: string | null; to: string | null };

/** Whether one of the person's holds covers the date (yyyy-mm-dd). */
export function heldOn(person: Person | undefined, date: string): boolean {
  return (person?.holds ?? []).some((h) => (h.from === null || h.from <= date) && (h.to === null || date <= h.to));
}

/** Whether the person's retirement date is reached on the sheet's today (yyyy-mm-dd; '' while loading: not reached):
 *  from then their OPEN and PENDING claims not bound by a statement count in no live total. */
export function retiredOn(person: Person | undefined, today: string): boolean {
  return today !== '' && person?.retiredFrom != null && person.retiredFrom <= today;
}

/** A grade a person was given, in force from its from-date (yyyy-mm-dd, or null: from the beginning of time). */
export type Grade = { grade: string; from: string | null };

/** The person's grade in force on a date (yyyy-mm-dd): the one whose from-date is the latest on or before it,
 *  a grade with no from-date applying from the beginning of time; '' when none applies. */
export function gradeOn(person: Person | undefined, date: string): string {
  let found: Grade | undefined;
  for (const g of person?.grades ?? []) {
    if (g.from !== null && g.from > date) continue;
    if (!found || (found.from ?? '') <= (g.from ?? '')) found = g;
  }
  return found?.grade ?? '';
}

export const GRADES = ['A', 'B', 'C'] as const;

export const peopleKey = (sheetId: string) => ['sheet', sheetId, 'people'] as const;

/** The sheet's people, in the order they were added. */
export function usePeople(sheetId: string) {
  return useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`),
  });
}

/** The id of the person whose editor is open ('' when the form adds a new person). */
export function useEditingPerson() {
  return useSearchParam('editPerson', asString);
}

/** Whether retired people are listed: off when the screen is first opened, and left as it is by loading a sheet. */
export function useShowRetired() {
  return useSearchParam('showRetired', asFlag);
}
