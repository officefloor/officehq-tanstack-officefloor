import { postJson } from '../../api/http';

export type Sheet = { id: string; createdBy: string };

/** Load a sheet by id; the server starts it empty the first time the id is used. */
export function openSheet(id: string, by: string): Promise<Sheet> {
  return postJson<Sheet>(
    `/api/sheets/${encodeURIComponent(id)}/open?by=${encodeURIComponent(by)}`,
    {},
  );
}

/** Every query about a sheet lives under this key, so loading it again re-reads all of it. */
export const sheetKey = (id: string) => ['sheet', id] as const;
