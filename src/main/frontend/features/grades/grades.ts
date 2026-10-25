import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A grade as the store holds it for a sheet: its code, and whether it is retired. People on a retired grade keep it,
 *  but it cannot be chosen for a new person or in an edit. */
export type Grade = { grade: string; retired: boolean };

export const gradesQueryKey = (sheetId: string) => ['sheet', sheetId, 'grades'] as const;

export const gradesPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/grades`;

/** The grades A, B and C, each with whether it is retired on the sheet. */
export function useGrades() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: gradesQueryKey(sheet ?? ''),
    queryFn: () => getJson<Grade[]>(gradesPath(sheet ?? '')),
    enabled: !!sheet,
  });
}
