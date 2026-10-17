import { useQuery } from '@tanstack/react-query';
import { getJson, postJson, putJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { asString, useSearchParam } from '../../url/useSearchParam';

export const GRADES = ['A', 'B', 'C'] as const;

export type Person = { id: number; name: string; grade: string };
export type PersonInput = { name: string; grade: string };

const base = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/people`;
export const peopleKey = (sheet: string) => ['sheet', sheet, 'people'] as const;

/** The active sheet's people, in the order they were added. */
export function usePeople() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: peopleKey(sheet ?? ''),
    queryFn: () => getJson<{ people: Person[] }>(base(sheet!)).then((r) => r.people),
    enabled: !!sheet,
  });
}

export const addPerson = (sheet: string, user: string, input: PersonInput) =>
  postJson<Person>(base(sheet), input, user);

export const updatePerson = (sheet: string, user: string, id: number, input: PersonInput) =>
  putJson<Person>(`${base(sheet)}/${id}`, input, user);

/** The person open in the editor (by id), kept in the URL. */
export const useEditingPerson = () => useSearchParam('editPerson', asString);
