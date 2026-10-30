import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A pool member: the person's id and name (or, for another pool that is a member, personId null and memberPool its
 *  name), their share percent (two decimals) and whether a retirement date is set (a retired member's share counts
 *  toward the total as given, but they cut 0.00, their share spread over the others; a pool whose every member is retired has no cuts), and their cut of the pool's pot (two decimals; "" when the pool is not balanced or has no
 *  pot). current: whether the membership's optional from- and to-dates include the sheet's today; a member not
 *  current is left out entirely: not in the total, not in the balance, no cut (""). A pool that is a member of none has the sheet's pot; one that is a member of another has its cut there. */
export type PoolMember = { personId: string | null; memberPool: string | null; name: string; share: string; retired: boolean; current: boolean; cut: string };

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
