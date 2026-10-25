import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A sheet's note as the store holds it: the sheet's today when it was added (YYYY-MM-DD), who added it, its text. */
export type Note = { id: number; date: string; user: string; text: string };

export const notesQueryKey = (sheetId: string) => ['sheet', sheetId, 'notes'] as const;

export const notesPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/notes`;

/** The sheet's notes, newest first; as of a date, those added while the sheet's today was on or before it. */
export function useNotes() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: notesQueryKey(sheet ?? ''),
    queryFn: () => getJson<Note[]>(notesPath(sheet ?? '')),
    enabled: !!sheet,
  });
}
