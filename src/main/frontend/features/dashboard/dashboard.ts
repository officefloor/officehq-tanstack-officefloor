import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Role } from '../access/access';

// The role is per user: each signed-in user acts in their own role on a sheet.
export const myRoleKey = (sheetId: string, user: string) => ['sheet', sheetId, 'my-role', user] as const;

/** The role the signed-in user acts in on the sheet; while viewing as of a date, the role they had then. */
export function useMyRole(sheetId: string, user: string) {
  return useQuery({
    queryKey: myRoleKey(sheetId, user),
    queryFn: async () =>
      (await getJson<Role>(`/api/sheets/${encodeURIComponent(sheetId)}/my-role`, user)).role,
  });
}
