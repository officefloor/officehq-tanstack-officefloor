import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A sheet's pool as the store holds it: a trimmed name, on the sheet at most once, and its members, each a person
 *  of the sheet at most once with a share percent. `totalShare` is the members' shares added up, a retired member's
 *  (one with a retirement date set, whatever the date) counting as given; `balanced` is whether it is exactly 100.00. */
export type Pool = { id: number; name: string; totalShare: string; balanced: boolean; members: PoolMember[] };

/** A pool's member: the person, or another pool (`poolId`; the other null), its name, its share (two decimals) and its
 *  cut of the pool's pot (two decimals; "" when the pool is not balanced or has no pot, or every member is retired, or the member's dates do not include the
 *  sheet's today — such a member is left out of the total share and balance too). A pool that is a member of
 *  none has the sheet's pot; a pool that is a member of another has its cut there as its pot. */
export type PoolMember = { personId: number | null; poolId: number | null; name: string; share: string; cut: string };

export const poolsQueryKey = (sheetId: string) => ['sheet', sheetId, 'pools'] as const;

export const poolsPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/pools`;

/** The sheet's pools, in the order they were added. */
export function usePools() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: poolsQueryKey(sheet ?? ''),
    queryFn: () => getJson<Pool[]>(poolsPath(sheet ?? '')),
    enabled: !!sheet,
  });
}
