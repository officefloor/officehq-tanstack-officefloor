import { useQuery } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
import { getJson } from '../../api/http';
import { asFlag, asString, useSearchParam } from '../../url/useSearchParam';

/** A person on a sheet: a name and a grade (A, B or C: the one in force on the sheet's today), the version stored (one more on each edit), how many
 *  statements have been taken of them, and the last one's due-total (null when n/a, or none taken) and date
 *  (the sheet's today then; null when none taken), the date they are retired from (null: not retired), whether
 *  they have any hold, their dated grades, earliest first, and their holds in the order they were put on (their OPEN
 *  claims dated within one, not bound by a statement, count in no live total), their own terms in days (null: none), their contact (trimmed, lower-cased; null: none), their aliases, sorted as names are compared, and whether their name or an alias is also used on another
 *  sheet of the sheet's tree (its ancestors and descendants), the sheet they were last transferred from (null:
 *  never transferred), and their past retirements, in the order they were unretired, and their tags (trimmed, lower-cased,
 *  without repeats), sorted (empty when none), and their statements still standing (a cancelled one is gone),
 *  in the order taken. */
export type Person = { id: number; name: string; grade: string; version: number; statementCount: number;
  statedTotal: string | null; statementDate: string | null; retiredFrom: string | null; held: boolean;
  grades: Grade[]; holds: Hold[]; terms: number | null;
  contact: string | null; aliases: string[]; clashing: boolean; origin: string | null;
  retirements: Retirement[]; tags: string[]; statements: StatementLine[] };

/** A statement still standing: its date (yyyy-mm-dd) and due-total (two decimals; null: n/a). */
export type StatementLine = { date: string | null; dueTotal: string | null };

/** A past retirement, from the date they were retired from to the day they were unretired (yyyy-mm-dd). */
export type Retirement = { from: string; to: string };

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

/** The query of the sheet's people, in list order (by their position; new people go to the end). */
export const peopleQuery = (sheetId: string) => ({
  queryKey: peopleKey(sheetId),
  queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`),
});

/** The sheet's people, in list order (by their position; new people go to the end). */
export function usePeople(sheetId: string) {
  return useQuery(peopleQuery(sheetId));
}

/** The id of the person whose editor is open ('' when the form adds a new person). */
export function useEditingPerson() {
  return useSearchParam('editPerson', asString);
}

/** Close the person's editor, when it is still theirs that is open (another may have been opened since). */
export function useCloseEditor() {
  const navigate = useNavigate();
  return (personId: number) =>
    void navigate({
      to: '.',
      search: (prev: Record<string, unknown>) => {
        if (prev.editPerson !== String(personId)) return prev;
        const next = { ...prev };
        delete next.editPerson;
        return next;
      },
      replace: true,
    });
}

/** Whether retired people are listed: off when the screen is first opened, and left as it is by loading a sheet. */
export function useShowRetired() {
  return useSearchParam('showRetired', asFlag);
}
