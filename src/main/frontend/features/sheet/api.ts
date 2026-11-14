import { useQuery } from '@tanstack/react-query';
import { postJson } from '../../api/http';

/** `today` is the sheet's own date (yyyy-MM-dd): every date decision and default uses it. */
export type Sheet = { id: string; createdBy: string; today: string };

/** Load a sheet by id; the server starts it empty the first time the id is used. */
export function openSheet(id: string, by: string): Promise<Sheet> {
  return postJson<Sheet>(
    `/api/sheets/${encodeURIComponent(id)}/open?by=${encodeURIComponent(by)}`,
    {},
  );
}

/** Change the sheet's today; refused (rejects) when it is not a date. */
export function setSheetToday(id: string, by: string, today: string): Promise<Sheet> {
  return postJson<Sheet>(
    `/api/sheets/${encodeURIComponent(id)}/today?by=${encodeURIComponent(by)}`,
    { today },
  );
}

/** Every query about a sheet lives under this key, so loading it again re-reads all of it. */
export const sheetKey = (id: string) => ['sheet', id] as const;

/** The loaded sheet's today, read from the sheet query `ActiveSheet` keeps; '' until loaded. */
export function useSheetToday(id: string, by: string): string {
  const sheet = useQuery({ queryKey: sheetKey(id), queryFn: () => openSheet(id, by) });
  return sheet.data?.today ?? '';
}
