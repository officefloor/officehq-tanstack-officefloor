import { counts, fixedDue, useClaims } from '../claims/useClaims';
import { claimDueNow } from '../overdue/useOverdue';
import { gradeOn, holdsById, usePeople } from '../people/usePeople';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { rateFor, sumDues, useRates } from './useRates';

/** Each person's due-total, by person id: the sum of the figures of their claims that count (open (OPEN or PENDING)
 *  claims dated within one of their holds left out unless a statement binds them; a settled claim's frozen due, else the figure their last statement
 *  bound it to, else its live due, with its late charge while overdue),
 *  skipping those with none; 'n/a' when no rate for the grade they have on the sheet's today is in force
 *  then and no counting claim of theirs keeps a fixed figure or has a due (at the grade it was claimed under). */
export function useDueTotals(sheetId: string): Map<number, string> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId);
  const settings = useSettings(sheetId).data;
  const floor = settings?.floor;
  const holds = holdsById(people.data);
  const totals = new Map<number, string>();
  for (const p of people.data ?? []) {
    const counting = (claims.data ?? []).filter((c) => c.personId === p.id && counts(c, holds));
    const rated = rateFor(rates.data, p.grade, today.data?.today) !== undefined;
    const dues = counting.map((c) =>
      claimDueNow(c, claims.data, rateFor(rates.data, gradeOn(p, c.date), c.date), floor, settings, today.data?.today));
    totals.set(p.id, rated || counting.some((c) => fixedDue(c) !== null && fixedDue(c) !== 'n/a') || dues.some((d) => d !== 'n/a')
      ? sumDues(dues)
      : 'n/a');
  }
  return totals;
}
