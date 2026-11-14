import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/**
 * A pool's member: a person (personId) or another pool (poolId), its name, its share percent (two decimals), whether
 * it is a person with a retirement date set and its cut of the pool's pot ('': none — the pool is unbalanced, or has
 * no pot, or every member in it is retired, or the member's dates leave out the sheet's today) and its membership's
 * from-date and to-date (null: no start / no end), and its cut for each period (periodCuts, in the periods' order;
 * '': none): for a closed or soft-closed period, fixed as it stood when the period was closed (whatever its to-date);
 * for an open one whose to-date is on or before the sheet's today, from the pool, its members, the pot and the holds as
 * they were at its end. A pool that is a member of none has the sheet's pot; one that is a member of another, its cut there.
 */
export type PoolMember = {
  personId: number | null;
  poolId: number | null;
  name: string;
  share: string;
  retired: boolean;
  cut: string;
  from: string | null;
  to: string | null;
  periodCuts: { period: string; cut: string }[];
};

/**
 * A pool on the sheet: its members in the order they were added, the total share (a retired member's share counts
 * as given, one whose dates leave out the sheet's today is not counted) and whether that total is exactly 100.00.
 * A crew is also a pool (crew: true), listed after the pools under its name: its lead and members at equal shares,
 * the remainder on the lead; its members come from the crew, and it is a member of no other pool.
 */
export type Pool = { id: number; name: string; members: PoolMember[]; totalShare: string; balanced: boolean; crew: boolean };

/** The query key for the sheet's pools; under sheetKey, so re-loading the sheet re-reads them. */
export const poolsKey = (sheet: string) => [...sheetKey(sheet), 'pools'] as const;

/** The sheet's pools, in the order they were added. */
export function usePools() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: poolsKey(sheet),
    queryFn: () => getJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheet)}/pools`),
    enabled: !!sheet,
  });
}

/** Two-decimal cuts added up, skipping blank ones; added in cents so it is exact. */
export function cutTotal(cuts: string[]): string {
  const cents = cuts.filter((c) => c !== '').reduce((sum, c) => sum + Math.round(Number(c) * 100), 0);
  return (cents / 100).toFixed(2);
}

/** A person's cuts across every pool they are a member of (a pool showing no cut adds nothing). */
export function personCuts(pools: Pool[], personId: number): string[] {
  return pools.flatMap((p) => p.members.filter((m) => m.personId === personId).map((m) => m.cut));
}
