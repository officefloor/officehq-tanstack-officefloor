import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { claimDueCents, live, type Claim, type Late } from '../claims/useClaims';
import { gradeOn, heldOnBy, type Person } from '../people/usePeople';
import { rateOn, totalDue, type Rate } from '../rates/useRates';

/** A period as the server sends it: its name, its from and to dates (yyyy-mm-dd), inclusive, whether
 *  it is closed and, when it is, the due-total fixed when it was closed. */
export type Period = { name: string; from: string; to: string; closed: boolean; closedTotal: string | null };

export const periodsKey = (sheet: string) => ['sheet', sheet, 'periods'] as const;

/** The sheet's periods, by from-date, re-read whenever the sheet is loaded. */
export function usePeriods(sheet: string) {
  return useQuery({
    queryKey: periodsKey(sheet),
    queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheet)}/periods`),
    staleTime: 0,
    gcTime: 0,
  });
}

/** A date the server accepts: yyyy-mm-dd. */
export const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** The period whose dates contain `date` (yyyy-mm-dd), or undefined when none does. Periods never
 *  overlap, so there is at most one. */
export function periodOf(periods: Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}

/** A period's due-total: the one fixed when it was closed, else the figures of everyone's counted
 *  (OPEN or SETTLED) claims dated in it, not OPEN ones dated within their person's hold unless stated, n/a ones skipped. */
export function periodDue(period: Period, claims: Claim[], people: Person[], rates: Rate[], floor: string, late: Late): string {
  if (period.closed && period.closedTotal !== null) return period.closedTotal;
  const personOf = new Map(people.map((p) => [p.id, p]));
  return totalDue(
    live(claims, heldOnBy(people))
      .filter((c) => periodOf([period], c.date) !== undefined && personOf.has(c.personId))
      .map((c) => claimDueCents(c, floor, rateOn(rates, gradeOn(personOf.get(c.personId)!, c.date), c.date), late)),
  );
}
