import { useQuery } from '@tanstack/react-query';
import { formatCents, hundredths } from '../rates/api';
import { useCap } from '../settings/api';
import { claimsKey, counts, listClaims, liveClaims } from '../claims/api';
import { listPeople, peopleKey } from '../people/api';
import { liveKey } from '../sheet/api';
import { listPools, personCutTotal, poolsKey } from './api';

/** What a person's due-total is held to: the sheet's `cap` (null: no cap) less their cut total, not below 0.00. */
export function capLessCuts(cap: string | null, cutTotal: string): string | null {
  if (cap === null) return null;
  const room = hundredths(cap) - hundredths(cutTotal);
  return formatCents(room < 0n ? 0n : room);
}

/** Each person's cap (by id; null: no cap): the sheet's cap in force on the date of their latest claim that
 *  counts (none: on its today) less their cut total. */
export function useCapOf(sheetId: string, today: string, live = false): (personId: number) => string | null {
  const cap = useCap(sheetId, live);
  const pools = useQuery({ queryKey: liveKey(poolsKey(sheetId), live), queryFn: () => listPools(sheetId, live) });
  const people = useQuery({ queryKey: liveKey(peopleKey(sheetId), live), queryFn: () => listPeople(sheetId, live) });
  const claims = useQuery({ queryKey: liveKey(claimsKey(sheetId), live), queryFn: () => listClaims(sheetId, live) });
  return (personId) => {
    const person = (people.data ?? []).filter((p) => p.id === personId);
    const latest = liveClaims((claims.data ?? []).filter((c) => c.personId === personId), person)
      .filter(counts).reduce((d, c) => (c.date > d ? c.date : d), '');
    return capLessCuts(cap(latest || today), personCutTotal(pools.data ?? [], personId));
  };
}
