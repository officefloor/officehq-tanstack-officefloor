import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A pool member: the person's id and name, their share percent (two decimals) and whether a retirement
 *  date is set (a retired member's share counts 0.00 toward the total). */
export type PoolMember = { personId: string; name: string; share: string; retired: boolean };

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
