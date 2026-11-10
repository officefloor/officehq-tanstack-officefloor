import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/**
 * A sheet's settings: its clerk limit, with two decimals ('' for no limit), and its floor, the
 * least a claim's live due may be, with two decimals, and its terms, the whole number of days after a
 * claim's date that it is due by.
 */
export type Settings = { clerkLimit: string; floor: string; terms: string };

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

/** A sheet's terms until they are set. */
export const DEFAULT_TERMS = 14;

/** The sheet's terms in days (the default until the settings are read). */
export function useTerms(sheetId: string): number {
  const terms = useSettings(sheetId).data?.terms;
  return terms === undefined ? DEFAULT_TERMS : Number(terms);
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

/** Terms as the screen takes them: a whole number of days, 0 or more. */
export const TERMS = /^\d{1,5}$/;
