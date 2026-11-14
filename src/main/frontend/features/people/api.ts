import { getJson, postJson, putJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

/** `version` is raised by every saved edit; an edit sends the version its form loaded. Their
 *  statements: how many were taken, and the last one's due-total and date (null until the first). */
export type Person = {
  id: number; sheetId: string; name: string; grade: Grade; version: number;
  statementCount: number; statedTotal: string | null; statementDate: string | null;
};
export type PersonInput = { name: string; grade: string };

const base = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/people`;

export const peopleKey = (sheetId: string) => [...sheetKey(sheetId), 'people'] as const;

/** The sheet's people, in the order they were added. */
export function listPeople(sheetId: string): Promise<Person[]> {
  return getJson<Person[]>(base(sheetId));
}

/** Add a person; refused (rejects) when the name is empty or already in use on the sheet. */
export function addPerson(sheetId: string, by: string, input: PersonInput): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}?by=${encodeURIComponent(by)}`, input);
}

/** Edit a person loaded at `version`; refused (rejects) like an add, and with a 409 when someone
 *  else has saved the person since. */
export function updatePerson(sheetId: string, by: string, id: number, version: number, input: PersonInput): Promise<Person> {
  return putJson<Person>(`${base(sheetId)}/${id}?by=${encodeURIComponent(by)}`, { ...input, version });
}

/** Take a statement of the person: records their due-total now and binds each of their counting
 *  claims to the figure it shows now (unbinding the others). */
export function takeStatement(sheetId: string, by: string, id: number): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/statement?by=${encodeURIComponent(by)}`, {});
}
