import { counts, useClaims } from '../claims/useClaims';
import { claimDueNow } from '../overdue/useOverdue';
import { gradeOn, holdsById, usePeople } from '../people/usePeople';
import { rateFor, sumDues, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { usePeriods } from './usePeriods';

/** Each person's due-total for each period, by person id then period name: the figures of their claims
 *  that count whose date falls in it (a settled claim's frozen due, else the figure its last statement
 *  bound it to, else its live due, with its late charge while overdue), skipping those with none — the
 *  one fixed when the period was closed while it is closed (0.00 for a person added since). */
export function usePersonPeriodDueTotals(sheetId: string): Map<number, Map<string, string>> {
  const periods = usePeriods(sheetId);
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const settings = useSettings(sheetId).data;
  const floor = settings?.floor;
  const today = useSheetToday(sheetId).data?.today;
  const holds = holdsById(people.data);
  const totals = new Map<number, Map<string, string>>();
  for (const person of people.data ?? []) {
    const own = (claims.data ?? []).filter((c) => c.personId === person.id && counts(c, holds));
    const byPeriod = new Map<string, string>();
    for (const p of periods.data ?? []) {
      byPeriod.set(
        p.name,
        p.closed
          ? (p.personTotals[String(person.id)] ?? '0.00')
          : sumDues(
              own
                .filter((c) => p.from <= c.date && c.date <= p.to)
                .map((c) => claimDueNow(c, claims.data, rateFor(rates.data, gradeOn(person, c.date), c.date), floor, settings, today)),
            ),
      );
    }
    totals.set(person.id, byPeriod);
  }
  return totals;
}

/** Each period's due-total, by period name: the sum of the people's due-totals for it. */
export function usePeriodDueTotals(sheetId: string): Map<string, string> {
  const periods = usePeriods(sheetId);
  const perPerson = [...usePersonPeriodDueTotals(sheetId).values()];
  const totals = new Map<string, string>();
  for (const p of periods.data ?? []) {
    totals.set(p.name, sumDues(perPerson.map((byPeriod) => byPeriod.get(p.name) ?? '0.00')));
  }
  return totals;
}
