import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { GRADES } from '../people/people';

/** A grade as the store holds it for a sheet: its code, and whether it is retired. People on a retired grade keep it,
 *  but it cannot be chosen for a new person or in an edit. */
export type Grade = { grade: string; retired: boolean };

export const gradesQueryKey = (sheetId: string) => ['sheet', sheetId, 'grades'] as const;

export const gradesPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/grades`;

/** The sheet's grades — A, B and C, then those added to it in the order added — each with whether it is retired. */
export function useGrades() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: gradesQueryKey(sheet ?? ''),
    queryFn: () => getJson<Grade[]>(gradesPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** The sheet's grade codes, in order: A, B and C until the store's list has loaded. Every grade select, the summary,
 *  the rates and the rate calendar list these. */
export function useGradeCodes(): string[] {
  const grades = useGrades();
  return grades.data ? grades.data.map((g) => g.grade) : [...GRADES];
}
