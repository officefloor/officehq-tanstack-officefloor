import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

/** `today` is the sheet's own date (yyyy-MM-dd): every date decision and default uses it.
 *  `archived`: a steward archived it; everything stays visible and every change is refused. */
export type Sheet = { id: string; createdBy: string; today: string; archived: boolean };

/** Load a sheet by id; the server starts it empty the first time the id is used. */
export function openSheet(id: string, by: string, live = false): Promise<Sheet> {
  return postJson<Sheet>(
    `/api/sheets/${encodeURIComponent(id)}/open?by=${encodeURIComponent(by)}`,
    {},
    live,
  );
}

/** Change the sheet's today; refused (rejects) when it is not a date. */
export function setSheetToday(id: string, by: string, today: string): Promise<Sheet> {
  return postJson<Sheet>(
    `/api/sheets/${encodeURIComponent(id)}/today?by=${encodeURIComponent(by)}`,
    { today },
  );
}

/** Archive the sheet; refused (rejects) for anyone but a steward, and when already archived. */
export function archiveSheet(id: string, by: string): Promise<Sheet> {
  return postJson<Sheet>(
    `/api/sheets/${encodeURIComponent(id)}/archive?by=${encodeURIComponent(by)}`,
    {},
  );
}

/** Every query about a sheet lives under this key, so loading it again re-reads all of it. */
export const sheetKey = (id: string) => ['sheet', id] as const;

/** A query key about a sheet, for its live reads (`live`: as it is now, even while the screen shows it
 *  as of a date) kept apart from the screen's; still under the sheet's key, so loading it re-reads them. */
export const liveKey = <K extends readonly unknown[]>(key: K, live: boolean): readonly unknown[] =>
  live ? [key[0], key[1], 'live', ...key.slice(2)] : key;

/** The loaded sheet's today, read from the sheet query `ActiveSheet` keeps; '' until loaded. */
export function useSheetToday(id: string, by: string, live = false): string {
  const sheet = useQuery({ queryKey: liveKey(sheetKey(id), live), queryFn: () => openSheet(id, by, live) });
  return sheet.data?.today ?? '';
}

export const descendantsKey = (id: string) => [...sheetKey(id), 'descendants'] as const;

/** The ids of the sheet's descendants: its children (the sheets naming it as their parent), theirs, ... */
export function listDescendants(id: string, live = false): Promise<string[]> {
  return getJson<string[]>(`/api/sheets/${encodeURIComponent(id)}/descendants`, live);
}

export const treeKey = (id: string) => [...sheetKey(id), 'tree'] as const;

/** The ids of the sheets of the sheet's tree: its root (the top of its parent chain) first, then the
 *  root's descendants, nearest first; the sheet itself among them. */
export function listTree(id: string): Promise<string[]> {
  return getJson<string[]>(`/api/sheets/${encodeURIComponent(id)}/tree`);
}

/** A clone just made: the sheet cloned and the id it was cloned to. */
export type Clone = { from: string; to: string };

/** Clone the sheet to another id; refused (rejects) for anyone but a steward, and when that sheet was
 *  ever used. */
export function cloneSheet(id: string, by: string, to: string): Promise<Clone> {
  return postJson<Clone>(
    `/api/sheets/${encodeURIComponent(id)}/clone?by=${encodeURIComponent(by)}`,
    { to },
  );
}
