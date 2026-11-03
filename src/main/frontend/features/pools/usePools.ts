import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A member of a pool: a person (by id, and their name now) or another pool (`pool`, its name; `personId` then
 *  null), their share percent (two decimals), whether they are retired (a person with a retirement date set,
 *  whatever the date: they get no cut, their share spread over the others, but it counts in the total share) and their cut of the pool's pot (two decimals; null when
 *  the pool is not balanced, has no pot or has only retired members: a pool that is a member of another has its cut there as its pot; null
 *  too for a member that is not current), the membership's from-date and to-date (null when not given) and whether
 *  they include the sheet's today (a member that is not current counts in neither the total share nor the balance), and,
 *  for every period whose to-date is on or before the sheet's today, or that is closed or soft-closed, by period name in
 *  from-date order, their cut fixed when it was closed (a closed or soft-closed one) or as it stood at the end of that
 *  period (null when they had none then). */
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
  periodCuts: Record<string, string | null>;
};

/** A pool: its name, its members in the order they were added, the total of their shares (two decimals),
 *  whether that total is exactly 100.00 and its place in the order the sheet's pools were added (0 for the first). */
export type Pool = { name: string; members: PoolMember[]; totalShare: string; balanced: boolean; added: number };

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
