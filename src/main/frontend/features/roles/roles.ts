import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The role a user name has on the sheet; a name given none is a clerk. A steward settles any due, a clerk
 *  only one at or below the sheet's clerk limit; a viewer sees everything and changes nothing. */
export type Role = { user: string; role: 'clerk' | 'steward' | 'viewer' };

export const rolesKey = (sheet: string) => ['roles', sheet] as const;

export const rolesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/roles`;

/** Every role given on the sheet, by user name. */
export function useRoles(sheet: string) {
  return useQuery({
    queryKey: rolesKey(sheet),
    queryFn: () => getJson<Role[]>(rolesPath(sheet)),
  });
}

export const userRoleKey = (sheet: string, user: string) => ['roles', sheet, 'of', user] as const;

/** The role the user acts in on the sheet (steward while lent the role), as of the screen's date. */
export function useUserRole(sheet: string, user: string) {
  return useQuery({
    queryKey: userRoleKey(sheet, user),
    queryFn: () => getJson<Role>(`${rolesPath(sheet)}/of?user=${encodeURIComponent(user)}`),
  });
}
