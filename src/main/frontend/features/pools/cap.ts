import { useQuery } from '@tanstack/react-query';
import { formatCents, hundredths } from '../rates/api';
import { useCap } from '../settings/api';
import { liveKey } from '../sheet/api';
import { listPools, personCutTotal, poolsKey } from './api';

/** What a person's due-total is held to: the sheet's `cap` (null: no cap) less their cut total, not below 0.00. */
export function capLessCuts(cap: string | null, cutTotal: string): string | null {
  if (cap === null) return null;
  const room = hundredths(cap) - hundredths(cutTotal);
  return formatCents(room < 0n ? 0n : room);
}

/** Each person's cap (by id; null: no cap): the sheet's cap in force on its today less their cut total. */
export function useCapOf(sheetId: string, today: string, live = false): (personId: number) => string | null {
  const cap = useCap(sheetId, today, live);
  const pools = useQuery({ queryKey: liveKey(poolsKey(sheetId), live), queryFn: () => listPools(sheetId, live) });
  return (personId) => capLessCuts(cap, personCutTotal(pools.data ?? [], personId));
}
