import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A role given to a user name: clerk (the default for any name) or steward. */
export type RoleEntry = { name: string; role: string };

/** The sheet's settings as the server sends them: `clerkLimit` with two decimals, or null for no limit. */
export type Settings = { clerkLimit: string | null; roles: RoleEntry[] };

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

export const ROLES = ['clerk', 'steward'] as const;

/** A clerk limit the server accepts: an amount with up to two decimals, or blank for no limit. */
export const LIMIT = /^(\d{1,10}(\.\d{1,2})?)?$/;
