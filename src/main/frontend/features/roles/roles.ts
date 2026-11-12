import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';

/** A user name's role on the sheet. A name with none assigned is a clerk; a viewer may change nothing;
 *  a restricted user acts as a clerk but sees on the people list only its own and its crews' people. */
export type Role = { user: string; role: string };

export const ROLES = ['clerk', 'steward', 'viewer', 'restricted'] as const;

/** Under the sheet's key, so loading the sheet again re-reads its roles. */
export const rolesKey = (sheet: string) => [...sheetKey(sheet), 'roles'] as const;

export function useRoles(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(rolesKey(sheet), asOf),
    queryFn: () => getJson<Role[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/roles`, asOf)),
    enabled: sheet !== '',
  });
}

/** The steward role delegated to `user` by the steward `by` for from..to (yyyy-mm-dd), inclusive. */
export type Delegation = { user: string; from: string; to: string; by: string };

export const delegationsKey = (sheet: string) => [...sheetKey(sheet), 'delegations'] as const;

export function useDelegations(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(delegationsKey(sheet), asOf),
    queryFn: () => getJson<Delegation[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/delegations`, asOf)),
    enabled: sheet !== '',
  });
}
