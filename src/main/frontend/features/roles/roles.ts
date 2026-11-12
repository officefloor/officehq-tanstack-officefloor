import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';

/** A user name's role on the sheet. A name with none assigned is a clerk; a viewer may change nothing. */
export type Role = { user: string; role: string };

export const ROLES = ['clerk', 'steward', 'viewer'] as const;

/** Under the sheet's key, so loading the sheet again re-reads its roles. */
export const rolesKey = (sheet: string) => [...sheetKey(sheet), 'roles'] as const;

export function useRoles(sheet: string) {
  return useQuery({
    queryKey: rolesKey(sheet),
    queryFn: () => getJson<Role[]>(`/api/sheets/${encodeURIComponent(sheet)}/roles`),
    enabled: sheet !== '',
  });
}

/** The steward role delegated to `user` by the steward `by` for from..to (yyyy-mm-dd), inclusive. */
export type Delegation = { user: string; from: string; to: string; by: string };

export const delegationsKey = (sheet: string) => [...sheetKey(sheet), 'delegations'] as const;

export function useDelegations(sheet: string) {
  return useQuery({
    queryKey: delegationsKey(sheet),
    queryFn: () => getJson<Delegation[]>(`/api/sheets/${encodeURIComponent(sheet)}/delegations`),
    enabled: sheet !== '',
  });
}
