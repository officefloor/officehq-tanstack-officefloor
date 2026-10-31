import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** `version` counts the person's saves; an edit sends the version it loaded, so a save made by
 *  someone else in the meantime refuses it. `statementCount` is how many statements have been
 *  taken for them; `statedTotal` the last one's due-total (two decimals, null when it was n/a or
 *  none was taken) and `statementDate` its date (yyyy-mm-dd, null when none was taken).
 *  `retired` is whether a retirement date is set (whatever the date), `retiredFrom` that date
 *  (yyyy-mm-dd, null when not retired). `held` is whether they are on hold: their OPEN claims then
 *  count in no live total. `patrons` are the ids of the people they have named as patrons, in
 *  the order they were named. */
export type Person = {
  id: number;
  name: string;
  grade: string;
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
  retired: boolean;
  retiredFrom: string | null;
  held: boolean;
  patrons: number[];
};

export const GRADES = ['A', 'B', 'C'] as const;

export const peopleKey = (sheet: string) => ['sheet', sheet, 'people'] as const;

/** The sheet's people in the order they were added, re-read whenever the sheet is loaded. */
export function usePeople(sheet: string) {
  return useQuery({
    queryKey: peopleKey(sheet),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
    staleTime: 0,
    gcTime: 0,
  });
}
