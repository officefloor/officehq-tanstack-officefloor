import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { bound, deferred, isOpen } from '../claims/claims';
import type { Claim, ClaimStatus } from '../claims/claims';
import { covers, liveHolds } from '../people/people';
import type { Person } from '../people/people';
import { claimDue, total } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { sheetKey, useCurrentSheet } from '../session/session';
import type { DueRules } from '../settings/settings';

/**
 * A period as the store holds it: its name (unique on the sheet), its from and to dates (yyyy-MM-dd),
 * both inclusive, whether it is closed and, when closed, the due-total fixed then (null while open) and each
 * person's due-total fixed then, by person id (empty while open), with each person's carried figure fixed then
 * (personCarried; empty while open). A soft-closed period (soft; never also closed)
 * keeps its figures fixed the same way, while claims dated in it can still be added and changed. cap is the most OPEN
 * or PENDING claims due by a date in it a person may have when another of theirs is opened (null: none).
 * personOpenings: the opening balances pasted into it, added up by person id. added: where it stands in the order the
 * periods were added (higher is later).
 */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
  personTotals: Record<string, string>;
  soft: boolean;
  cap: number | null;
  personCarried: Record<string, string>;
  personOpenings: Record<string, string>;
  locked: boolean;
  added: number | null;
};

/** The fields the period form submits; cap is a whole number of at least 1, or blank for none. */
export type PeriodInput = { name: string; from: string; to: string; cap: string };

/** The query key for the sheet's periods; under sheetKey, so re-loading the sheet re-reads them. */
export const periodsKey = (sheet: string) => [...sheetKey(sheet), 'periods'] as const;

/** The sheet's periods, earliest first. */
export function usePeriods() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: periodsKey(sheet),
    queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheet)}/periods`),
    enabled: !!sheet,
  });
}

/**
 * The fiscal year the date (yyyy-MM-dd) falls in, FY<yyyy>: a fiscal year starts on the first of the start month (1 to 12)
 * and is named by the calendar year that first day falls in.
 */
export const fiscalYearOf = (date: string, startMonth: number) => {
  const year = Number(date.slice(0, 4));
  return `FY${Number(date.slice(5, 7)) < startMonth ? year - 1 : year}`;
};

/** Whether the date (yyyy-MM-dd) falls in the period, both ends included. */
export const contains = (p: Period, date: string) => p.from <= date && date <= p.to;

/** The period whose dates contain the date; undefined with none. Periods never overlap, so there is at most one. */
export const periodOf = (periods: Period[], date: string) => periods.find((p) => contains(p, date));

/**
 * The status a claim had at the end of the day (yyyy-MM-dd), as its status log records it: the last line logged on
 * or before it; undefined when it was added after. A claim with no log at all is taken at its status now.
 */
export function statusAt(c: Claim, date: string): ClaimStatus | undefined {
  if (c.log.length === 0) return c.status;
  let status: ClaimStatus | undefined;
  for (const line of c.log) if (line.date <= date) status = line.status;
  return status;
}

/**
 * Whether a claim counts toward a period: by the status it had at the end of the period's last day (OPEN, PENDING
 * or SETTLED then), left out as the live rules say while it is deferred, or when it was open then, is open still, one
 * of its person's `holds` covers its date (their retirement; a hold does not) and no statement binds it.
 */
export const countsInPeriod = (period: Period, c: Claim, holds: Person['holds'], today: string) => {
  const then = statusAt(c, period.to);
  if (then === 'SETTLED') return !deferred(c, today);
  if (then !== 'OPEN' && then !== 'PENDING') return false;
  return !deferred(c, today) && (!isOpen(c) || bound(c) || !holds.some((h) => covers(h, c.date)));
};

/**
 * A person's due-total for a period: the dues (late charges included, as shown now) of their claims due by a date in it
 * that count by their status at the period's end, added up; a closed period keeps the figure fixed when it was
 * closed or soft-closed (0.00 for a person added since).
 */
export function personPeriodDue(period: Period, person: Person, claims: Claim[], rates: Rate[], rules: DueRules): string {
  if (period.closed || period.soft) return period.personTotals?.[String(person.id)] ?? '0.00';
  const figures = claims
    .filter((c) => c.personId === person.id && contains(period, c.dueBy) && countsInPeriod(period, c, liveHolds(person, rules.today), rules.today))
    .map((c) => claimDue(c, rates, person.grades, rules));
  return total(figures);
}

/** A period's due-total: the people's due-totals for it, added up. */
export function periodDue(period: Period, claims: Claim[], people: Person[], rates: Rate[], rules: DueRules): string {
  return total(people.map((p) => personPeriodDue(period, p, claims, rates, rules)));
}

/**
 * A person's carried figure for a period: the dues (as shown now) of their claims belonging to a period that ends
 * before this one starts and still OPEN or PENDING at this period's end (by the status log), counted as the period
 * figures count them, added up with the opening balances pasted for them into it; 0.00 with none. A closed or soft-closed period keeps the figure fixed when it was
 * closed (0.00 for a person added since).
 */
export function personPeriodCarried(period: Period, periods: Period[], person: Person, claims: Claim[], rates: Rate[], rules: DueRules): string {
  if (period.closed || period.soft) return period.personCarried?.[String(person.id)] ?? '0.00';
  const figures = claims
    .filter((c) => {
      if (c.personId !== person.id) return false;
      const own = periodOf(periods, c.dueBy);
      if (!own || own.to >= period.from) return false;
      const then = statusAt(c, period.to);
      return (then === 'OPEN' || then === 'PENDING') && countsInPeriod(period, c, liveHolds(person, rules.today), rules.today);
    })
    .map((c) => claimDue(c, rates, person.grades, rules));
  return total([...figures, period.personOpenings?.[String(person.id)] ?? '0.00']);
}
