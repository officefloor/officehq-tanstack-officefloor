import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A member of a pool with their share percent (two decimals): a person (memberPool null), retired when the person has
 *  a retirement date set, or another pool (memberPool its name, person 0); and their cut of the pool's pot (two
 *  decimals), '' when the pool is not balanced or has no pot. A pool in none has the sheet's pot; a pool in another
 *  has its cut there. */
export type PoolMember = {
  person: number;
  memberPool: string | null;
  name: string;
  share: string;
  retired: boolean;
  cut: string;
};

/** A pool: its members, the total of their shares (a retired member's counting 0.00), and whether that is 100.00. */
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
