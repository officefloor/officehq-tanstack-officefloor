import { counts, fixedDue, isOpen, useClaims, type Claim } from '../claims/useClaims';
import { gradeOn, heldIds, usePeople } from '../people/usePeople';
import { due, isCorrection, rateFor, sumDues, useRates, withCharge } from '../rates/useRates';
import { useSettings, type Settings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';

/** Whether a claim is overdue: it is open (OPEN or PENDING) and its due-by is before the sheet's today. */
export function isOverdue(c: Claim, today: string): boolean {
  return isOpen(c) && c.dueBy < today;
}

/** Whether a claim's live figures (its rate step and late charge) round half up: when the claim is
 *  dated before the sheet's policy date (blank meaning the sheet's today); else they round half to
 *  even. A correction rounds by the date of the claim it corrects (its person's claim with that
 *  number), not its own. Half to even until the settings and the sheet's today are loaded. */
export function roundsHalfUp(c: Claim, claims: Claim[] | undefined, settings: Settings | undefined, today: string | undefined): boolean {
  const policy = settings === undefined ? undefined : settings.policyDate ?? today;
  if (policy === undefined) return false;
  const corrected = c.corrects === null
    ? undefined
    : (claims ?? []).find((o) => o.personId === c.personId && o.number === c.corrects);
  return (corrected?.date ?? c.date) < policy;
}

/** A claim's due now: its figure (a settled claim's frozen due, else the one its last statement
 *  bound it to, else its floored live due), plus, while it is overdue, the sheet's late percent of
 *  that figure for every full 30 days it is overdue, rounded like a due. The charge is never bound by
 *  a statement: it follows the sheet's today until settling freezes it in. A correction (a negative
 *  amount) has no late charge. Just the figure until the settings and the sheet's today are loaded. */
export function dueNow(c: Claim, figure: string, settings: Settings | undefined, today: string | undefined, halfUp: boolean): string {
  if (settings === undefined || today === undefined || !isOverdue(c, today) || isCorrection(c.amount)) {
    return figure;
  }
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${c.dueBy}T00:00:00Z`)) / 86_400_000;
  return withCharge(figure, settings.latePct, Math.floor(days / 30), halfUp);
}

/** A claim's due now (see `dueNow`) from its fixed figure, else its live due at this rate (a
 *  two-decimal percent; undefined when none applies) and floor, every rounding by the claim's policy
 *  (see `roundsHalfUp`) — the other claims on the sheet are what a correction is looked up in. */
export function claimDueNow(c: Claim, claims: Claim[] | undefined, pct: string | undefined, floor: string | undefined,
  settings: Settings | undefined, today: string | undefined): string {
  const up = roundsHalfUp(c, claims, settings, today);
  return dueNow(c, fixedDue(c) ?? due(c.amount, pct, floor, up), settings, today, up);
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
      (c) => c.personId === p.id && counts(c, held) && isOverdue(c, today),
    );
    figures.set(p.id, {
      count: overdue.length,
      total: sumDues(overdue.map((c) =>
        claimDueNow(c, claims.data, rateFor(rates.data, gradeOn(p, c.date), c.date), settings.floor, settings, today))),
    });
  }
  return figures;
}
