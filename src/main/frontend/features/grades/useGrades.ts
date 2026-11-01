import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade as the server sends it: its code and whether it is retired. */
export type Grade = { grade: string; retired: boolean };

export const gradesKey = (sheet: string) => ['sheet', sheet, 'grades'] as const;

/** The sheet's grades, in order, re-read whenever the sheet is loaded. */
export function useGrades(sheet: string) {
  return useQuery({
    queryKey: gradesKey(sheet),
    queryFn: () => getJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheet)}/grades`),
    staleTime: 0,
    gcTime: 0,
  });
}
