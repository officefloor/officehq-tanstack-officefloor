import { counts, fixedDue, useClaims, type Claim } from '../claims/useClaims';
import { heldIds, usePeople } from '../people/usePeople';
import { due, rateFor, sumDues, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
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

/** Each person's overdue figures, by person id: how many of their claims that count are overdue, and
 *  the sum of those claims' figures (a stated claim's bound figure, else its live due), skipping
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
      total: sumDues(overdue.map((c) => fixedDue(c) ?? due(c.amount, rateFor(rates.data, p.grade, c.date), settings.floor))),
    });
  }
  return figures;
}
