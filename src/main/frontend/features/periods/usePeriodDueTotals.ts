import { counts, fixedDue, useClaims } from '../claims/useClaims';
import { dueNow } from '../overdue/useOverdue';
import { gradeOn, heldIds, usePeople } from '../people/usePeople';
import { due, rateFor, sumDues, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { periodOf, usePeriods } from './usePeriods';

/** Each period's due-total, by period name: the figures of the claims that count whose date falls in
 *  it (a settled claim's frozen due, else the figure its last statement bound it to, else its live due,
 *  with its late charge while overdue), skipping those with none — the fixed one once it is closed. */
export function usePeriodDueTotals(sheetId: string): Map<string, string> {
  const periods = usePeriods(sheetId);
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const settings = useSettings(sheetId).data;
  const floor = settings?.floor;
  const today = useSheetToday(sheetId).data?.today;
  const byId = new Map((people.data ?? []).map((person) => [person.id, person]));
  const held = heldIds(people.data);
  const totals = new Map<string, string>();
  for (const p of periods.data ?? []) {
    totals.set(
      p.name,
      p.closed && p.closedTotal !== null
        ? p.closedTotal
        : sumDues(
            (claims.data ?? [])
              .filter((c) => counts(c, held) && periodOf([p], c.date) !== undefined)
              .map((c) => dueNow(c, fixedDue(c) ?? due(c.amount, rateFor(rates.data, gradeOn(byId.get(c.personId), c.date), c.date), floor), settings, today)),
          ),
    );
  }
  return totals;
}
