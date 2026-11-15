import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

const sheetBase = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

/** A note on the sheet: its text (trimmed), who added it and the sheet's today then (yyyy-MM-dd). */
export type Note = { id: number; sheetId: string; text: string; by: string; addedOn: string };

export const notesKey = (sheetId: string) => [...sheetKey(sheetId), 'notes'] as const;

/** The sheet's notes, newest first; as of a date, those added while the sheet's today was on or before it. */
export function listNotes(sheetId: string): Promise<Note[]> {
  return getJson<Note[]>(`${sheetBase(sheetId)}/notes`);
}

/** Add a note; refused (rejects) for a blank one. */
export function addNote(sheetId: string, by: string, text: string): Promise<Note> {
  return postJson<Note>(`${sheetBase(sheetId)}/notes?by=${encodeURIComponent(by)}`, { text });
}
