import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { readPath, useAsOf } from '../asof/useAsOf';

/** A pool member: the person's id and name (or, for another pool that is a member, personId null and memberPool its
 *  name), their share percent (two decimals) and whether a retirement date is set (a retired member's share counts
 *  toward the total as given, but they cut 0.00, their share spread over the others; a pool whose every member is retired has no cuts), and their cut of the pool's pot (two decimals; "" when the pool is not balanced or has no
 *  pot). current: whether the membership's optional from- and to-dates include the sheet's today; a member not
 *  current is left out entirely: not in the total, not in the balance, no cut (""). A pool that is a member of none has the sheet's pot; one that is a member of another has its cut there.
 *  periodCuts: for each period whose to-date is on or before the sheet's today, by name, the member's cut as it stood at
 *  the end of that period (the pool, its members, the pot and the holds as they were then); "" when there was none. */
export type PoolMember = { personId: string | null; memberPool: string | null; name: string; share: string; retired: boolean; current: boolean; cut: string; periodCuts: Record<string, string> };

/** A pool: its members in the order added, the total of their shares and whether that is exactly 100.00. crew: whether
 *  it is a crew's pool — its members the crew's lead and members at equal shares, never added through the pool. */
export type Pool = { name: string; totalShare: string; balanced: boolean; members: PoolMember[]; crew: boolean };

export const poolsKey = (sheet: string, asOf = '') => ['sheet', sheet, 'pools', asOf] as const;

/** The sheet's pools, in the order added; with an as-of date, memberships and holds judged on it. */
export function usePools(sheet: string): Pool[] {
  const asOf = useAsOf();
  const { data } = useQuery({
    queryKey: poolsKey(sheet, asOf),
    queryFn: () => getJson<Pool[]>(readPath(`/api/sheets/${encodeURIComponent(sheet)}/pools`, asOf)),
    enabled: sheet !== '',
  });
  return data ?? [];
}
