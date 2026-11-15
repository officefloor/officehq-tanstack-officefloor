import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A member of a pool, a person (`personId`) or another pool (`poolId`, `name` its name), with their share
 *  percent (two decimals); a `retired` member's share counts as 0.00 toward the pool's total. `cut` is
 *  their cut of the pool's pot (two decimals; a retired member's or one on a hold in force on the sheet's
 *  today 0.00, their share spread over the others in proportion; the remainder going to the largest share), "" when the pool is unbalanced or has no pot. A pool that is a member of none cuts the sheet's
 *  pot; a member pool cuts its own cut in the pool above. */
export type PoolMember = { personId: number | null; poolId: number | null; name: string; share: string; retired: boolean; cut: string };

/** A pool on the sheet: its members in the order added, the total of their shares and whether that
 *  total is exactly 100.00 (`balanced`). */
export type Pool = { id: number; sheetId: string; name: string; members: PoolMember[]; totalShare: string; balanced: boolean;
  parentId: number | null };

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

/** A pool member to add: a person or another pool of the sheet. */
export type MemberRef = { personId: number } | { memberPoolId: number };

/** Add a person or another pool to a pool with a share percent; refused (rejects) for a person already in
 *  the pool, a share that is not more than 0.00 and at most 100.00 with at most two decimals, or a pool
 *  already a member of a pool, that would contain itself or make a chain of pools more than three deep. */
export function addPoolMember(sheetId: string, by: string, poolId: number, member: MemberRef, share: string): Promise<Pool[]> {
  return postJson<Pool[]>(`${base(sheetId)}/${poolId}/members?by=${encodeURIComponent(by)}`, { ...member, share });
}

/** Add up cuts (two decimals) in cents, a blank cut adding nothing. */
function sumCuts(cuts: string[]): string {
  const cents = cuts.filter((c) => c !== '').reduce((total, c) => total + Math.round(Number(c) * 100), 0);
  return (cents / 100).toFixed(2);
}

/** A person's cut total: their cuts across every pool they are a member of, added up (a pool that shows
 *  no cut adds nothing). */
export function personCutTotal(pools: Pool[], personId: number): string {
  return sumCuts(pools.flatMap((p) => p.members.filter((m) => m.personId === personId).map((m) => m.cut)));
}

/** The sheet's cut total: every person's cuts in every pool added up. */
export function sheetCutTotal(pools: Pool[]): string {
  return sumCuts(pools.flatMap((p) => p.members.filter((m) => m.personId !== null).map((m) => m.cut)));
}
