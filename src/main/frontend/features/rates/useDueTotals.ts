import { counts, fixedDue, useClaims } from '../claims/useClaims';
import { dueNow } from '../overdue/useOverdue';
import { heldIds, usePeople } from '../people/usePeople';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { due, rateFor, sumDues, useRates } from './useRates';

/** Each person's due-total, by person id: the sum of the figures of their claims that count (a held
 *  person's OPEN claims left out; a settled claim's frozen due, else the figure their last statement
 *  bound it to, else its live due, with its late charge while overdue),
 *  skipping those with none; 'n/a' when no rate for their grade is in force on the sheet's today and
 *  no counting claim of theirs keeps a fixed figure. */
export function useDueTotals(sheetId: string): Map<number, string> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId);
  const settings = useSettings(sheetId).data;
  const floor = settings?.floor;
  const held = heldIds(people.data);
  const totals = new Map<number, string>();
  for (const p of people.data ?? []) {
    const counting = (claims.data ?? []).filter((c) => c.personId === p.id && counts(c, held));
    const rated = rateFor(rates.data, p.grade, today.data?.today) !== undefined;
    totals.set(p.id, rated || counting.some((c) => fixedDue(c) !== null)
      ? sumDues(counting.map((c) =>
          dueNow(c, fixedDue(c) ?? due(c.amount, rateFor(rates.data, p.grade, c.date), floor), settings, today.data?.today)))
      : 'n/a');
  }
  return totals;
}
