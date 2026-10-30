import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { readPath, useAsOf } from '../asof/useAsOf';

/** A sheet note: the sheet's today when it was added (YYYY-MM-DD), who added it, and its text. */
export type Note = { on: string; user: string; text: string };

export const notesKey = (sheet: string, asOf = '') => ['sheet', sheet, 'notes', asOf] as const;

/** The sheet's notes, newest first; as of a date, those added while the sheet's today was on or before it. */
export function useNotes(sheet: string): Note[] {
  const asOf = useAsOf();
  const { data } = useQuery({
    queryKey: notesKey(sheet, asOf),
    queryFn: () => getJson<Note[]>(readPath(`/api/sheets/${encodeURIComponent(sheet)}/notes`, asOf)),
    enabled: sheet !== '',
  });
  return data ?? [];
}
