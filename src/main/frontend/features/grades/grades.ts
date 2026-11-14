import { useQuery } from '@tanstack/react-query';
import { getJson, getLiveJson } from '../../api/http';
import { GRADES } from '../people/people';
import { sheetKey, useCurrentSheet } from '../session/session';

/** A grade on the sheet and whether it is retired. */
export type Grade = { grade: string; retired: boolean };

/** The query key for the sheet's grades; under sheetKey, so re-loading the sheet re-reads them. */
export const gradesKey = (sheet: string) => [...sheetKey(sheet), 'grades'] as const;

/**
 * The sheet's grades, A to C then those added on it, each with whether it is retired. A retired grade cannot be chosen for a person (by the
 * form, the quick-add bar or an import); those already on it keep it.
 */
export function useGrades() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: gradesKey(sheet),
    queryFn: () => getJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheet)}/grades`),
    enabled: !!sheet,
  });
}

/** The query key for the sheet's grade codes as the store holds them now; under sheetKey, as the grades. */
export const gradeCodesKey = (sheet: string) => [...sheetKey(sheet), 'grade-codes'] as const;

/**
 * The codes of the sheet's grades, in order (A, B, C until they are read): those every grade select, the summary and
 * the rates list. Read as the store holds them now, even while the screen shows the sheet as of a date, so a grade
 * added since is listed (with nothing on it then).
 */
export function useGradeCodes(): string[] {
  const [sheet] = useCurrentSheet();
  const grades = useQuery({
    queryKey: gradeCodesKey(sheet),
    queryFn: () => getLiveJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheet)}/grades`),
    enabled: !!sheet,
  }).data;
  return grades ? grades.map((g) => g.grade) : [...GRADES];
}
