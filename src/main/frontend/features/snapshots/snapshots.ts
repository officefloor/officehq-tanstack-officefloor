import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** One of `person`'s snapshot lines: the day being left (yyyy-mm-dd) when the sheet's today moved, and their
 *  due-total on it (two decimals, or n/a). */
export type SnapshotLine = { person: number; day: string; dueTotal: string };

export const snapshotLinesKey = (sheet: string) => ['snapshot-lines', sheet] as const;

export const snapshotLinesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/snapshot-lines`;

/** Every snapshot line on the sheet, oldest first. */
export function useSnapshotLines(sheet: string) {
  return useQuery({
    queryKey: snapshotLinesKey(sheet),
    queryFn: () => getJson<SnapshotLine[]>(snapshotLinesPath(sheet)),
  });
}

export const snapshotComparePath = (sheet: string, from: string, to: string) =>
  `/api/sheets/${encodeURIComponent(sheet)}/snapshot-compare?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`;

/** Compares two snapshot dates: per person in list order, `<name> <then> <now>`, trimmed. */
export function fetchSnapshotCompare(sheet: string, from: string, to: string) {
  return getJson<string[]>(snapshotComparePath(sheet, from, to));
}
