import { useQuery } from '@tanstack/react-query';
import { getJson, postJson, putJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { asString, useSearchParam } from '../../url/useSearchParam';

export const GRADES = ['A', 'B', 'C'] as const;

export type Person = { id: number; name: string; grade: string; version: number };
/** An edit carries the version of the person it was loaded from; the server refuses it (409) when
 *  someone else has saved the person since. */
export type PersonInput = { name: string; grade: string; version?: number };

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

/** The outcome of an import: the counts added and updated, or the numbers of the lines refused
 *  (nothing imported). */
export type ImportResult = { added: number; updated: number; errors: number[] };

/** Imports people pasted as lines of name,grade; a refusal (400) carries the bad line numbers. */
export const importPeople = (sheet: string, user: string, text: string) =>
  postJson<ImportResult>(`${base(sheet)}/import`, { text }, user);

/** The person open in the editor (by id), kept in the URL. */
export const useEditingPerson = () => useSearchParam('editPerson', asString);
