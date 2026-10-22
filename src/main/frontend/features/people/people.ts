import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** A person on a sheet: a name and a grade (A, B or C), and the version stored (one more on each edit). */
export type Person = { id: number; name: string; grade: string; version: number };

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
