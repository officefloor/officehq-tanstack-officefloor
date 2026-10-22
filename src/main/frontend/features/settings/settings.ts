import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The sheet's settings: the clerk limit with two decimals ('' when there is none) and the floor with two
 *  decimals (a claim's live due is at least it). */
export type Settings = { clerkLimit: string; floor: string };

export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'] as const;

/** The floor a sheet starts with. */
export const DEFAULT_FLOOR = '1.00';

/** The sheet's settings as they are now. */
export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}

/** The sheet's floor in force (the starting floor while the settings load). */
export function useFloor(sheetId: string): string {
  return useSettings(sheetId).data?.floor ?? DEFAULT_FLOOR;
}
