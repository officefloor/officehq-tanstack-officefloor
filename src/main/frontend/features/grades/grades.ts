import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade, with whether it is retired on the sheet. */
export type Grade = { grade: string; retired: boolean };

export const gradesKey = (sheetId: string) => ['sheet', sheetId, 'grades'] as const;

/** The grades (A, B, C), each with whether it is retired on the sheet. */
export function useGrades(sheetId: string) {
  return useQuery({
    queryKey: gradesKey(sheetId),
    queryFn: () => getJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades`),
  });
}

/** Whether the grade is retired on the sheet: it cannot be chosen for a new person or in an edit. */
export function useIsGradeRetired(sheetId: string): (grade: string) => boolean {
  const grades = useGrades(sheetId).data ?? [];
  return (grade) => grades.some((g) => g.grade === grade && g.retired);
}

/** The refusal of a retired grade chosen for a person. */
export const retiredGradeMessage = (grade: string) => `The grade ${grade} is retired: choose another.`;
