import { countsLive, useClaims } from '../claims/useClaims';
import { gradeOn, heldOn, usePeople } from '../people/usePeople';
import { claimDue, toCents } from '../rates/due';
import { rateOn, useRates } from '../rates/useRates';
import { useFloor, useLatePct, usePolicyDate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { periodOf, usePeriods, type Period } from './usePeriods';

/** A period's due-total: the dues, late charges included, of the claims that count whose date falls
 *  in it, leaving out a person's unbound OPEN claims dated within one of their holds (a claim whose due is n/a adds nothing); a closed
 *  period gives the due-total fixed when it was closed. */
export function usePeriodDueTotal(sheet: string): (period: Period) => bigint {
  const periods = usePeriods(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const floor = useFloor(sheet);
  const late = { pct: useLatePct(sheet), today: useSheetToday(sheet) };
  const policy = usePolicyDate(sheet);
  const personOf = new Map(people.map((p) => [p.id, p]));
  return (period: Period) =>
    period.closed ? toCents(period.closedTotal) : claims
      .filter((c) => personOf.has(c.personId) && countsLive(c, (date) => heldOn(personOf.get(c.personId)!, date)) && periodOf(periods, c.date)?.name === period.name)
      .reduce((sum, c) => sum + (claimDue(c, rateOn(rates, gradeOn(personOf.get(c.personId)!, c.date), c.date), floor, late, policy) ?? 0n), 0n);
}
