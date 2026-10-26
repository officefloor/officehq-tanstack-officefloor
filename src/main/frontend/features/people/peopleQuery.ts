import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

/**
 * `version` is raised on every edit; an edit names the version it loaded. `retiredFrom` (yyyy-mm-dd) is
 * set once the person is retired, which they are at once, whatever the date; null otherwise. `held` is whether
 * they are on hold: while held, their OPEN claims count in no live total.
 */
export type Person = { id: number; name: string; grade: Grade; version: number; retiredFrom: string | null; held: boolean };

/** Under the sheet's key, so re-loading the sheet re-reads its people. */
export const peopleKey = (sheetId: string) => [...sheetKey(sheetId), 'people'] as const;

export const peopleQuery = (sheetId: string) => ({
  queryKey: peopleKey(sheetId),
  queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`),
});
