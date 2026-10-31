import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';

/** A pool's member as the server sends it: the person (or, for a member that is another pool, null
 *  and that pool's name in `pool`), their share (two decimals) and whether they are retired (a
 *  retirement date set, whatever the date), and their cut of the pool's pot (two decimals; null when
 *  the pool is unbalanced or has no pot). A pool that is a member of none has the sheet's pot; one
 *  that is a member of another has its cut there. */
export type PoolMember = {
  personId: number | null;
  name: string;
  share: string;
  retired: boolean;
  cut: string | null;
  pool: string | null;
};

/** A pool as the server sends it: its name, its members in the order added, the total of their
 *  shares (two decimals; a retired member's counts 0.00) and whether it is exactly 100.00. */
export type Pool = { name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

/** A share percent: up to two decimals. */
export const SHARE = /^\d{1,3}(\.\d{1,2})?$/;

export const poolsKey = (sheet: string) => ['sheet', sheet, 'pools'] as const;

/** The sheet's pools, in the order added, re-read whenever the sheet is loaded. */
export function usePools(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(poolsKey(sheet), asOf),
    queryFn: () => getJson<Pool[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/pools`, asOf)),
    staleTime: 0,
    gcTime: 0,
  });
}
