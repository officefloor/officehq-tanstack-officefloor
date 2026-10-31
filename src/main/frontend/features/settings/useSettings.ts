import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A role given to a user name: clerk (the default for any name) or steward. */
export type RoleEntry = { name: string; role: string };

/** The sheet's settings as the server sends them: `clerkLimit` with two decimals, or null for no
 *  limit, and `floor` with two decimals. */
export type Settings = { clerkLimit: string | null; floor: string; roles: RoleEntry[] };

/** The floor of a sheet that has not set one. */
export const DEFAULT_FLOOR = '1.00';

export const settingsKey = (sheet: string) => ['sheet', sheet, 'settings'] as const;

/** The sheet's settings, re-read whenever the sheet is loaded. */
export function useSettings(sheet: string) {
  return useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`),
    staleTime: 0,
    gcTime: 0,
  });
}

/** The sheet's floor: a claim's live due is at least it. */
export function useFloor(sheet: string): string {
  return useSettings(sheet).data?.floor ?? DEFAULT_FLOOR;
}

export const ROLES = ['clerk', 'steward'] as const;

/** A clerk limit the server accepts: an amount with up to two decimals, or blank for no limit. */
export const LIMIT = /^(\d{1,10}(\.\d{1,2})?)?$/;
