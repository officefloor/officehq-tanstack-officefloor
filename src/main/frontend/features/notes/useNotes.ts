import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A sheet's note: the sheet's today when it was added (yyyy-mm-dd), who added it and its text. */
export type Note = { id: number; addedOn: string; addedBy: string; text: string };

export function notesKey(sheetId: string) {
  return ['sheet', sheetId, 'notes'];
}

/** The sheet's notes, newest first (as of the screen's date: those added while the sheet's today was on or before it). */
export function useNotes(sheetId: string) {
  return useQuery({
    queryKey: notesKey(sheetId),
    queryFn: async () => (await getJson<{ notes: Note[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`)).notes,
  });
}
