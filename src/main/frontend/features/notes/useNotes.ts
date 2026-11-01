import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';

/** A note as the server sends it: the sheet's today when added, who added it, its text. */
export type Note = { date: string; user: string; text: string };

export const notesKey = (sheet: string) => ['sheet', sheet, 'notes'] as const;

/** The sheet's notes, newest first, re-read whenever the sheet is loaded. */
export function useNotes(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(notesKey(sheet), asOf),
    queryFn: () => getJson<Note[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/notes`, asOf)),
    staleTime: 0,
    gcTime: 0,
  });
}
