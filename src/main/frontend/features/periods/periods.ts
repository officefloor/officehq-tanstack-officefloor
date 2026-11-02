import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { claimDueCents, counted, countedOn, statusOn } from '../claims/claims';
import type { Claim, Late } from '../claims/claims';
import { gradeOn } from '../people/people';
import type { Person } from '../people/people';
import { claimRateOf, twoDecimals } from '../rates/rates';
import type { Rate } from '../rates/rates';
import type { DatedCents } from '../settings/settings';

/** A period on the sheet: its name (unique on the sheet) and its from and to date (yyyy-MM-dd), inclusive;
 *  whether it is closed, its due-total fixed when closed (two decimals; null while open), and each person's due-total
 *  for it fixed then (by person id; empty while open, and a person added since has none); and whether it is
 *  soft-closed instead: its figures fixed the same way, while claims dated in it may still change; and the most OPEN or
 *  PENDING claims a person may have in it (null for no cap); and each person's carried figure for it fixed when it was
 *  (soft-)closed (by person id; empty while open); and each person's opening balance for it, added to their carried
 *  figure (by person id; none for a person with no opening); and whether a steward has locked it, so it cannot be
 *  reopened; and its place in the order the sheet's periods were added (lower first). */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
  closedPeople: Record<string, string>;
  soft: boolean;
  cap: number | null;
  closedCarried: Record<string, string>;
  openings: Record<string, string>;
  locked: boolean;
  added: number;
};

export const periodsKey = (sheet: string) => ['periods', sheet] as const;

