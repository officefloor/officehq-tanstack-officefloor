import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A pool member: the person's id and name (or, for another pool that is a member, personId null and memberPool its
 *  name), their share percent (two decimals) and whether a retirement date is set (a retired member's share counts
 *  0.00 toward the total), and their cut of the pool's pot (two decimals; "" when the pool is not balanced or has no
 *  pot). A pool that is a member of none has the sheet's pot; one that is a member of another has its cut there. */
export type PoolMember = { personId: string | null; memberPool: string | null; name: string; share: string; retired: boolean; cut: string };

/** A pool: its members in the order added, the total of their shares and whether that is exactly 100.00. */
export type Pool = { name: string; totalShare: string; balanced: boolean; members: PoolMember[] };

export const poolsKey = (sheet: string) => ['sheet', sheet, 'pools'] as const;

/** The sheet's pools, in the order added. */
export function usePools(sheet: string): Pool[] {
  const { data } = useQuery({
    queryKey: poolsKey(sheet),
    queryFn: () => getJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheet)}/pools`),
    enabled: sheet !== '',
  });
  return data ?? [];
}
