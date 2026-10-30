import { countsLive, useClaims } from '../claims/useClaims';
import { gradeOn, usePeople } from '../people/usePeople';
import { claimDue, toCents } from '../rates/due';
import { rateOn, useRates } from '../rates/useRates';
import { useFloor, useLatePct, usePolicyDate, useTerms } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { periodOf, usePeriods, type Period } from './usePeriods';

/** A period's due-total: the dues, late charges included, of the claims that count whose date falls
 *  in it, leaving out a held person's OPEN claims (a claim whose due is n/a adds nothing); a closed
 *  period gives the due-total fixed when it was closed. */
export function usePeriodDueTotal(sheet: string): (period: Period) => bigint {
  const periods = usePeriods(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const floor = useFloor(sheet);
  const late = { pct: useLatePct(sheet), terms: useTerms(sheet), today: useSheetToday(sheet) };
  const policy = usePolicyDate(sheet);
  const personOf = new Map(people.map((p) => [p.id, p]));
  const heldOf = new Map(people.map((p) => [p.id, p.held]));
  return (period: Period) =>
    period.closed ? toCents(period.closedTotal) : claims
      .filter((c) => personOf.has(c.personId) && countsLive(c, heldOf.get(c.personId)!) && periodOf(periods, c.date)?.name === period.name)
      .reduce((sum, c) => sum + (claimDue(c, rateOn(rates, gradeOn(personOf.get(c.personId)!, c.date), c.date), floor, late, policy) ?? 0n), 0n);
}
