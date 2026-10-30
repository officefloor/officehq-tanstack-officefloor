import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { peopleKey } from '../people/peopleQuery';
import { capLess, sumDue } from '../rates/ratesQuery';
import { claimsQuery, counts } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { valueOn } from '../settings/dated';
import { useCaps } from '../settings/settingsQuery';
import { useSheetToday } from '../sheet/useSheetToday';

/** A member of a pool: the person's id and name, their share percent with two decimals, whether they are retired, and their
 *  cut of the pool's pot with two decimals ("" when the pool is not balanced, there is no pot, every member is retired, or the member's dates do
 *  not include the sheet's today). A member that is
 *  another pool has its name as pool (null for a person) and personId 0; its cut is that pool's pot. periodCuts are its cut
 *  as it stood at the end of each period whose to-date is on or before the sheet's today ("" when it had none). */
export type PoolMember = {
  personId: number;
  name: string;
  share: string;
  retired: boolean;
  cut: string;
  pool: string | null;
  periodCuts: { period: string; cut: string }[];
};

/** A pool of the sheet: its name, its members, its total share (a retired member's counting as given) with two decimals,
 *  and whether that total is exactly 100.00. */
export type Pool = { name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

/** Under the people's key, so re-loading the sheet, or any change to its people (a retirement), re-reads the pools. */
export const poolsKey = (sheetId: string) => [...peopleKey(sheetId), 'pools'] as const;

export const poolsQuery = (sheetId: string) => ({
  queryKey: poolsKey(sheetId),
  queryFn: () => getJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`),
});

/** A person's cut total: their cut in every pool they are a member of, added up; a pool that shows no cut adds nothing. */
export function cutTotal(pools: Pool[], personId: number): string {
  const cuts = pools.flatMap((p) => p.members.filter((m) => m.pool === null && m.personId === personId && m.cut !== '').map((m) => m.cut));
  return sumDue(cuts);
}

/** The cap a person's due-total is held to, by person id: the sheet's cap in force on the date of their latest claim that counts
 *  (the sheet's today when they have none) less their cut total, not below 0.00 ("" for no cap). */
export function useCapFor(sheetId: string): (personId: number) => string {
  const caps = useCaps(sheetId);
  const today = useSheetToday(sheetId);
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const pools = useQuery(poolsQuery(sheetId)).data ?? [];
  return (personId) => {
    const latest = claims
      .filter((c) => c.personId === personId && counts(c, people, today))
      .reduce((max, c) => (c.date > max ? c.date : max), '');
    return capLess(valueOn(caps, latest || today) ?? '', cutTotal(pools, personId));
  };
}
