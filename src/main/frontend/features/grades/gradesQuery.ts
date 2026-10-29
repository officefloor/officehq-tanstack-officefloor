import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';
import type { Grade } from '../people/peopleQuery';

/** A grade and whether it is retired on the sheet: a retired grade cannot be chosen for a person; people on it keep it. */
export type GradeState = { grade: Grade; retired: boolean };

/** Under the sheet's key, so re-loading the sheet re-reads them. */
export const gradesKey = (sheetId: string) => [...sheetKey(sheetId), 'grades'] as const;

export const gradesQuery = (sheetId: string) => ({
  queryKey: gradesKey(sheetId),
  queryFn: () => getJson<GradeState[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades`),
});
