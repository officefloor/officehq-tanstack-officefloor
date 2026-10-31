import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { claimDueCents, live, type Claim, type Late } from '../claims/useClaims';
import { gradeOn, heldOnBy, type Person } from '../people/usePeople';
import { hundredths, rateOn, totalDue, type Rate } from '../rates/useRates';
import type { FloorOn } from '../settings/useSettings';

/** A period as the server sends it: its name, its from and to dates (yyyy-mm-dd), inclusive, whether
 *  it is closed and, when it is, the due-total fixed when it was closed and each person's (by person
 *  id; a person not in it, added since, has 0.00). */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
  closedPersonTotals: Record<string, string>;
};

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

/** A person's due-total for a period, in cents: the one fixed when the period was closed (0 for a
 *  person added since), else the figures of their counted (OPEN or SETTLED) claims dated in it, not
 *  OPEN ones dated within their hold unless stated, n/a ones skipped. */
export function personPeriodDueCents(period: Period, person: Person, claims: Claim[], rates: Rate[], floor: FloorOn, late: Late): bigint {
  if (period.closed) {
    const fixed = period.closedPersonTotals?.[String(person.id)];
    return fixed === undefined ? 0n : hundredths(fixed);
  }
  return live(claims, heldOnBy([person]))
    .filter((c) => c.personId === person.id && periodOf([period], c.date) !== undefined)
    .reduce<bigint>((sum, c) => sum + (claimDueCents(c, floor, rateOn(rates, gradeOn(person, c.date), c.date), late) ?? 0n), 0n);
}

/** A period's due-total: everyone's due-total for it added up. */
export function periodDue(period: Period, claims: Claim[], people: Person[], rates: Rate[], floor: FloorOn, late: Late): string {
  return totalDue(people.map((p) => personPeriodDueCents(period, p, claims, rates, floor, late)));
}
