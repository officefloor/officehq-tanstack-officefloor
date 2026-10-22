import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { live, type Claim } from '../claims/claims';
import { gradeOn, type Person } from '../people/people';
import { claimDue, rateOn, sumDue, type Late, type Rate } from '../rates/rates';

/** A period on the sheet: a name unique on the sheet and an inclusive from and to date (yyyy-mm-dd). No two
 *  periods overlap. A closed period keeps the due-total fixed when it was closed (null while open). */
export type Period = { name: string; from: string; to: string; closed: boolean; closedTotal: string | null };

export const periodsKey = (sheetId: string) => ['sheet', sheetId, 'periods'] as const;

/** The sheet's periods, earliest first. */
export function usePeriods(sheetId: string) {
  return useQuery({
    queryKey: periodsKey(sheetId),
    queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`),
  });
}

/** The period whose dates contain the date (yyyy-mm-dd), or undefined when none does. */
export function periodOf(periods: Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}

/** A period's due-total as shown on its row: the one fixed when it was closed, else the dues (as shown on their
 *  rows) of the claims among these that count (see live) whose date falls in it. */
export function periodDueTotal(period: Period, claims: Claim[], people: Person[], rates: Rate[], floor: string,
    late: Late): string {
  if (period.closedTotal !== null) return period.closedTotal;
  const byId = new Map(people.map((p) => [p.id, p]));
  return sumDue(
    live(claims, people)
      .filter((c) => periodOf([period], c.date))
      .map((c) => claimDue(c, rateOn(rates, gradeOn(byId.get(c.personId), c.date), c.date), floor, late)),
  );
}
