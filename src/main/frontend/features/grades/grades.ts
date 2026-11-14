import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** A grade on the sheet and whether it is retired. */
export type Grade = { grade: string; retired: boolean };

/** The query key for the sheet's grades; under sheetKey, so re-loading the sheet re-reads them. */
export const gradesKey = (sheet: string) => [...sheetKey(sheet), 'grades'] as const;

/**
 * The sheet's grades, A to C, each with whether it is retired. A retired grade cannot be chosen for a person (by the
 * form, the quick-add bar or an import); those already on it keep it.
 */
export function useGrades() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: gradesKey(sheet),
    queryFn: () => getJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheet)}/grades`),
    enabled: !!sheet,
  });
}
