import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { peopleKey } from '../people/people';

/** A member of a pool: the person, their `share` percent (two decimals; a JSON number, shown with two), and whether they are `retired` (a retirement
 *  date set, whatever the date): then their share counts as 0.00 toward the pool's total. `cut` is their cut of the sheet's
 *  pot (two decimals, as text; a retired member's 0.00), null when the pool is not balanced or the sheet has no pot. */
export type PoolMember = { personId: number; name: string; share: number; retired: boolean; cut: string | null };

/** A pool of the sheet: its `name` (trimmed, unique on the sheet), its members in the order added, the `totalShare`
 *  of their shares, and whether it is `balanced` (a total of exactly 100.00). A person can be in several pools. */
export type Pool = { name: string; members: PoolMember[]; totalShare: number; balanced: boolean };

/** Under the people's key, so every change to a person (a retirement above all) reloads the totals. */
export const poolsKey = (sheetId: string) => [...peopleKey(sheetId), 'pools'];

/** The sheet's pools, in the order added. */
export function usePools(sheetId: string) {
  return useQuery({
    queryKey: poolsKey(sheetId),
    queryFn: () => getJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`),
  });
}
