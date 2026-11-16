import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { peopleKey } from '../people/people';

/** A member of a pool: a person (`personId`, `name`) or another pool (`pool`, its name; `personId` null), the `share` percent (two decimals; a JSON number, shown with two), and whether they are `retired` (a retirement
 *  date set, whatever the date): then their share counts as 0.00 toward the pool's total. `cut` is their cut of the sheet's
 *  pool's pot (two decimals, as text; a retired member's 0.00), null when the pool is not balanced or has no pot: a pool that
 *  is a member of none has the sheet's pot, a member pool its cut in the pool above. The membership may carry a `from` and a
 *  `to` date (yyyy-MM-dd, null for none); a member not `active` (its dates do not include the sheet's today) is left out
 *  entirely: not in the total, no cut. */
export type PoolMember = {
  personId: number | null;
  name: string;
  pool: string | null;
  share: number;
  retired: boolean;
  cut: string | null;
  from: string | null;
  to: string | null;
  active: boolean;
};

/** A pool of the sheet: its `name` (trimmed, unique on the sheet), its members in the order added, the `totalShare`
 *  of their shares, and whether it is `balanced` (a total of exactly 100.00). A person can be in several pools. */
export type Pool = { name: string; members: PoolMember[]; totalShare: number; balanced: boolean };

/** Under the people's key, so every change to a person (a retirement above all) reloads the totals. */
export const poolsKey = (sheetId: string) => [...peopleKey(sheetId), 'pools'];

/** The sheet's pools, in the order added. */
export function usePools(sheetId: string) {
  return useQuery({
    queryKey: poolsKey(sheetId),
    queryFn: () => getJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`),
  });
}

const toCents = (cut: string) => Math.round(Number(cut) * 100);
const fromCents = (cents: number) => (cents / 100).toFixed(2);

/** A person's cut total: their cuts across every pool they are a member of, added up (a pool that shows no cut adds
 *  nothing). Two decimals, as text. */
export function personCutTotal(pools: Pool[] | undefined, personId: number): string {
  let cents = 0;
  for (const pool of pools ?? [])
    for (const m of pool.members) if (m.pool === null && m.personId === personId && m.cut !== null) cents += toCents(m.cut);
  return fromCents(cents);
}

/** The sheet's cut total: everyone's cut total added up. Two decimals, as text. */
export function sheetCutTotal(pools: Pool[] | undefined): string {
  let cents = 0;
  for (const pool of pools ?? [])
    for (const m of pool.members) if (m.pool === null && m.cut !== null) cents += toCents(m.cut);
  return fromCents(cents);
}
