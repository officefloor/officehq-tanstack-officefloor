import { counts, fixedDue, useClaims } from '../claims/useClaims';
import { claimDueNow } from '../overdue/useOverdue';
import { gradeOn, holdsById, usePeople } from '../people/usePeople';
import { settingOn, useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { hundredths, money, rateFor, sumDues, useRates } from './useRates';

/** Each person's due-total, by person id: the sum of the figures of their claims that count (open (OPEN or PENDING)
 *  claims dated within one of their holds left out unless a statement binds them; a settled claim's frozen due, else the figure their last statement
 *  bound it to, else its live due, with its late charge while overdue),
 *  skipping those with none; 'n/a' when no rate for the grade they have on the sheet's today is in force
 *  then and no counting claim of theirs keeps a fixed figure or has a due (at the grade it was claimed under).
 *  A due-total is at most the sheet's cap in force on its today ('n/a' is not capped). */
export function useDueTotals(sheetId: string): Map<number, string> {
  const totals = new Map<number, string>();
  for (const [id, f] of useDueFigures(sheetId)) totals.set(id, f.total);
  return totals;
}

/** Each person's excess, by person id: how far the sum of their figures is above the sheet's cap; 0.00 when
 *  it is not (or there is no cap, or their due-total is 'n/a'). */
export function useExcess(sheetId: string): Map<number, string> {
  const excess = new Map<number, string>();
  for (const [id, f] of useDueFigures(sheetId)) excess.set(id, f.excess);
  return excess;
}

/** Each person's settled-total and open-total, by person id: the sum of the figures of their counting SETTLED
 *  claims, and of their counting open (OPEN or PENDING) claims, as in their due-total ('n/a' figures skipped).
 *  The two add up to the due-total before the cap. */
export function useSettledOpenTotals(sheetId: string): Map<number, { settled: string; open: string }> {
  const split = new Map<number, { settled: string; open: string }>();
  for (const [id, f] of useDueFigures(sheetId)) split.set(id, { settled: f.settled, open: f.open });
  return split;
}

/** Each person's due-total held to the cap, their excess over it, and its settled and open parts. */
function useDueFigures(sheetId: string): Map<number, { total: string; excess: string; settled: string; open: string }> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId);
  const settings = useSettings(sheetId).data;
  const holds = holdsById(people.data, today.data?.today);
  const cap = settingOn(settings, 'cap', today.data?.today);
  const totals = new Map<number, { total: string; excess: string; settled: string; open: string }>();
  for (const p of people.data ?? []) {
    const counting = (claims.data ?? []).filter((c) => c.personId === p.id && counts(c, holds, today.data?.today));
    const rated = rateFor(rates.data, p.grade, today.data?.today) !== undefined;
    const dues = counting.map((c) =>
      claimDueNow(c, claims.data, rateFor(rates.data, gradeOn(p, c.date), c.date, c.added), settings, today.data?.today, gradeOn(p, c.date)));
    const shown = rated || counting.some((c) => fixedDue(c) !== null && fixedDue(c) !== 'n/a') || dues.some((d) => d !== 'n/a');
    const sum = sumDues(dues);
    const over = shown && cap !== null && hundredths(sum) > hundredths(cap);
    totals.set(p.id, {
      total: !shown ? 'n/a' : over ? cap : sum,
      excess: over ? money(hundredths(sum) - hundredths(cap)) : '0.00',
      settled: sumDues(dues.filter((_, i) => counting[i].status === 'SETTLED')),
      open: sumDues(dues.filter((_, i) => counting[i].status !== 'SETTLED')),
    });
  }
  return totals;
}
