import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';

/** The sheet's settings: `clerkLimit` (two decimals) is the most due a clerk may settle a claim at,
 *  null for no limit; `floor` (two decimals) is the least a claim's live due may be; `terms` is the
 *  whole days after its date a claim is due by; `latePct` (two decimals) is the percent of an overdue
 *  claim's figure charged for every full 30 days it is overdue. */
export type Settings = { clerkLimit: string | null; floor: string; terms: string; latePct: string };

/** Under the sheet's key, so loading the sheet again re-reads its settings. */
export const settingsKey = (sheet: string) => [...sheetKey(sheet), 'settings'] as const;

export function useSettings(sheet: string) {
  return useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`),
    enabled: sheet !== '',
  });
}
