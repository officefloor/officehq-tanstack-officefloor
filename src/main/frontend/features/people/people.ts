import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, asString, useSearchParam } from '../../url/useSearchParam';

/** A person on a sheet: a name and a grade (A, B or C), the version stored (one more on each edit), how many
 *  statements have been taken of them, and the last one's due-total (null when n/a, or none taken) and date
 *  (the sheet's today then; null when none taken), and the date they are retired from (null: not retired). */
export type Person = { id: number; name: string; grade: string; version: number; statementCount: number;
  statedTotal: string | null; statementDate: string | null; retiredFrom: string | null };

export const GRADES = ['A', 'B', 'C'] as const;

export const peopleKey = (sheetId: string) => ['sheet', sheetId, 'people'] as const;

/** The sheet's people, in the order they were added. */
export function usePeople(sheetId: string) {
  return useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`),
  });
}

/** The id of the person whose editor is open ('' when the form adds a new person). */
export function useEditingPerson() {
  return useSearchParam('editPerson', asString);
}

/** Whether retired people are listed: off when the screen is first opened, and left as it is by loading a sheet. */
export function useShowRetired() {
  return useSearchParam('showRetired', asFlag);
}
