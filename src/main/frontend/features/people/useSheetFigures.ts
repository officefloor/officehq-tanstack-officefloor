import { countsLive, useClaims } from '../claims/useClaims';
import { isOverdue } from '../claims/overdue';
import { capped, claimDue, dueTotal, excess, formatCents } from '../rates/due';
import { rateOn, useRates } from '../rates/useRates';
import { useCap, useFloor, useLatePct, usePolicyDate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { gradeOn, heldOn, usePeople, type Person } from './usePeople';

export const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'] as const;

// The figures the people list, its totals, the summary and the header are all worked from, so
// every place that repeats a figure shows the same one.
export function useSheetFigures(sheet: string) {
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = { pct: useLatePct(sheet), today };
  const policy = usePolicyDate(sheet);
  const cap = useCap(sheet);
  const byId = new Map(people.map((p) => [p.id, p]));
  const live = claims.filter((c) => {
    const person = byId.get(c.personId);
    return person !== undefined && countsLive(c, (date) => heldOn(person, date));
  });
  // A claim is worked at the grade in force on its date; the row shows the one in force today.
  // The sum of a person's figures, before the cap.
  const uncappedOf = (person: Person) =>
    dueTotal(
      live.filter((c) => c.personId === person.id),
      (date) => rateOn(rates, gradeOn(person, date), date),
      rateOn(rates, gradeOn(person, today), today),
      floor,
      late,
      policy,
    );
  // A person's due-total: at most the sheet's cap (n/a is not capped); everything that adds up
  // due-totals uses it. Their excess is how far the sum of their figures is above the cap.
  const totalOf = (person: Person) => capped(uncappedOf(person), cap);
  const excessOf = (person: Person) => excess(uncappedOf(person), cap);
  const overdue = live.filter((c) => isOverdue(c, today));
  const overdueTotal = (cs: typeof overdue) =>
    formatCents(cs.reduce((sum, c) => sum + (claimDue(c, rateOn(rates, gradeOn(byId.get(c.personId)!, c.date), c.date), floor, late, policy) ?? 0n), 0n));
  // The sheet's due-total: the people's due-totals added up, skipping n/a.
  const sheetDueTotal = formatCents(people.reduce((sum, p) => {
    const due = totalOf(p);
    return due === null ? sum : sum + due;
  }, 0n));
  const onSheet = new Set(people.map((p) => p.id));
  const sheetClaims = claims.filter((c) => onSheet.has(c.personId));
  const statusCount = (s: (typeof STATUSES)[number]) => sheetClaims.filter((c) => c.status === s).length;
  return { people, claims, rates, today, byId, live, totalOf, excessOf, overdue, overdueTotal, sheetDueTotal, statusCount };
}
