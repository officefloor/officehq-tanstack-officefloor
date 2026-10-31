import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

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
