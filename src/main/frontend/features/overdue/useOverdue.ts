import { counts, fixedDue, isDeferred, isOpen, useClaims, type Claim } from '../claims/useClaims';
import { gradeOn, holdsById, usePeople } from '../people/usePeople';
import { atLeast, due, isCorrection, rateFor, sumDues, useRates, withCharge } from '../rates/useRates';
import { settingOn, useSettings, type Settings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';

/** The day (yyyy-mm-dd) a claim's overdue days count from: its due-by plus the grace days, in force on the claim's
 *  date, of the grade its person had on that date (`grade`); its due-by when that grade has none then. */
export function overdueFrom(c: Claim, settings: Settings | undefined, grade: string | undefined): string {
  const grace = grade === undefined ? null : settingOn(settings, `grace-${grade}`, c.date);
  if (grace === null || grace === '') return c.dueBy;
  return new Date(Date.parse(`${c.dueBy}T00:00:00Z`) + Number(grace) * 86_400_000).toISOString().slice(0, 10);
}

/** Whether a claim is overdue: it is open (OPEN or PENDING), not deferred, and its overdue-from day (see
 *  `overdueFrom`; `grade` is the grade its person had on its date) is before the sheet's today. */
export function isOverdue(c: Claim, today: string, settings: Settings | undefined, grade: string | undefined): boolean {
  return isOpen(c) && !isDeferred(c, today) && overdueFrom(c, settings, grade) < today;
}

/** Whether a claim's live figures (its rate step and late charge) round half up: by the sheet's rounding
 *  mode in force on the claim's date (half-up or half-even) when there is one; else when the claim is
 *  dated before the sheet's policy date (blank meaning the sheet's today); else they round half to
 *  even. A correction rounds by the date of the claim it corrects (its person's claim with that
 *  number), not its own. Half to even until the settings and the sheet's today are loaded. */
export function roundsHalfUp(c: Claim, claims: Claim[] | undefined, settings: Settings | undefined, today: string | undefined): boolean {
  const corrected = c.corrects === null
    ? undefined
    : (claims ?? []).find((o) => o.personId === c.personId && o.number === c.corrects);
  const date = corrected?.date ?? c.date;
  const mode = settingOn(settings, 'rounding', date);
  if (mode === 'half-up') return true;
  if (mode === 'half-even') return false;
  const policy = settings === undefined ? undefined : settings.policyDate ?? today;
  if (policy === undefined) return false;
  return date < policy;
}

/** A claim's due now: a settled claim's frozen due; else the due as shown its last statement bound it to (late
 *  charge and floor included: while bound, its charge no longer follows the sheet's today); else the greater of the floor (two decimals; none while
 *  it is not loaded) and its live rate figure plus,
 *  while it is overdue, for every full 30 days it is overdue the sheet's late percent in force on that boundary
 *  date (the overdue-from day — the due-by plus the grade's grace — plus 30, 60, 90 ... days) of that figure plus the charges of the boundaries before it (compounding), each rounded like a due, the charges added up. A
 *  correction (a negative amount) has no floor and no late charge, nor does a claim whose late charge a steward waived. No charge until the settings and the sheet's today are loaded. */
export function dueNow(c: Claim, figure: string, floor: string | undefined, settings: Settings | undefined,
  today: string | undefined, halfUp: boolean, grade: string | undefined): string {
  if (c.due !== null || fixedDue(c) !== null || figure === 'n/a' || isCorrection(c.amount)) return figure;
  const pcts: string[] = [];
  if (!c.waived && settings !== undefined && today !== undefined && isOverdue(c, today, settings, grade)) {
    const dueBy = Date.parse(`${overdueFrom(c, settings, grade)}T00:00:00Z`);
    const periods = Math.floor((Date.parse(`${today}T00:00:00Z`) - dueBy) / 86_400_000 / 30);
    for (let n = 1; n <= periods; n++) {
      const boundary = new Date(dueBy + n * 30 * 86_400_000).toISOString().slice(0, 10);
      pcts.push(settingOn(settings, 'late-pct', boundary) ?? '0.00');
    }
  }
  return atLeast(withCharge(figure, pcts, halfUp), floor);
}

/** A claim's due now (see `dueNow`) from its fixed figure, else its live rate figure at this rate (a
 *  two-decimal percent; undefined when none applies), with the floor in force on its date, every rounding by
 *  the claim's policy (see `roundsHalfUp`) — the other claims on the sheet are what a correction is looked up in.
 *  No floor until the settings are loaded. */
export function claimDueNow(c: Claim, claims: Claim[] | undefined, pct: string | undefined,
  settings: Settings | undefined, today: string | undefined, grade: string | undefined): string {
  const up = roundsHalfUp(c, claims, settings, today);
  const floor = settings === undefined ? undefined : settingOn(settings, 'floor', c.date) ?? undefined;
  return dueNow(c, fixedDue(c) ?? due(c.amount, pct, up), floor, settings, today, up, grade);
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
  const holds = holdsById(people.data, today);
  const figures = new Map<number, { count: number; total: string }>();
  if (settings === undefined || today === undefined) return figures;
  for (const p of people.data ?? []) {
    const overdue = (claims.data ?? []).filter(
      (c) => c.personId === p.id && counts(c, holds, today) && isOverdue(c, today, settings, gradeOn(p, c.date)),
    );
    figures.set(p.id, {
      count: overdue.length,
      total: sumDues(overdue.map((c) =>
        claimDueNow(c, claims.data, rateFor(rates.data, gradeOn(p, c.date), c.date, c.added), settings, today, gradeOn(p, c.date)))),
    });
  }
  return figures;
}
