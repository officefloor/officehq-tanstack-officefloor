import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** A note on the sheet: the sheet's today when it was added (yyyy-MM-dd), its user and its text. */
export type Note = { date: string; user: string; text: string };

/** The query key for the sheet's notes; under sheetKey, so re-loading the sheet re-reads them. */
export const notesKey = (sheet: string) => [...sheetKey(sheet), 'notes'] as const;

/** The sheet's notes, newest first; as of a date, those added while the sheet's today was on or before it. */
export function useNotes() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: notesKey(sheet),
    queryFn: () => getJson<Note[]>(`/api/sheets/${encodeURIComponent(sheet)}/notes`),
    enabled: !!sheet,
  });
}
