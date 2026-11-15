import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A person in a pool, with their share percent (two decimals); a `retired` member's share counts as
 *  0.00 toward the pool's total. `cut` is their cut of the sheet's pot (two decimals; a retired member's
 *  0.00, the remainder going to the largest share), "" when the pool is unbalanced or there is no pot. */
export type PoolMember = { personId: number; name: string; share: string; retired: boolean; cut: string };

/** A pool on the sheet: its members in the order added, the total of their shares and whether that
 *  total is exactly 100.00 (`balanced`). */
export type Pool = { id: number; sheetId: string; name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

const base = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/pools`;

export const poolsKey = (sheetId: string) => [...sheetKey(sheetId), 'pools'] as const;

/** The sheet's pools, in the order they were added. */
export function listPools(sheetId: string): Promise<Pool[]> {
  return getJson<Pool[]>(base(sheetId));
}

/** Add a pool (name trimmed); refused (rejects) for a blank name or one already used on the sheet. */
export function addPool(sheetId: string, by: string, name: string): Promise<Pool[]> {
  return postJson<Pool[]>(`${base(sheetId)}?by=${encodeURIComponent(by)}`, { name });
}

/** Add a person to a pool with a share percent; refused (rejects) for a person already in the pool or a
 *  share that is not more than 0.00 and at most 100.00 with at most two decimals. */
export function addPoolMember(sheetId: string, by: string, poolId: number, personId: number, share: string): Promise<Pool[]> {
  return postJson<Pool[]>(`${base(sheetId)}/${poolId}/members?by=${encodeURIComponent(by)}`, { personId, share });
}
