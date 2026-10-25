import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { gradeOn, type Person } from '../people/people';
import { countsAsOf, dueByOf, holdsOf, statusOn, type Claim } from '../claims/claims';
import { chargedOf, sumDue, type Rate } from '../rates/rates';
import type { Settings } from '../roles/roles';

/** A sheet's period as the store holds it: its name (unique on the sheet) and its from and to date
 *  (YYYY-MM-DD), inclusive. No two periods overlap. A closed period keeps the due-total fixed when it was closed
 *  (`closedTotal`, null while open), and each person's fixed then (`personTotals`, by person id; empty while
 *  open), and each person's carried figure fixed then (`personCarried`, by person id; empty while open), until a steward reopens it. A soft-closed period (`softClosed`) keeps them fixed the same way, while claims
 *  whose due-by falls in it can still be added, edited and change status; it can be closed (fixing them afresh) or reopened.
 *  A period's `cap` (null for none) is the most OPEN or PENDING claims a person may have whose due-by falls in it: opening one more is refused. */
export type Period = {
  id: number; name: string; from: string; to: string; closed: boolean; softClosed: boolean; closedTotal: string | null;
  personTotals: Record<string, string>; cap: number | null; personCarried: Record<string, string>;
  /** Each person's opening balance for the period (by person id; absent for none), added to their carried figure. */
  personOpening: Record<string, string>;
  /** Whether a steward has locked the period: a locked period cannot be reopened until a steward unlocks it. */
  locked: boolean;
};

export const periodsQueryKey = (sheetId: string) => ['sheet', sheetId, 'periods'] as const;

export const periodsPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/periods`;

/** Generates a period per calendar month from a from-date's month to a to-date's. */
export const periodsGeneratePath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/generate-periods`;

/** Generates a fiscal year's twelve monthly periods, as the period generator does. */
export const periodsGenerateFyPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/generate-fy`;

/** Generates a calendar year's four quarters (yyyy-Q1 to yyyy-Q4), as the period form would add each. */
export const periodsGenerateQuartersPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/generate-quarters`;

/** Rolls a fiscal year over: generates the next one's periods and carries each person's open figure into its first. */
export const rolloverPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/rollover`;

/** Pastes opening balances: lines of name,period,amount. */
export const openingsImportPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/import-openings`;

export const periodClosePath = (sheetId: string, periodId: number) => `${periodsPath(sheetId)}/${periodId}/close`;

export const periodSoftClosePath = (sheetId: string, periodId: number) => `${periodsPath(sheetId)}/${periodId}/soft-close`;

export const periodReopenPath = (sheetId: string, periodId: number) => `${periodsPath(sheetId)}/${periodId}/reopen`;

export const periodLockPath = (sheetId: string, periodId: number) => `${periodsPath(sheetId)}/${periodId}/lock`;

export const periodUnlockPath = (sheetId: string, periodId: number) => `${periodsPath(sheetId)}/${periodId}/unlock`;

/** The sheet's periods, by from-date. */
export function usePeriods() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: periodsQueryKey(sheet ?? ''),
    queryFn: () => getJson<Period[]>(periodsPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** The period whose dates contain a date (YYYY-MM-DD), or undefined for none. */
export function periodOn(periods: Period[] | undefined, date: string): Period | undefined {
  return (periods ?? []).find((p) => p.from <= date && date <= p.to);
}

/** A period's due-total: the dues (with any late charge, as shown now) of the claims whose due-by falls in it that counted
 *  by the status they had at the end of its last day (not a person's unbound OPEN ones dated within a hold of theirs), added up, skipping n/a; for a closed period, the one fixed when it was closed. */
export function periodDue(period: Period, people: Person[] | undefined, claims: Claim[], rates: Rate[] | undefined,
  settings: Settings | undefined, today: string | undefined): string {
  if (period.closedTotal !== null) return period.closedTotal;
  const personOf = new Map((people ?? []).map((p) => [p.id, p]));
  const held = holdsOf(people);
  return sumDue(claims
    .filter((c) => countsAsOf(c, held, today, period.to) && personOf.has(c.personId) && periodOn([period], dueByOf(c)))
    .map((c) => chargedOf(c, rates, gradeOn(personOf.get(c.personId)!, c.date), settings, today)));
}

/** A person's due-total for a period: the dues (with any late charge, as shown now) of their claims whose due-by falls in
 *  it that counted by the status they had at the end of its last day
 *  (not their unbound OPEN ones dated within a hold of theirs), added up, skipping n/a; for a closed period, the one
 *  fixed when it was closed (or soft-closed) — 0.00 for a person added since. */
export function personPeriodDue(period: Period, person: Person, people: Person[] | undefined, claims: Claim[],
  rates: Rate[] | undefined, settings: Settings | undefined, today: string | undefined): string {
  if (period.closed || period.softClosed) return period.personTotals?.[String(person.id)] ?? '0.00';
  const held = holdsOf(people);
  return sumDue(claims
    .filter((c) => c.personId === person.id && countsAsOf(c, held, today, period.to) && periodOn([period], dueByOf(c)))
    .map((c) => chargedOf(c, rates, gradeOn(person, c.date), settings, today)));
}

/** A person's carried figure for a period: the dues (with any late charge, as shown now) of their claims that count whose
 *  due-by falls in a period ending before this one starts and that were still OPEN or PENDING at the end of this one's
 *  last day (by the status log, as `personPeriodDue` counts), added up, skipping n/a, plus their opening balance for it; 0.00 for nothing carried. For a closed
 *  (or soft-closed) period, the one fixed when it was closed — 0.00 for a person added since. */
export function personPeriodCarried(period: Period, periods: Period[], person: Person, people: Person[] | undefined,
  claims: Claim[], rates: Rate[] | undefined, settings: Settings | undefined, today: string | undefined): string {
  if (period.closed || period.softClosed) return period.personCarried?.[String(person.id)] ?? '0.00';
  const held = holdsOf(people);
  const earlier = periods.filter((q) => q.to < period.from);
  const opening = period.personOpening?.[String(person.id)];
  return sumDue([...claims
    .filter((c) => {
      if (c.personId !== person.id || !periodOn(earlier, dueByOf(c))) return false;
      const status = statusOn(c, period.to);
      return (status === 'OPEN' || status === 'PENDING') && countsAsOf(c, held, today, period.to);
    })
    .map((c) => chargedOf(c, rates, gradeOn(person, c.date), settings, today)), ...(opening ? [opening] : [])]);
}

/** The fiscal year a date (YYYY-MM-DD) falls in, FY<yyyy>: a fiscal year starts on the first of the sheet's fiscal
 *  year start month (1 unless set) and is named by the calendar year that day falls in. */
export function fiscalYearOf(date: string, settings: Settings | undefined): string {
  const start = Number(settings?.fiscalStartMonth || '1');
  const year = Number(date.slice(0, 4));
  const month = Number(date.slice(5, 7));
  return `FY${month >= start ? year : year - 1}`;
}
