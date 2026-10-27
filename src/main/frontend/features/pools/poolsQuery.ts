import { getJson } from '../../api/http';
import { peopleKey } from '../people/peopleQuery';

/** A member of a pool: the person's id and name, their share percent with two decimals, and whether they are retired. */
export type PoolMember = { personId: number; name: string; share: string; retired: boolean };

/** A pool of the sheet: its name, its members, its total share (a retired member's counting 0.00) with two decimals,
 *  and whether that total is exactly 100.00. */
export type Pool = { name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

/** Under the people's key, so re-loading the sheet, or any change to its people (a retirement), re-reads the pools. */
export const poolsKey = (sheetId: string) => [...peopleKey(sheetId), 'pools'] as const;

export const poolsQuery = (sheetId: string) => ({
  queryKey: poolsKey(sheetId),
  queryFn: () => getJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`),
});
