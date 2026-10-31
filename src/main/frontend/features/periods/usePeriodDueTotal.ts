import { countsAtEnd, countsLive, statusAt, useClaims } from '../claims/useClaims';
import { gradeOn, retiredOn, usePeople } from '../people/usePeople';
import { claimDue, toCents } from '../rates/due';
import { useGrace } from '../claims/useGrace';
import { claimRate, useRates } from '../rates/useRates';
import { useFloor, useLatePct, usePolicy } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { useOpenings } from './useOpenings';
import { periodOf, usePeriods, type Period } from './usePeriods';

/** A person's due-total for a period: the dues as shown now, late charges included, of their claims whose due-by
 *  (as fixed at creation) falls in it that counted at the end of its last day (see countsAtEnd), leaving out their unbound OPEN claims once they are retired (a hold takes none out) (a claim whose due is n/a
 *  adds nothing); a closed or soft-closed period gives the figure fixed when it was closed (0.00 for a person added later). */
export function usePersonPeriodDueTotal(sheet: string): (period: Period, personId: string) => bigint {
  const periods = usePeriods(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const floor = useFloor(sheet);
  const late = { pct: useLatePct(sheet), today: useSheetToday(sheet), grace: useGrace(sheet) };
  const policy = usePolicy(sheet);
  const personOf = new Map(people.map((p) => [p.id, p]));
  return (period: Period, personId: string) => {
    if (period.closed || period.softClosed) return toCents(period.closedPersonTotals[personId] ?? '0.00');
    const person = personOf.get(personId);
    if (!person) return 0n;
    return claims
      .filter((c) => c.personId === personId && countsAtEnd(c, period.to, retiredOn(person, late.today), late.today) && periodOf(periods, c.dueBy)?.name === period.name)
      .reduce((sum, c) => sum + (claimDue(c, claimRate(rates, gradeOn(person, c.date), c), floor, late, policy) ?? 0n), 0n);
  };
}

/** A person's carried figure for a period: the dues as shown now, late charges included, of their claims whose due-by
 *  falls in a period ending before this one starts that counted at the end of this period's last day (see countsAtEnd)
 *  and were then still OPEN or PENDING by the status log, plus their opening balance for it; 0 when nothing is carried. A closed or soft-closed period
 *  gives the figure fixed when it was closed (0.00 for a person added later; worked as above for a close made before
 *  carried figures were kept). */
export function usePersonPeriodCarried(sheet: string): (period: Period, personId: string) => bigint {
  const periods = usePeriods(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const floor = useFloor(sheet);
  const late = { pct: useLatePct(sheet), today: useSheetToday(sheet), grace: useGrace(sheet) };
  const policy = usePolicy(sheet);
  const openings = useOpenings(sheet);
  const personOf = new Map(people.map((p) => [p.id, p]));
  return (period: Period, personId: string) => {
    if (period.closed || period.softClosed) {
      const fixed = period.closedPersonCarried?.[personId];
      if (fixed !== undefined) return toCents(fixed);
      if (period.closedPersonTotals[personId] === undefined) return 0n;
    }
    const person = personOf.get(personId);
    if (!person) return 0n;
    const opening = openings
      .filter((o) => o.personId === personId && o.period === period.name)
      .reduce((sum, o) => sum + toCents(o.amount), 0n);
    return opening + claims
      .filter((c) => {
        if (c.personId !== personId) return false;
        const from = periodOf(periods, c.dueBy);
        if (!from || from.to >= period.from) return false;
        const then = statusAt(c, period.to);
        return (then === 'OPEN' || then === 'PENDING')
          && countsAtEnd(c, period.to, retiredOn(person, late.today), late.today);
      })
      .reduce((sum, c) => sum + (claimDue(c, claimRate(rates, gradeOn(person, c.date), c), floor, late, policy) ?? 0n), 0n);
  };
}

/** A period's due-total: the sum of every person's due-total for it. */
export function usePeriodDueTotal(sheet: string): (period: Period) => bigint {
  const people = usePeople(sheet);
  const personDueTotal = usePersonPeriodDueTotal(sheet);
  return (period: Period) => people.reduce((sum, p) => sum + personDueTotal(period, p.id), 0n);
}

/** A period's classic due-total: the dues as shown now, late charges included, of every person's claims whose due-by
 *  falls in it, each counted by its status now (as the live totals count it) rather than its status at the period's
 *  end; a closed or soft-closed period gives the figure fixed when it was closed. */
export function useClassicPeriodDueTotal(sheet: string): (period: Period) => bigint {
  const periods = usePeriods(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const floor = useFloor(sheet);
  const late = { pct: useLatePct(sheet), today: useSheetToday(sheet), grace: useGrace(sheet) };
  const policy = usePolicy(sheet);
  return (period: Period) => {
    if (period.closed || period.softClosed) return toCents(period.closedTotal || '0.00');
    return people.reduce((sum, person) => {
      const retired = retiredOn(person, late.today);
      return claims
        .filter((c) => c.personId === person.id && countsLive(c, retired, late.today) && periodOf(periods, c.dueBy)?.name === period.name)
        .reduce((s, c) => s + (claimDue(c, claimRate(rates, gradeOn(person, c.date), c), floor, late, policy) ?? 0n), sum);
    }, 0n);
  };
}
