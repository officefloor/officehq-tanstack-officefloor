import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A note of the sheet: its `text` (trimmed, not blank), who added it (`by`) and the sheet's today when they did
 *  (`on`, yyyy-MM-dd). */
export type Note = { text: string; by: string; on: string };

export const notesKey = (sheetId: string) => ['sheet', sheetId, 'notes'];

/** The sheet's notes, newest first. */
export function useNotes(sheetId: string) {
  return useQuery({
    queryKey: notesKey(sheetId),
    queryFn: () => getJson<Note[]>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`),
  });
}
