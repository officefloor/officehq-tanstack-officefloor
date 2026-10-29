import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/** A note on the sheet: `on` is the sheet's today when it was added (yyyy-mm-dd), `by` the user who added it. */
export type Note = { id: number; on: string; by: string; text: string };

/** Under the sheet's key, so re-loading the sheet re-reads its notes. */
export const notesKey = (sheetId: string) => [...sheetKey(sheetId), 'notes'] as const;

export const notesQuery = (sheetId: string) => ({
  queryKey: notesKey(sheetId),
  queryFn: () => getJson<Note[]>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`),
});
