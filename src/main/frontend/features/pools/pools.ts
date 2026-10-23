import { useQueries, useQuery } from '@tanstack/react-query';
import { getJson, getJsonNow } from '../../api/http';
import { usePeriods } from '../periods/periods';
import { useSheetToday } from '../sheet/sheet';

/** A member of a pool — a person, or another pool (poolId; personId then null) — with their share percent, whether
 *  they are retired by the sheet's today (their share still counts toward the pool's total but they cut 0.00, their
 *  share spread over the others), and their cut of the pool's pot ('' when the pool is unbalanced, there is no pot
 *  or every member is retired). A pool that is a member of none has the sheet's
 *  pot; a member pool's pot is its cut in the pool above. A membership may carry a from- and a to-date ('' when
 *  open); a member whose dates leave out the sheet's today (outside) counts nothing toward the total and has no cut. */
export type PoolMember = {
  personId: number | null;
  name: string;
  share: string;
  retired: boolean;
  cut: string;
  poolId: number | null;
  from: string;
  to: string;
  outside: boolean;
};

/** A pool on the sheet: its members in the order they were added, the total of their shares and whether it is
 *  balanced (the total is exactly 100.00). A crew is also listed as a pool, after the pools: crewId is its crew's id
 *  (null for a pool), its members the lead and the crew's members at equal shares. */
export type Pool = {
  id: number;
  name: string;
  members: PoolMember[];
  totalShare: string;
  balanced: boolean;
  crewId: number | null;
};

export const poolsKey = (sheetId: string) => ['sheet', sheetId, 'pools'] as const;

/** The sheet's pools, in the order they were added. */
export function usePools(sheetId: string) {
  return useQuery({
    queryKey: poolsKey(sheetId),
    queryFn: () => getJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`),
  });
}

/** The cuts of each period: for each period whose to-date is on or before the sheet's today, or that is closed or
 *  soft-closed, by period name, the sheet's pools as they stood at the end of its to-date (that day as the sheet's
 *  today) — with each member's cut, once the period is closed or soft-closed, the one fixed when it was closed. Keyed
 *  under the sheet, so a load of the sheet re-reads them. */
export function usePeriodCuts(sheetId: string): { period: string; pools: Pool[] }[] {
  const today = useSheetToday(sheetId);
  const periods = (usePeriods(sheetId).data ?? []).filter((p) => p.closed || p.soft || (today !== '' && p.to <= today));
  const results = useQueries({
    queries: periods.map((p) => {
      const fixed = p.closed || p.soft;
      const query = `asOf=${encodeURIComponent(p.to)}${fixed ? `&period=${encodeURIComponent(p.name)}` : ''}`;
      return {
        queryKey: [...poolsKey(sheetId), 'asOf', p.to, fixed ? p.name : ''] as const,
        queryFn: () => getJsonNow<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools?${query}`),
      };
    }),
  });
  return periods.map((p, i) => ({ period: p.name, pools: results[i]?.data ?? [] }));
}

/** A member's cut in a pool as it stood in an earlier read of the pools ('' when there was none). */
export function cutThen(pools: Pool[], poolId: number, member: PoolMember): string {
  const pool = pools.find((p) => p.id === poolId);
  const m = pool?.members.find((x) => x.poolId !== null ? x.poolId === member.poolId : x.personId === member.personId);
  return m?.cut ?? '';
}

function cents(amount: string): number {
  return Math.round(Number(amount) * 100);
}

function formatCents(total: number): string {
  const abs = Math.abs(total);
  return `${total < 0 ? '-' : ''}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`;
}

/** A person's cuts, in cents, across every pool they are a member of (a pool showing no cut adds nothing). */
function personCutCents(pools: Pool[], personId: number): number {
  return pools
    .flatMap((p) => p.members)
    .filter((m) => m.poolId === null && m.personId === personId && m.cut !== '')
    .reduce((total, m) => total + cents(m.cut), 0);
}

/** A person's cut total: their cuts across every pool they are a member of, added up. */
export function personCutTotal(pools: Pool[], personId: number): string {
  return formatCents(personCutCents(pools, personId));
}

/** The sheet's cut total: every listed person's cut total added up. */
export function sheetCutTotal(pools: Pool[], personIds: number[]): string {
  return formatCents(personIds.reduce((total, id) => total + personCutCents(pools, id), 0));
}
