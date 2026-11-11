import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade on the sheet and whether it is retired: a retired grade cannot be chosen for a new person or in an edit. */
export type Grade = { grade: string; retired: boolean };

export const gradesKey = (sheetId: string) => ['sheet', sheetId, 'grades'] as const;

/** The grades on the sheet, in order: A, B and C, then those added. */
export function useGrades(sheetId: string) {
  return useQuery({
    queryKey: gradesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ grades: Grade[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/grades`)).grades,
  });
}

const BASE_GRADES = ['A', 'B', 'C'];

/** The codes of the grades on the sheet, in order (A, B and C until they are read). */
export function useGradeCodes(sheetId: string): string[] {
  const grades = useGrades(sheetId).data;
  return grades ? grades.map((g) => g.grade) : BASE_GRADES;
}
