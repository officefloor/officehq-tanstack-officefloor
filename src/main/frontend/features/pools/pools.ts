import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person in a pool with their share percent (two decimals); retired when the person has a retirement date set. */
export type PoolMember = { person: number; name: string; share: string; retired: boolean };

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
