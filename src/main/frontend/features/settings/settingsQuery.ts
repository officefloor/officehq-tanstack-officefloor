import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/** `clerkLimit` has two decimals ("1.50"), or is "" when there is no limit; `floor` has two decimals;
 *  `terms` is a whole number of days ("14"). */
export type Settings = { clerkLimit: string; floor: string; terms: string };

/** What the request carries: a setting left out (or blank, for the floor and terms) is left as it is. */
export type SettingsChange = { clerkLimit?: string; floor?: string; terms?: string };

/** The floor a sheet starts with. */
export const DEFAULT_FLOOR = '1.00';

/** The terms a sheet starts with, in days. */
export const DEFAULT_TERMS = '14';

export type RoleName = 'clerk' | 'steward';

/** The role a sheet gives a user name; a name with no entry is a clerk. */
export type Role = { user: string; role: RoleName };

export const ROLES: RoleName[] = ['clerk', 'steward'];

/** Under the sheet's key, so re-loading the sheet re-reads its settings and roles. */
export const settingsKey = (sheetId: string) => [...sheetKey(sheetId), 'settings'] as const;
export const rolesKey = (sheetId: string) => [...sheetKey(sheetId), 'roles'] as const;

export const settingsQuery = (sheetId: string) => ({
  queryKey: settingsKey(sheetId),
  queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
});

/** The sheet's floor: what a claim's live due is at least. */
export function useFloor(sheetId: string): string {
  return useQuery(settingsQuery(sheetId)).data?.floor ?? DEFAULT_FLOOR;
}

/** The sheet's terms in days: how long after its date a claim is due by. */
export function useTerms(sheetId: string): number {
  return Number(useQuery(settingsQuery(sheetId)).data?.terms ?? DEFAULT_TERMS);
}

export const rolesQuery = (sheetId: string) => ({
  queryKey: rolesKey(sheetId),
  queryFn: () => getJson<Role[]>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`),
});
