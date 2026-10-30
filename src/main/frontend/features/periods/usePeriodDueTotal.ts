import { countsAtEnd, useClaims } from '../claims/useClaims';
import { gradeOn, heldOn, usePeople } from '../people/usePeople';
import { claimDue, toCents } from '../rates/due';
import { rateOn, useRates } from '../rates/useRates';
import { useFloor, useLatePct, usePolicyDate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { periodOf, usePeriods, type Period } from './usePeriods';

/** A person's due-total for a period: the dues as shown now, late charges included, of their claims whose date
 *  falls in it that counted at the end of its last day (see countsAtEnd), leaving out their unbound OPEN claims dated within one of their holds (a claim whose due is n/a
 *  adds nothing); a closed or soft-closed period gives the figure fixed when it was closed (0.00 for a person added later). */
export function usePersonPeriodDueTotal(sheet: string): (period: Period, personId: string) => bigint {
  const periods = usePeriods(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const floor = useFloor(sheet);
  const late = { pct: useLatePct(sheet), today: useSheetToday(sheet) };
  const policy = usePolicyDate(sheet);
  const personOf = new Map(people.map((p) => [p.id, p]));
  return (period: Period, personId: string) => {
    if (period.closed || period.softClosed) return toCents(period.closedPersonTotals[personId] ?? '0.00');
    const person = personOf.get(personId);
    if (!person) return 0n;
    return claims
      .filter((c) => c.personId === personId && countsAtEnd(c, period.to, (date) => heldOn(person, date), late.today) && periodOf(periods, c.date)?.name === period.name)
      .reduce((sum, c) => sum + (claimDue(c, rateOn(rates, gradeOn(person, c.date), c.date), floor, late, policy) ?? 0n), 0n);
  };
}

/** A period's due-total: the sum of every person's due-total for it. */
export function usePeriodDueTotal(sheet: string): (period: Period) => bigint {
  const people = usePeople(sheet);
  const personDueTotal = usePersonPeriodDueTotal(sheet);
  return (period: Period) => people.reduce((sum, p) => sum + personDueTotal(period, p.id), 0n);
}
