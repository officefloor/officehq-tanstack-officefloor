import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade, with whether it is retired on the sheet. */
export type Grade = { grade: string; retired: boolean };

export const gradesKey = (sheetId: string) => ['sheet', sheetId, 'grades'] as const;

/** The sheet's grades (A, B, C, then those added to it in the order added), each with whether it is retired. */
export function useGrades(sheetId: string) {
  return useQuery({
    queryKey: gradesKey(sheetId),
    queryFn: () => getJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades`),
  });
}

/** The grades every sheet has, shown while the sheet's grades load. */
const BASE_GRADES: string[] = ['A', 'B', 'C'];

/** The sheet's grade codes in order: A, B, C, then those added to it. Every grade select, the summary and the rates
 *  list them. */
export function useGradeCodes(sheetId: string): string[] {
  const grades = useGrades(sheetId).data;
  return useMemo(() => (grades ? grades.map((g) => g.grade) : BASE_GRADES), [grades]);
}

/** Whether the grade is retired on the sheet: it cannot be chosen for a new person or in an edit. */
export function useIsGradeRetired(sheetId: string): (grade: string) => boolean {
  const grades = useGrades(sheetId).data ?? [];
  return (grade) => grades.some((g) => g.grade === grade && g.retired);
}

/** The refusal of a retired grade chosen for a person. */
export const retiredGradeMessage = (grade: string) => `The grade ${grade} is retired: choose another.`;
