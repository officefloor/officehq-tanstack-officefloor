import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/**
 * A pool's member: a person (personId) or another pool (poolId), its name, its share percent (two decimals), whether
 * it is a person with a retirement date set and its cut of the pool's pot ('': none — the pool is unbalanced, or has
 * no pot). A pool that is a member of none has the sheet's pot; one that is a member of another, its cut there.
 */
export type PoolMember = {
  personId: number | null;
  poolId: number | null;
  name: string;
  share: string;
  retired: boolean;
  cut: string;
};

/**
 * A pool on the sheet: its members in the order they were added, the total share (a retired member's share counts
 * as 0.00) and whether that total is exactly 100.00.
 */
export type Pool = { id: number; name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

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
