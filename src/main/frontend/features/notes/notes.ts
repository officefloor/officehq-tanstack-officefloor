import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A note on the sheet: the sheet's today when it was added, who added it and its text. */
export type Note = { id: number; on: string; by: string; text: string };

export const notesKey = (sheetId: string) => ['sheet', sheetId, 'notes'] as const;

export const notesQuery = (sheetId: string) => ({
  queryKey: notesKey(sheetId),
  queryFn: () => getJson<Note[]>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`),
});

/** The sheet's notes, newest first. */
export function useNotes(sheetId: string) {
  return useQuery(notesQuery(sheetId));
}
