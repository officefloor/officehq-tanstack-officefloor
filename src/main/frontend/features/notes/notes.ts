import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';

/** A note on the sheet: the sheet's today when it was added (yyyy-mm-dd), who added it, and its text. */
export type Note = { id: number; date: string; user: string; text: string };

/** Under the sheet's key, so loading the sheet again re-reads its notes. */
export const notesKey = (sheet: string) => [...sheetKey(sheet), 'notes'] as const;

/** The sheet's notes, newest first; under an as-of date, those added on or before it. */
export function useNotes(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(notesKey(sheet), asOf),
    queryFn: () => getJson<Note[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/notes`, asOf)),
    enabled: sheet !== '',
  });
}
