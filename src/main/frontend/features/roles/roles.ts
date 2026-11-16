import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The role a user name has on the sheet: clerk (any name not given one), steward or viewer (sees everything, changes nothing). */
export type Role = { sheetId: string; user: string; role: string };

export const rolesKey = (sheetId: string) => ['sheet', sheetId, 'roles'];

/** The roles given on the sheet, by user name. */
export function useRoles(sheetId: string) {
  return useQuery({
    queryKey: rolesKey(sheetId),
    queryFn: () => getJson<Role[]>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`),
  });
}
