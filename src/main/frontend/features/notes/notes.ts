import { queryOptions, useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A note on the sheet: the sheet's today when it was added, who added it, and its text. */
export type Note = { id: number; on: string; by: string; text: string };

export const notesKey = (sheet: string) => ['notes', sheet] as const;

export const notesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/notes`;

export const notesQuery = (sheet: string) =>
  queryOptions({ queryKey: notesKey(sheet), queryFn: () => getJson<Note[]>(notesPath(sheet)) });

/** The sheet's notes, newest first: those added while its today (or the as-of date) was on or before it. */
export function useNotes(sheet: string) {
  return useQuery(notesQuery(sheet));
}
