import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, useSearchParam } from '../../url/useSearchParam';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

/** A grade in force from a date (yyyy-MM-dd; '' from the beginning of time) until the next one. */
export type GradeFrom = { from: string; grade: Grade };

/** The grade in force on a date (yyyy-MM-dd): the one whose from-date is the latest on or before it. */
export function gradeOn(grades: GradeFrom[], date: string): Grade {
  let best = grades[0];
  for (const g of grades) if (g.from <= date && g.from >= best.from) best = g;
  return best.grade;
}

// `version` is raised by each save; an edit sends the version it was made from. `statementCount`
// is how many statements have been taken for them; `statedTotal` and `statementDate` are the last
// one's due-total and date ('' before the first). `retired` is set once they have a retirement
// date, `retiredFrom` ('' when not retired), whatever that date is. `held` is whether they are on hold: their OPEN and PENDING claims then count in no total.
// `patronIds` are the ids of the people they name as their patrons. `grade` is the grade in force on
// the sheet's today; `grades` every grade they have been given, by from-date, the first from ''.
export type Person = {
  id: string;
  name: string;
  grade: Grade;
  version: number;
  statementCount: number;
  statedTotal: string;
  statementDate: string;
  retired: boolean;
  retiredFrom: string;
  held: boolean;
  patronIds: string[];
  grades: GradeFrom[];
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

/** Whether retired people are listed. Off when the screen is first opened; loading a sheet keeps it. */
export const useShowRetired = () => useSearchParam('retired', asFlag);
