import { gradeOn, heldOnBy, usePeople } from '../people/usePeople';
import { rateOn, trimmedTotal, useRates } from '../rates/useRates';
import { useCap, useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';
import { claimed, live, overdueFigures, personDueCents, useClaims } from './useClaims';

/**
 * The sheet's totals: its claimed-total, everyone's counted (OPEN or SETTLED) claims added up
 * (not OPEN ones dated within their person's hold, unless stated), and its due-total, everyone's due-total
 * added up (a person whose due-total is n/a is skipped), and how many of the claims that count are
 * overdue, with their dues added up. Both due and overdue totals leave out the single highest and
 * lowest person's figure when three or more people have one. One place, so every screen repeating a total shows the same figure.
 */
export function useSheetTotals(sheet: string) {
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const claims = live(all, heldOnBy(people, today));
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const cap = useCap(sheet);
  const dues = people.map((p) =>
    personDueCents(claims.filter((c) => c.personId === p.id), floor, rateOn(rates, gradeOn(p, today), today), (d) => rateOn(rates, gradeOn(p, d), d), late, cap),
  );
  const over = people.map((p) =>
    overdueFigures(claims.filter((c) => c.personId === p.id), late, floor, (d) => rateOn(rates, gradeOn(p, d), d)),
  );
  return {
    claimedTotal: claimed(claims),
    dueTotal: trimmedTotal(dues),
    overdueCount: over.reduce((n, o) => n + o.count, 0),
    overdueTotal: trimmedTotal(over.map((o) => o.cents)),
  };
}
