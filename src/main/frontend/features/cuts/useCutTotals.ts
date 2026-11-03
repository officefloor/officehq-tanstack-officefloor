import { usePeople } from '../people/usePeople';
import { usePools } from '../pools/usePools';
import { money, hundredths } from '../rates/useRates';

/** Each person's cut total (two decimals), by person id: their cuts across every pool they are a member
 *  of, added up; a pool that shows no cut for them adds nothing. */
export function useCutTotals(sheetId: string): Map<number, string> {
  const people = usePeople(sheetId);
  const pools = usePools(sheetId);
  const totals = new Map<number, string>();
  if (people.data === undefined || pools.data === undefined) return totals;
  const cents = new Map<number, bigint>(people.data.map((p) => [p.id, 0n]));
  for (const pool of pools.data) {
    for (const m of pool.members) {
      if (m.personId === null || m.cut === null || !cents.has(m.personId)) continue;
      cents.set(m.personId, cents.get(m.personId)! + hundredths(m.cut));
    }
  }
  for (const [id, c] of cents) totals.set(id, money(c));
  return totals;
}
