import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A role given to a user name: clerk (the default for any name) or steward. */
export type RoleEntry = { name: string; role: string };

/** The sheet's settings as the server sends them: `clerkLimit` with two decimals, or null for no
 *  limit, `floor` with two decimals and `terms` in days. */
export type Settings = { clerkLimit: string | null; floor: string; terms: number; roles: RoleEntry[] };

/** The floor of a sheet that has not set one. */
export const DEFAULT_FLOOR = '1.00';

/** The terms, in days, of a sheet that has not set them. */
export const DEFAULT_TERMS = 14;

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

/** The sheet's terms: a claim is due by its date plus this many days. */
export function useTerms(sheet: string): number {
  return useSettings(sheet).data?.terms ?? DEFAULT_TERMS;
}

export const ROLES = ['clerk', 'steward'] as const;

/** A clerk limit the server accepts: an amount with up to two decimals, or blank for no limit. */
export const LIMIT = /^(\d{1,10}(\.\d{1,2})?)?$/;

/** Terms the server accepts: a whole number of days, or blank for the default 14. */
export const DAYS = /^(\d{1,4})?$/;
