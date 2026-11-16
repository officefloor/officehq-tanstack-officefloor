import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A sheet setting: `value` is text (clerk-limit: an amount with two decimals), null when blank. */
export type Setting = { sheetId: string; key: string; value: string | null };

export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'];

/** The sheet's settings, by key. */
export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Setting[]>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}
