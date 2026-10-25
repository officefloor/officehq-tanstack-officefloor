import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A sheet's pool as the store holds it: a trimmed name, on the sheet at most once, and its members, each a person
 *  of the sheet at most once with a share percent. `totalShare` is the members' shares added up, a retired member's
 *  (one with a retirement date set, whatever the date) counting as given; `balanced` is whether it is exactly 100.00. A crew is also a pool (`crew`; its id the crew's, negated), listed after the sheet's
 *  pools: its lead and members at equal shares of 100.00, the remainder on the lead, cut from the sheet's pot. */
export type Pool = { id: number; name: string; totalShare: string; balanced: boolean; crew: boolean; members: PoolMember[] };

/** A pool's member: the person, or another pool (`poolId`; the other null), its name, its share (two decimals) and its
 *  cut of the pool's pot (two decimals; "" when the pool is not balanced or has no pot, or every member is retired, or the member's dates do not include the
 *  sheet's today — such a member is left out of the total share and balance too). A pool that is a member of
 *  none has the sheet's pot; a pool that is a member of another has its cut there as its pot. */
export type PoolMember = {
  personId: number | null; poolId: number | null; name: string; share: string; cut: string; periodCuts: PeriodCut[];
};

/** A member's cut as it stood at the end of a period whose to-date is on or before the sheet's today (the pool, its
 *  members, the pot and the holds as they were then, that day the sheet's today): "" when there was none. The
 *  periods by from-date. */
export type PeriodCut = { period: string; cut: string };

export const poolsQueryKey = (sheetId: string) => ['sheet', sheetId, 'pools'] as const;

export const poolsPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/pools`;

/** The sheet's pools, in the order they were added. */
export function usePools() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: poolsQueryKey(sheet ?? ''),
    queryFn: () => getJson<Pool[]>(poolsPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** The cuts of the given person (every person when null) across every pool they are a member of, added up; a pool
 *  that shows no cuts adds nothing. Summed in whole cents, so the total is exact. */
export function cutTotalOf(pools: Pool[], personId: number | null): string {
  const cents = pools
    .flatMap((p) => p.members)
    .filter((m) => m.personId != null && (personId == null || m.personId === personId) && m.cut !== '')
    .reduce((n, m) => n + Math.round(Number(m.cut) * 100), 0);
  return (cents / 100).toFixed(2);
}
