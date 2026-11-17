import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The role a user name has on the sheet: clerk (any name not given one), steward, viewer (sees everything, changes nothing) or
 *  restricted (sees on the people list only the people of the crews they lead or belong to, and themselves). */
export type Role = { sheetId: string; user: string; role: string };

export const rolesKey = (sheetId: string) => ['sheet', sheetId, 'roles'];

/** The roles given on the sheet, by user name. */
export function useRoles(sheetId: string) {
  return useQuery({
    queryKey: rolesKey(sheetId),
    queryFn: () => getJson<Role[]>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`),
  });
}

/** The role the user acts in on the sheet (a steward while a delegation to them holds its today); as of a date, the
 *  role they acted in then. */
export function useActingRole(sheetId: string, user: string) {
  return useQuery({
    queryKey: [...rolesKey(sheetId), 'acting', user],
    queryFn: () =>
      getJson<Role>(`/api/sheets/${encodeURIComponent(sheetId)}/roles/${encodeURIComponent(user.trim())}`),
    enabled: user.trim() !== '',
  });
}
