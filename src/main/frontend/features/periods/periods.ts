import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { gradeOn } from '../people/people';
import type { Person } from '../people/people';
import { claimDue, rateOn, sumDue } from '../rates/rates';
import type { Late, Rate } from '../rates/rates';

/**
 * A period: its name (unique on the sheet), its from and to dates (yyyy-MM-dd), inclusive, whether
 * it is closed, and the due-total fixed when it was closed ('' while open).
 */
export type Period = { name: string; from: string; to: string; closed: boolean; closedTotal: string };

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
 * A period's due-total: the one fixed when it was closed, else the due of the claims that count
 * whose date falls in it, added up.
 */
export function periodDueTotal(
  period: Period,
  people: Person[],
  claims: Claim[],
  rates: Rate[],
  floor: string,
  late: Late,
): string {
  if (period.closed) return period.closedTotal;
  const dues = people.flatMap((person) =>
    claims
      .filter((c) => c.personId === person.id && counts(c, person.holds) && period.from <= c.date && c.date <= period.to)
      .map((c) => claimDue(c, rateOn(rates, gradeOn(person.grades, c.date), c.date), floor, late)),
  );
  return sumDue(dues);
}
