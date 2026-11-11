import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A note on the sheet: the sheet's today when it was added, who added it, and its text. */
export type Note = { addedOn: string; addedBy: string; text: string };

export const notesKey = (sheetId: string) => ['sheet', sheetId, 'notes'] as const;

export const notesQuery = (sheetId: string) => ({
  queryKey: notesKey(sheetId),
  queryFn: async () =>
    (await getJson<{ notes: Note[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`)).notes,
});

/** The notes on the sheet, newest first; as of a date, those added while the sheet's today was on or before it. */
export function useNotes(sheetId: string) {
  return useQuery(notesQuery(sheetId));
}