export const periodsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/periods`;

/** Where monthly periods are generated between two dates. */
export const generatePeriodsPath = (sheet: string) => `${periodsPath(sheet)}/generate`;

/** Where a fiscal year's twelve monthly periods are generated. */
export const generateFiscalYearPath = (sheet: string) => `${periodsPath(sheet)}/generate-fy`;

/** Where a fiscal year is rolled over into the next. */
export const rolloverPath = (sheet: string) => `${periodsPath(sheet)}/rollover`;

/** Where the last rollover is undone. */
export const rolloverUndoPath = (sheet: string) => `${periodsPath(sheet)}/rollover-undo`;

/** Where a calendar year's four quarterly periods are generated. */
export const generateQuartersPath = (sheet: string) => `${periodsPath(sheet)}/generate-quarters`;

/** The fiscal year a date (yyyy-MM-dd) falls in, named FY<yyyy> by the calendar year its first day falls in, for a
 *  fiscal year starting in the month (1 to 12). */
export function fiscalYearOf(date: string, startMonth: number): string {
  const year = Number(date.slice(0, 4));
  const month = Number(date.slice(5, 7));
  return `FY${month >= startMonth ? year : year - 1}`;
}

/** How many periods a generate made, with every period on the sheet after it. */
export type Generated = { made: number; periods: Period[] };

/** Where opening balances are pasted. */
export const openingsPath = (sheet: string) => `${periodsPath(sheet)}/openings`;

/** Where a period is closed. */
export const closePeriodPath = (sheet: string, name: string) =>
  `${periodsPath(sheet)}/${encodeURIComponent(name)}/close`;

/** Where a period is soft-closed. */
export const softClosePeriodPath = (sheet: string, name: string) =>
  `${periodsPath(sheet)}/${encodeURIComponent(name)}/soft-close`;

/** Where a closed or soft-closed period is reopened (a steward only). */
export const reopenPeriodPath = (sheet: string, name: string) =>
  `${periodsPath(sheet)}/${encodeURIComponent(name)}/reopen`;

/** Where a period is locked (a steward only). */
export const lockPeriodPath = (sheet: string, name: string) =>
  `${periodsPath(sheet)}/${encodeURIComponent(name)}/lock`;

/** Where a locked period is unlocked (a steward only). */
export const unlockPeriodPath = (sheet: string, name: string) =>
  `${periodsPath(sheet)}/${encodeURIComponent(name)}/unlock`;

/** Every period on the sheet, by from-date; no two overlap. */
export function usePeriods(sheet: string) {
  return useQuery({
    queryKey: periodsKey(sheet),
    queryFn: () => getJson<Period[]>(periodsPath(sheet)),
  });
}

/** The period whose dates contain the date (yyyy-MM-dd), or undefined when none does. */
export function periodOf(periods: Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}

/** Each period's live due-total (in hundredths, by name): the due now of the claims whose due-by falls in it that counted
 *  at the end of its last day (see {@link countedOn}: OPEN, PENDING or SETTLED then by their status log, but not a
 *  deferred one, nor an unbound open one within its person's hold); a claim with no rate adds nothing. */
export function periodDueCents(
  periods: Period[],
  claims: Claim[],
  people: Person[],
  rates: Rate[],
  floor: DatedCents,
  late: Late,
): Map<string, bigint> {
  const totals = new Map<string, bigint>();
  for (const [name, byPerson] of personPeriodDueCents(periods, claims, people, rates, floor, late)) {
    totals.set(name, [...byPerson.values()].reduce((sum, due) => sum + due, 0n));
  }
  return totals;
}

/** Each period's live due-total per person (in hundredths, by period name then person id): the part of the period's
 *  due-total from the person's claims (see {@link periodDueCents}), so the people's add up to the period's. */
export function personPeriodDueCents(
  periods: Period[],
  claims: Claim[],
  people: Person[],
  rates: Rate[],
  floor: DatedCents,
  late: Late,
): Map<string, Map<number, bigint>> {
  const byId = new Map(people.map((p) => [p.id, p]));
  const totals = new Map<string, Map<number, bigint>>();
  for (const c of claims) {
    const period = periodOf(periods, c.dueBy);
    const person = byId.get(c.person);
    if (period === undefined || person === undefined || !countedOn(c, person, period.to)) continue;
    const due = claimDueCents(c, claimRateOf(rates, gradeOn(person, c.date), c), floor, late) ?? 0n;
    const byPerson = totals.get(period.name) ?? new Map<number, bigint>();
    byPerson.set(person.id, (byPerson.get(person.id) ?? 0n) + due);
    totals.set(period.name, byPerson);
  }
  return totals;
}

/** Each period's carried figure per person (in hundredths, by period name then person id): the due now of the person's
 *  claims that count at the end of the period (see {@link countedOn}) whose due-by falls in an earlier period (one
 *  ending before it starts) and that were still OPEN or PENDING at its end by their status log, plus their opening
 *  balance for it; a claim with no rate adds nothing. */
export function personPeriodCarriedCents(
  periods: Period[],
  claims: Claim[],
  people: Person[],
  rates: Rate[],
  floor: DatedCents,
  late: Late,
): Map<string, Map<number, bigint>> {
  const byId = new Map(people.map((p) => [p.id, p]));
  const totals = new Map<string, Map<number, bigint>>();
  for (const period of periods) {
    const byPerson = new Map<number, bigint>();
    for (const [id, amount] of Object.entries(period.openings)) {
      byPerson.set(Number(id), BigInt(amount.replace('.', '')));
    }
    totals.set(period.name, byPerson);
  }
  for (const c of claims) {
    const home = periodOf(periods, c.dueBy);
    const person = byId.get(c.person);
    if (home === undefined || person === undefined) continue;
    for (const period of periods) {
      if (home.to >= period.from) continue;
      const then = statusOn(c, period.to);
      if ((then !== 'OPEN' && then !== 'PENDING') || !countedOn(c, person, period.to)) continue;
      const due = claimDueCents(c, claimRateOf(rates, gradeOn(person, c.date), c), floor, late) ?? 0n;
      const byPerson = totals.get(period.name) ?? new Map<number, bigint>();
      byPerson.set(person.id, (byPerson.get(person.id) ?? 0n) + due);
      totals.set(period.name, byPerson);
    }
  }
  return totals;
}

/** Each period's classic due-total (in hundredths, by name): the due now of the claims whose due-by falls in it that
 *  count by their status now (see {@link counted}: OPEN, PENDING or SETTLED), not by their status log; a claim with no
 *  rate adds nothing. */
export function classicPeriodDueCents(
  periods: Period[],
  claims: Claim[],
  people: Person[],
  rates: Rate[],
  floor: DatedCents,
  late: Late,
): Map<string, bigint> {
  const byId = new Map(people.map((p) => [p.id, p]));
  const totals = new Map<string, bigint>();
  for (const c of counted(claims, people)) {
    const period = periodOf(periods, c.dueBy);
    const person = byId.get(c.person);
    if (period === undefined || person === undefined) continue;
    const due = claimDueCents(c, claimRateOf(rates, gradeOn(person, c.date), c), floor, late) ?? 0n;
    totals.set(period.name, (totals.get(period.name) ?? 0n) + due);
  }
  return totals;
}

/** Whether a period's figures are fixed: closed or soft-closed. */
export function isFixed(period: Period): boolean {
  return period.closed || period.soft;
}

/** A person's due-total for a period as shown: the one fixed when it was (soft-)closed (0.00 for a person added since), else
 *  their live one. */
export function shownPersonDueTotal(period: Period, personId: number, liveCents: bigint): string {
  return isFixed(period) ? (period.closedPeople[String(personId)] ?? '0.00') : twoDecimals(liveCents);
}

/** A person's carried figure for a period as shown: the one fixed when it was (soft-)closed (0.00 for a person added
 *  since), else their live one. */
export function shownPersonCarried(period: Period, personId: number, liveCents: bigint): string {
  return isFixed(period) ? (period.closedCarried[String(personId)] ?? '0.00') : twoDecimals(liveCents);
}

/** A period's due-total as shown: the one fixed when it was (soft-)closed, else its live one. */
export function shownDueTotal(period: Period, liveCents: bigint): string {
  return isFixed(period) && period.closedTotal !== null ? period.closedTotal : twoDecimals(liveCents);
}
