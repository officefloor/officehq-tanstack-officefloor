import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { GRADES } from '../people/people';

/** A grade of the sheet: its code (A, B, C, or one added to the sheet) and whether it is retired. A retired grade cannot be chosen for a new
 *  person nor in an edit; people already on it keep it. */
export type Grade = { grade: string; retired: boolean };

export const gradesKey = (sheetId: string) => ['sheet', sheetId, 'grades'];

/** The sheet's grades, A, B, C, then those added in the order they were added. */
export function useGrades(sheetId: string) {
  return useQuery({
    queryKey: gradesKey(sheetId),
    queryFn: () => getJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades`),
  });
}

/** The codes of the sheet's grades: A, B and C, then those added to the sheet, in the order they were added; A, B and C
 *  until they are loaded. Every grade select and per-grade figure lists these. */
export function useGradeCodes(sheetId: string): string[] {
  const grades = useGrades(sheetId);
  return grades.data?.map((g) => g.grade) ?? [...GRADES];
}
