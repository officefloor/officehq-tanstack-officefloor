import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A sheet's settings: its clerk limit, with two decimals ('' for no limit). */
export type Settings = { clerkLimit: string };

/** A user's role on a sheet. A name with no role assigned is a clerk. */
export type Role = { user: string; role: string };

export const ROLES = ['clerk', 'steward'] as const;

export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'] as const;
export const rolesKey = (sheetId: string) => ['sheet', sheetId, 'roles'] as const;

export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}

/** The roles assigned on the sheet, by user name. */
export function useRoles(sheetId: string) {
  return useQuery({
    queryKey: rolesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ roles: Role[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`)).roles,
  });
}

/** A clerk limit as the screen takes it: blank, or 0.00 or more with up to two decimals. */
export const LIMIT = /^\d+(\.\d{1,2})?$/;
