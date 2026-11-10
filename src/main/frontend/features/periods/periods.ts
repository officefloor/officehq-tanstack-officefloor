import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { countedOn } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { gradeOn, liveHolds } from '../people/people';
import type { Person } from '../people/people';
import { claimDue, rateOn, sumDue } from '../rates/rates';
import type { Dated, Late, Rate } from '../rates/rates';

/**
 * A period: its name (unique on the sheet), its from and to dates (yyyy-MM-dd), inclusive, whether
 * it is closed, the due-total fixed when it was closed ('' while open), and each person's due-total
 * fixed when it was closed, by person id (empty while open), and whether it is soft-closed: closed
 * with its figures fixed, but its claims can still be changed.
 */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string;
  personTotals: Record<string, string>;
  soft: boolean;
};

export const periodsKey = (sheetId: string) => ['sheet', sheetId, 'periods'] as const;

/** The sheet's periods, by from-date. */
export function usePeriods(sheetId: string) {
  return useQuery({
    queryKey: periodsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ periods: Period[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`)).periods,
  });
}

/** The period whose dates contain the date (yyyy-MM-dd), or null when none does. */
export function periodOf(periods: Period[], date: string): Period | null {
  return periods.find((p) => p.from <= date && date <= p.to) ?? null;
}

/**
 * A person's due-total for a period: the one fixed when it was closed (0.00 for a person added
 * after), else the due as shown now of their claims due by a date in it (a claim's period is the one
 * containing its due-by) that counted at the end of its last
 * day, by the status their log records then, added up.
 */
export function personPeriodDueTotal(
  period: Period,
  person: Person,
  claims: Claim[],
  rates: Rate[],
  floor: Dated,
  late: Late,
): string {
  if (period.closed) return period.personTotals[person.id] ?? '0.00';
  return sumDue(
    claims
      .filter((c) => c.personId === person.id && countedOn(c, period.to, liveHolds(person, late.today), late.today) && period.from <= c.dueBy && c.dueBy <= period.to)
      .map((c) => claimDue(c, rateOn(rates, gradeOn(person.grades, c.date), c.date), floor, late)),
  );
}

/** A period's due-total: the people's due-totals for it, added up. */
export function periodDueTotal(
  period: Period,
  people: Person[],
  claims: Claim[],
  rates: Rate[],
  floor: Dated,
  late: Late,
): string {
  return sumDue(people.map((person) => personPeriodDueTotal(period, person, claims, rates, floor, late)));
}
