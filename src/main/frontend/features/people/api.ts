import { getJson, postJson, putJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

export type Person = { id: number; sheetId: string; name: string; grade: Grade };
export type PersonInput = { name: string; grade: string };

const base = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/people`;

export const peopleKey = (sheetId: string) => [...sheetKey(sheetId), 'people'] as const;

/** The sheet's people, in the order they were added. */
export function listPeople(sheetId: string): Promise<Person[]> {
  return getJson<Person[]>(base(sheetId));
}

/** Add a person; refused (rejects) when the name is empty. */
export function addPerson(sheetId: string, by: string, input: PersonInput): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}?by=${encodeURIComponent(by)}`, input);
}

/** Edit a person; refused (rejects) like an add. */
export function updatePerson(sheetId: string, by: string, id: number, input: PersonInput): Promise<Person> {
  return putJson<Person>(`${base(sheetId)}/${id}?by=${encodeURIComponent(by)}`, input);
}
