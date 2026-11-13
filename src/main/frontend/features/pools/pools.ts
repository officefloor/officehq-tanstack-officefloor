import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** A pool's member: the person, their share percent (two decimals) and whether a retirement date is set. */
export type PoolMember = { personId: number; name: string; share: string; retired: boolean };

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
