import { counts, fixedDue, useClaims, type Claim } from '../claims/useClaims';
import { gradeOn, heldIds, usePeople } from '../people/usePeople';
import { due, isCorrection, rateFor, sumDues, useRates, withCharge } from '../rates/useRates';
import { useSettings, type Settings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';

/** A claim's due-by: its date (yyyy-mm-dd) plus the sheet's terms in days. */
export function dueBy(date: string, terms: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + terms);
  return d.toISOString().slice(0, 10);
}

/** Whether a claim is overdue: it is OPEN and its due-by is before the sheet's today. */
export function isOverdue(c: Claim, terms: number, today: string): boolean {
  return c.status === 'OPEN' && dueBy(c.date, terms) < today;
}

/** A claim's due now: its figure (a settled claim's frozen due, else the one its last statement
 *  bound it to, else its floored live due), plus, while it is overdue, the sheet's late percent of
 *  that figure for every full 30 days it is overdue, rounded like a due. The charge is never bound by
 *  a statement: it follows the sheet's today until settling freezes it in. A correction (a negative
 *  amount) has no late charge. Just the figure until the settings and the sheet's today are loaded. */
export function dueNow(c: Claim, figure: string, settings: Settings | undefined, today: string | undefined): string {
  if (settings === undefined || today === undefined || !isOverdue(c, settings.terms, today) || isCorrection(c.amount)) {
    return figure;
  }
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${dueBy(c.date, settings.terms)}T00:00:00Z`)) / 86_400_000;
  return withCharge(figure, settings.latePct, Math.floor(days / 30));
}

/** Each person's overdue figures, by person id: how many of their claims that count are overdue, and
 *  the sum of those claims' dues now (a stated claim's bound figure, else its live due, with its late
 *  charge), skipping
 *  those with none. Empty until the terms and the sheet's today are loaded. */
export function useOverdue(sheetId: string): Map<number, { count: number; total: string }> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const settings = useSettings(sheetId).data;
  const today = useSheetToday(sheetId).data?.today;
  const held = heldIds(people.data);
  const figures = new Map<number, { count: number; total: string }>();
  if (settings === undefined || today === undefined) return figures;
  for (const p of people.data ?? []) {
    const overdue = (claims.data ?? []).filter(
      (c) => c.personId === p.id && counts(c, held) && isOverdue(c, settings.terms, today),
    );
    figures.set(p.id, {
      count: overdue.length,
      total: sumDues(overdue.map((c) =>
        dueNow(c, fixedDue(c) ?? due(c.amount, rateFor(rates.data, gradeOn(p, c.date), c.date), settings.floor), settings, today))),
    });
  }
  return figures;
}
