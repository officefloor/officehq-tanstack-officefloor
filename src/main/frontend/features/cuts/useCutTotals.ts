import { type Person, usePeople } from '../people/usePeople';
import { type Pool, usePools } from '../pools/usePools';
import { money, hundredths } from '../rates/useRates';

/** Each person's cut total (two decimals), by person id: their cuts across every pool they are a member
 *  of, added up; a pool that shows no cut for them adds nothing. */
export function useCutTotals(sheetId: string): Map<number, string> {
  const people = usePeople(sheetId);
  const pools = usePools(sheetId);
  return cutTotalsOf(people.data, pools.data);
}

/** Each person's cut total, by person id, from these people and pools (as useCutTotals shows them); empty
 *  until both are loaded. */
export function cutTotalsOf(people: Person[] | undefined, pools: Pool[] | undefined): Map<number, string> {
  const totals = new Map<number, string>();
  if (people === undefined || pools === undefined) return totals;
  const cents = new Map<number, bigint>(people.map((p) => [p.id, 0n]));
  for (const pool of pools) {
    for (const m of pool.members) {
      if (m.personId === null || m.cut === null || !cents.has(m.personId)) continue;
      cents.set(m.personId, cents.get(m.personId)! + hundredths(m.cut));
    }
  }
  for (const [id, c] of cents) totals.set(id, money(c));
  return totals;
}
