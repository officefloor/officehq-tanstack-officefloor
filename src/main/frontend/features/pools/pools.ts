import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person in a pool with their share percent, whether they are retired by the sheet's today (their share
 *  then counts 0.00 toward the pool's total), and their cut of the sheet's pot ('' when the pool is unbalanced or
 *  there is no pot). */
export type PoolMember = { personId: number; name: string; share: string; retired: boolean; cut: string };

/** A pool on the sheet: its members in the order they were added, the total of their shares and whether it is
 *  balanced (the total is exactly 100.00). */
export type Pool = { id: number; name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

export const poolsKey = (sheetId: string) => ['sheet', sheetId, 'pools'] as const;

/** The sheet's pools, in the order they were added. */
export function usePools(sheetId: string) {
  return useQuery({
    queryKey: poolsKey(sheetId),
    queryFn: () => getJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`),
  });
}
