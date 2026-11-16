import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

export const GRADES = ['A', 'B', 'C'] as const;

/** A person on the sheet. `name` is kept exactly as entered; `grade` is A, B or C. */
export type Person = { id: number; sheetId: string; name: string; grade: string };

export const peopleKey = (sheetId: string) => ['sheet', sheetId, 'people'];

/** The sheet's people, in the order they were added. */
export function usePeople(sheetId: string) {
  return useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`),
  });
}

/** The id of the person whose editor is open (in the person form), or '' when the form adds. */
export const useEditingPerson = () => useSearchParam('person', asString);
