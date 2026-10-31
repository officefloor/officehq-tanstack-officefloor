import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { GRADES } from '../people/usePeople';

export type Grade = { grade: string; retired: boolean };

export const gradesKey = (sheet: string) => ['sheet', sheet, 'grades'] as const;

/** The sheet's grades, each with whether it is retired. */
export function useGrades(sheet: string): Grade[] {
  const { data } = useQuery({
    queryKey: gradesKey(sheet),
    queryFn: () => getJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheet)}/grades`),
    enabled: sheet !== '',
  });
  return data ?? [];
}

/** The codes of the sheet's grades, in order: the standing A, B and C, then those added. */
export function useGradeCodes(sheet: string): string[] {
  const grades = useGrades(sheet);
  return grades.length === 0 ? [...GRADES] : grades.map((g) => g.grade);
}
