import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { peopleKey } from '../people/people';

/** A person in a pool with a share percent (two decimals). A retired member's share counts as 0.00 toward the total. */
export type PoolMember = { personId: string; name: string; share: string; retired: boolean };

/** A pool on the sheet: its members in the order added, their total share, and balanced when it is exactly 100.00. */
export type Pool = { name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

// Under the people's key: a change to a person (a retirement, a new name) reads the pools again too.
export const poolsKey = (sheetId: string) => [...peopleKey(sheetId), 'pools'] as const;

/** The pools on the sheet, by name. */
export function usePools(sheetId: string) {
  return useQuery({
    queryKey: poolsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ pools: Pool[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`)).pools,
  });
}
