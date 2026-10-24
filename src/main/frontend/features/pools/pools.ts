import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A sheet's pool as the store holds it: a trimmed name, on the sheet at most once, and its members, each a person
 *  of the sheet at most once with a share percent. `totalShare` is the members' shares added up, a retired member's
 *  (one with a retirement date set, whatever the date) counting as 0.00; `balanced` is whether it is exactly 100.00. */
export type Pool = { id: number; name: string; totalShare: string; balanced: boolean; members: PoolMember[] };

/** A pool's member: the person, their name, their share (two decimals) and their cut of the sheet's pot (two
 *  decimals; "" when the pool is not balanced or the sheet has no pot). */
export type PoolMember = { personId: number; name: string; share: string; cut: string };

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
