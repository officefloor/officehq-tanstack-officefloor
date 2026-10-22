import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A member of a pool — a person, or another pool (poolId; personId then null) — with their share percent, whether
 *  they are retired by the sheet's today (their share then counts 0.00 toward the pool's total), and their cut of the
 *  pool's pot ('' when the pool is unbalanced or there is no pot). A pool that is a member of none has the sheet's
 *  pot; a member pool's pot is its cut in the pool above. */
export type PoolMember = {
  personId: number | null;
  name: string;
  share: string;
  retired: boolean;
  cut: string;
  poolId: number | null;
};

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
