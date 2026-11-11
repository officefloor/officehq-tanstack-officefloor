import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade on the sheet and whether it is retired: a retired grade cannot be chosen for a new person or in an edit. */
export type Grade = { grade: string; retired: boolean };

export const gradesKey = (sheetId: string) => ['sheet', sheetId, 'grades'] as const;

/** The grades A, B and C on the sheet, in order. */
export function useGrades(sheetId: string) {
  return useQuery({
    queryKey: gradesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ grades: Grade[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/grades`)).grades,
  });
}
