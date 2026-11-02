import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { hundredths } from '../rates/rates';
import { counted, useClaims } from '../claims/claims';
import type { Person } from '../people/people';
import { useCapCents } from '../settings/settings';

/** A member of a pool with their share percent (two decimals): a person (memberPool null), retired when the person has
 *  a retirement date set, or another pool (memberPool its name, person 0); the membership's from-date and to-date (''
 *  when none) and whether they include the sheet's today (a member outside them is left out entirely); and their cut
 *  of the pool's pot (two decimals), '' when the pool is not balanced, has no pot or has only retired members, or the member is
 *  outside their dates. A pool in none has the sheet's pot; a pool in another has its cut there. And, by the name of
 *  every period whose to-date is on or before the sheet's today, their cut as it stood at the end of that period (the
 *  pool, its members, the pot and the holds as they were then, with that day as the sheet's today), '' when none; a closed or soft-closed period (ended or not) instead shows their cut
 *  fixed when it was closed, '' when they were not then in the pool. */
export type PoolMember = {
  person: number;
  memberPool: string | null;
  name: string;
  share: string;
  retired: boolean;
  from: string;
  to: string;
  current: boolean;
  cut: string;
  cuts: Record<string, string>;
};

/** A pool: its members, the total of their shares (a retired member's counting as given, one outside their dates left out), and whether that is 100.00. */
export type Pool = { name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

export const poolsKey = (sheet: string) => ['pools', sheet] as const;

export const poolsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/pools`;

export const poolMembersPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/pool-members`;

/** The sheet's pools, in the order added. */
export function usePools(sheet: string) {
  return useQuery({
    queryKey: poolsKey(sheet),
    queryFn: () => getJson<Pool[]>(poolsPath(sheet)),
  });
}

/** A person's cut total in hundredths: their cuts across every pool they are a member of, a pool that shows no cut
 *  adding nothing. */
export function personCutCents(pools: Pool[], personId: number): bigint {
  let cents = 0n;
  for (const pool of pools) {
    for (const m of pool.members) {
      if (m.memberPool === null && m.person === personId && m.cut !== '') cents += hundredths(m.cut);
    }
  }
  return cents;
}

/** Each person's cap in hundredths: the sheet's cap in force on the date of their latest claim that counts (none
 *  counting: on the sheet's today). The cap covers their due-total plus their cut total, so their due-total is held
 *  to the cap less their cut total, not below 0.00; undefined when there is no cap. */
export function useCapCentsOf(sheet: string): (person: Person) => bigint | undefined {
  const capOn = useCapCents(sheet);
  const { data: pools = [] } = usePools(sheet);
  const { data: claims = [] } = useClaims(sheet);
  return (person) => {
    const latest = counted(claims.filter((c) => c.person === person.id), [person])
      .reduce<string | undefined>((max, c) => (max === undefined || c.date > max ? c.date : max), undefined);
    const cap = capOn(latest);
    if (cap === undefined) return undefined;
    const left = cap - personCutCents(pools, person.id);
    return left > 0n ? left : 0n;
  };
}
