import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/**
 * A sheet's settings: its clerk limit, with two decimals ('' for no limit), and its floor, the
 * least a claim's live due may be, with two decimals.
 */
export type Settings = { clerkLimit: string; floor: string };

/** A save of the settings: only those being changed are sent. */
export type SettingsChange = Partial<Settings>;

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

/** The sheet's floor ('0.00', no floor, until the settings are read). */
export function useFloor(sheetId: string): string {
  return useSettings(sheetId).data?.floor ?? '0.00';
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

/** A floor as the screen takes it: 0.00 or more with up to two decimals. */
export const FLOOR = /^\d+(\.\d{1,2})?$/;
