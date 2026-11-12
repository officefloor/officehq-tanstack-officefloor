import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';

/** A member of a pool: a person on the sheet, or another pool (`memberPoolId` set, `personId` null), and its share
 *  percent (two decimals). `retired` once a retirement date is set for them, whatever the date: their share then
 *  counts as 0.00 toward the total. `cut` is their cut of the pool's pot (two decimals; 0.00 when retired or held
 *  on the sheet's today, their share then spread over the members with a cut in proportion to their shares), null
 *  when the pool is not balanced or has no pot: a pool that is a member of none has the sheet's pot, a member
 *  pool's pot is its cut in the pool above. */
export type PoolMember = {
  personId: number | null;
  memberPoolId: number | null;
  name: string;
  share: string;
  retired: boolean;
  cut: string | null;
};

/** A pool on the sheet: its members, the total of their shares (two decimals) and whether it is exactly 100.00. */
export type Pool = { id: number; name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

/** Under the sheet's key, so loading the sheet again re-reads its pools. */
export const poolsKey = (sheet: string) => [...sheetKey(sheet), 'pools'] as const;

export function usePools(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(poolsKey(sheet), asOf),
    queryFn: () => getJson<Pool[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/pools`, asOf)),
    enabled: sheet !== '',
  });
}
