import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A sheet setting: its value as text ("" when blank). A setting never set is not listed. */
export type Setting = { sheetId: string; key: string; value: string };
/** The role a user name has on the sheet; a name not listed is a clerk. */
export type Role = { sheetId: string; user: string; role: string };

/** The settings the screen offers: the clerk limit (two decimals; blank means no limit) and the
 *  floor (two decimals; the least a claim's live due can be). */
export const SETTING_KEYS = ['clerk-limit', 'floor'] as const;

/** A setting's value until it is first set. */
export const SETTING_DEFAULTS: Record<string, string> = { floor: '1.00' };

const base = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

export const settingsKey = (sheetId: string) => [...sheetKey(sheetId), 'settings'] as const;
export const rolesKey = (sheetId: string) => [...sheetKey(sheetId), 'roles'] as const;

export function listSettings(sheetId: string): Promise<Setting[]> {
  return getJson<Setting[]>(`${base(sheetId)}/settings`);
}

/** Change a setting; refused (rejects) when the value is not valid for it. */
export function changeSetting(sheetId: string, by: string, key: string, value: string): Promise<Setting> {
  return postJson<Setting>(`${base(sheetId)}/settings?by=${encodeURIComponent(by)}`, { key, value });
}

export function listRoles(sheetId: string): Promise<Role[]> {
  return getJson<Role[]>(`${base(sheetId)}/roles`);
}

/** Assign a user name a role; refused (rejects) when the role is not clerk or steward. */
export function setRole(sheetId: string, by: string, user: string, role: string): Promise<Role> {
  return postJson<Role>(`${base(sheetId)}/roles?by=${encodeURIComponent(by)}`, { user, role });
}

/** The sheet's floor: the least a claim's live due can be. */
export function useFloor(sheetId: string): string {
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  return settings.data?.find((s) => s.key === 'floor')?.value || SETTING_DEFAULTS.floor;
}
