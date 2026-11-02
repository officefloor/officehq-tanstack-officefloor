import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A member of a pool with their share percent (two decimals): a person (memberPool null), retired when the person has
 *  a retirement date set, or another pool (memberPool its name, person 0); the membership's from-date and to-date (''
 *  when none) and whether they include the sheet's today (a member outside them is left out entirely); and their cut
 *  of the pool's pot (two decimals), '' when the pool is not balanced, has no pot or has only retired members, or the member is
 *  outside their dates. A pool in none has the sheet's pot; a pool in another has its cut there. And, by the name of
 *  every period whose to-date is on or before the sheet's today, their cut as it stood at the end of that period (the
 *  pool, its members, the pot and the holds as they were then, with that day as the sheet's today), '' when none. */
export type PoolMember = {
  person: number;
  memberPool: string | null;
  name: string;
  share: string;
  retired: boolean;
  from: string;
  to: string;
  current: boolean;
  cut: string;
  cuts: Record<string, string>;
};

/** A pool: its members, the total of their shares (a retired member's counting as given, one outside their dates left out), and whether that is 100.00. */
export type Pool = { name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

export const poolsKey = (sheet: string) => ['pools', sheet] as const;

export const poolsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/pools`;

export const poolMembersPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/pool-members`;

/** The sheet's pools, in the order added. */
export function usePools(sheet: string) {
  return useQuery({
    queryKey: poolsKey(sheet),
    queryFn: () => getJson<Pool[]>(poolsPath(sheet)),
  });
}
