import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A member of a pool: the person (by id, and their name now), their share percent (two decimals),
 *  whether they are retired (a retirement date set, whatever the date: their share then counts as 0.00) and
 *  their cut of the sheet's pot (two decimals; null when the pool is not balanced or there is no pot). */
export type PoolMember = { personId: number; name: string; share: string; retired: boolean; cut: string | null };

/** A pool: its name, its members in the order they were added, the total of their shares (two decimals)
 *  and whether that total is exactly 100.00. */
export type Pool = { name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

export function poolsKey(sheetId: string) {
  return ['sheet', sheetId, 'pools'];
}

/** The sheet's pools, by name. */
export function usePools(sheetId: string) {
  return useQuery({
    queryKey: poolsKey(sheetId),
    queryFn: async () => (await getJson<{ pools: Pool[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`)).pools,
  });
}
