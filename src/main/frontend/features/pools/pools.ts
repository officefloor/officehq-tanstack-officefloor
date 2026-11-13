import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';
import { dueTotal } from '../rates/rates';

/** A member of a pool: a person on the sheet, or another pool (`memberPoolId` set, `personId` null), and its share
 *  percent (two decimals). `retired` once a retirement date is set for them, whatever the date: their share still
 *  counts toward the total. `cut` is their cut of the pool's pot (two decimals; 0.00 when retired or held on the
 *  sheet's today, their share then spread over the members with a cut in proportion to their shares), null when
 *  the pool is not balanced, has no pot, or every member is retired: a pool that is a member of none has the sheet's pot, a member
 *  pool's pot is its cut in the pool above. `from` and `to` bound the dates the membership counts (null for no
 *  bound): a member whose dates do not include the sheet's today counts toward neither the total nor the balance,
 *  and its `cut` is null. `periodCuts` has, for each period whose to-date is on or before the sheet's today (in the
 *  periods' order), the member's cut as it stood at the end of that period (the pool, its members, the pot and the
 *  holds as they were then, with that day as the sheet's today), null when there was none. */
export type PoolMember = {
  personId: number | null;
  memberPoolId: number | null;
  name: string;
  share: string;
  retired: boolean;
  cut: string | null;
  from: string | null;
  to: string | null;
  periodCuts: { period: string; cut: string | null }[];
};

/** A pool on the sheet: its members, the total of their shares (two decimals) and whether it is exactly 100.00.
 *  `crew` for a crew shown as a pool (its id the negated crew id): its members are the crew's lead and members at
 *  equal shares, and no member is added to it here. */
export type Pool = { id: number; name: string; members: PoolMember[]; totalShare: string; balanced: boolean; crew: boolean };

/** Under the sheet's key, so loading the sheet again re-reads its pools. */
export const poolsKey = (sheet: string) => [...sheetKey(sheet), 'pools'] as const;

/** A person's cut total: their cuts across every pool they are a member of, a pool showing no cut adding nothing. */
export const cutTotal = (pools: Pool[], personId: number) =>
  dueTotal(pools.flatMap((p) => p.members.filter((m) => m.personId === personId && m.cut !== null).map((m) => m.cut as string)));

export function usePools(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(poolsKey(sheet), asOf),
    queryFn: () => getJson<Pool[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/pools`, asOf)),
    enabled: sheet !== '',
  });
}
