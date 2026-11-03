import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A member of a pool: a person (by id, and their name now) or another pool (`pool`, its name; `personId` then
 *  null), their share percent (two decimals), whether they are retired (a person with a retirement date set,
 *  whatever the date: their share then counts as 0.00) and their cut of the pool's pot (two decimals; null when
 *  the pool is not balanced or has no pot: a pool that is a member of another has its cut there as its pot; null
 *  too for a member that is not current), the membership's from-date and to-date (null when not given) and whether
 *  they include the sheet's today (a member that is not current counts in neither the total share nor the balance). */
export type PoolMember = {
  personId: number | null;
  pool: string | null;
  name: string;
  share: string;
  retired: boolean;
  cut: string | null;
  from: string | null;
  to: string | null;
  current: boolean;
};

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
