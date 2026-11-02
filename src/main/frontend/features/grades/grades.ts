import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type GradeRow = { grade: string; retired: boolean };

export const gradesKey = (sheet: string) => ['grades', sheet] as const;

export const gradesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/grades`;

/** The sheet's grades, in order, with whether each is retired. */
export function useGrades(sheet: string) {
  return useQuery({
    queryKey: gradesKey(sheet),
    queryFn: () => getJson<GradeRow[]>(gradesPath(sheet)),
  });
}

/** The sheet's grade codes, in order: A, B and C, then those added, in the order added. */
export function useGradeCodes(sheet: string): string[] {
  const { data } = useGrades(sheet);
  return data?.length ? data.map((g) => g.grade) : ['A', 'B', 'C'];
}
