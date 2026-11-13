import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, asNumber, useSearchParam } from '../../url/useSearchParam';
import { sheetKey, useCurrentSheet } from '../session/session';

export const GRADES = ['A', 'B', 'C'] as const;

/** A grade recorded for a person, in force from its from-date (yyyy-MM-dd; null: from the beginning of time). */
export type GradeFrom = { grade: string; from: string | null };

/** The grade in force on the date (yyyy-MM-dd): the one recorded with the latest from-date on or before it. */
export function gradeOn(grades: GradeFrom[], date: string): string | undefined {
  let found: GradeFrom | undefined;
  for (const g of grades) {
    if (g.from !== null && g.from > date) continue;
    if (!found || (found.from ?? '') < (g.from ?? '')) found = g;
  }
  return found?.grade;
}

/**
 * A person on the sheet as the store holds them; `grade` is the one in force on the sheet's today and
 * `grades` every grade recorded for them, each from its from-date; `version` counts their saves. `statementCount` counts
 * the statements taken of them; `statedTotal` is the last one's due-total and `statementDate` the
 * sheet's today when it was taken, both null before the first. `retired` is whether a retirement date is
 * set (whatever the date), `retiredFrom` that date, null while they are not retired. `held` is whether
 * they are on hold: their open claims then count in no live total.
 */
export type Person = {
  id: number;
  name: string;
  grade: string;
  grades: GradeFrom[];
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
  retired: boolean;
  retiredFrom: string | null;
  held: boolean;
};

/** The fields the person form submits; a blank `gradeFrom` is the sheet's today. */
export type PersonInput = { name: string; grade: string; gradeFrom: string };

/** The query key for the sheet's people; under sheetKey, so re-loading the sheet re-reads them. */
export const peopleKey = (sheet: string) => [...sheetKey(sheet), 'people'] as const;

/** The sheet's people, in the order they were added. */
export function usePeople() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: peopleKey(sheet),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
    enabled: !!sheet,
  });
}

/** The id of the person whose editor is open in the person form, if any. */
export const useEditingPerson = () => useSearchParam('person', asNumber);

/** Whether retired people are listed; off until the toggle is turned on, and kept when a sheet is loaded. */
export const useShowRetired = () => useSearchParam('retired', asFlag);
