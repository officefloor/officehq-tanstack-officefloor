import { useOverdue } from '../overdue/useOverdue';
import { hundredths } from '../rates/useRates';
import { useSettledOpenTotals } from '../rates/useDueTotals';

/** Each person's score, by person id: their settled figures (the settled-total of their counting SETTLED
 *  claims) divided by 100, minus 5 for every overdue claim of theirs that counts, rounded half to even to a
 *  whole number. Worked in whole numbers, so exact. Missing until their figures are loaded. */
export function useScores(sheetId: string): Map<number, bigint> {
  const split = useSettledOpenTotals(sheetId);
  const overdue = useOverdue(sheetId);
  const scores = new Map<number, bigint>();
  for (const [id, s] of split) {
    const o = overdue.get(id);
    if (o === undefined) continue;
    scores.set(id, halfEven(hundredths(s.settled), 10_000n) - 5n * BigInt(o.count));
  }
  return scores;
}

/** n / d (d > 0) rounded half to even to a whole number. */
function halfEven(n: bigint, d: bigint): bigint {
  let q = n / d;
  let r = n % d;
  if (r < 0n) { q -= 1n; r += d; }
  if (2n * r > d || (2n * r === d && q % 2n !== 0n)) q += 1n;
  return q;
}
