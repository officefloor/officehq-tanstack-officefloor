import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A role assigned to a user name: clerk (any name without one) or steward. */
export type Role = { user: string; role: string };

/** A sheet setting; a null value is blank. */
export type Setting = { key: string; value: string | null };

export const rolesKey = (sheetId: string) => ['sheet', sheetId, 'roles'] as const;
export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'] as const;

/** The sheet's assigned roles, by user name. */
export function useRoles(sheetId: string) {
  return useQuery({
    queryKey: rolesKey(sheetId),
    queryFn: () => getJson<Role[]>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`),
  });
}

/** The sheet's settings, by key. */
export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Setting[]>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}
