import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade of the sheet: its code (A, B or C) and whether it is retired. A retired grade cannot be chosen for a new
 *  person nor in an edit; people already on it keep it. */
export type Grade = { grade: string; retired: boolean };

export const gradesKey = (sheetId: string) => ['sheet', sheetId, 'grades'];

/** The sheet's grades, A, B then C. */
export function useGrades(sheetId: string) {
  return useQuery({
    queryKey: gradesKey(sheetId),
    queryFn: () => getJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades`),
  });
}
