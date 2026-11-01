import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';
import { hundredths } from '../rates/useRates';

/** A pool's member as the server sends it: the person (or, for a member that is another pool, null
 *  and that pool's name in `pool`), their share (two decimals) and whether they are retired (a
 *  retirement date set, whatever the date), and their cut of the pool's pot (two decimals; null when
 *  the pool is unbalanced or has no pot, or the member is out of their dates), their from- and
 *  to-dates (null when none) and whether those include the sheet's today (a member out of them is
 *  left out of the pool). A pool that is a member of none has the sheet's pot; one that is a member
 *  of another has its cut there. `cuts` is their cut at the end of each period whose to-date is on or
 *  before the sheet's today, or the cut fixed when a closed period was closed (whatever its to-date),
 *  by period name in the periods' order (null when they had none then). */
export type PoolMember = {
  personId: number | null;
  name: string;
  share: string;
  retired: boolean;
  cut: string | null;
  pool: string | null;
  from: string | null;
  to: string | null;
  current: boolean;
  cuts: Record<string, string | null>;
};

/** A pool as the server sends it: its name, its members in the order added, the total of their
 *  shares (two decimals; a retired member's counts as given), whether it is exactly 100.00 and
 *  whether it is a crew's (listed after the pools: the lead and the members at equal shares of
 *  100.00, the remainder on the lead; its members come from the crew, not the pool's form). */
export type Pool = { name: string; members: PoolMember[]; totalShare: string; balanced: boolean; crew: boolean };

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

/** The cuts of the person members of `pools` (just `personId`'s when given) added up, in cents; a
 *  pool member that is another pool, or a member with no cut, adds nothing. */
export function cutTotalCents(pools: Pool[], personId?: number): bigint {
  return pools
    .flatMap((p) => p.members)
    .filter((m) => m.personId !== null && m.cut !== null && (personId === undefined || m.personId === personId))
    .reduce((sum, m) => sum + hundredths(m.cut as string), 0n);
}
