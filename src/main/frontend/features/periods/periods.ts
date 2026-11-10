import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { gradeOn } from '../people/people';
import type { Person } from '../people/people';
import { claimDue, rateOn, sumDue } from '../rates/rates';
import type { Dated, Late, Rate } from '../rates/rates';

/**
 * A period: its name (unique on the sheet), its from and to dates (yyyy-MM-dd), inclusive, whether
 * it is closed, the due-total fixed when it was closed ('' while open), and each person's due-total
 * fixed when it was closed, by person id (empty while open).
 */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string;
  personTotals: Record<string, string>;
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
 * after), else the due of their claims that count whose date falls in it, added up.
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
      .filter((c) => c.personId === person.id && counts(c, person.holds) && period.from <= c.date && c.date <= period.to)
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
