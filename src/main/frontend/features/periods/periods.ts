import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { countedAt, dueBy, type Claim } from '../claims/claims';
import { gradeOn, type Person } from '../people/people';
import { claimDue, rateOn, sumDue, type Late, type Rate } from '../rates/rates';
import type { DatedValue } from '../settings/settings';

/** A period on the sheet: a name unique on the sheet and an inclusive from and to date (yyyy-mm-dd). No two
 *  periods overlap. A closed period keeps the due-total fixed when it was closed (null while open), and each
 *  person's due-total fixed then, by person id (empty while open). A soft-closed period keeps them fixed the same
 *  way, while its claims can still be added and changed. */
export type Period = { name: string; from: string; to: string; closed: boolean; closedTotal: string | null;
  personTotals: Record<string, string>; soft: boolean };

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

/** A person's due-total for a period: while it is open, the dues (as shown on their rows now) of the person's claims
 *  that counted at the end of its last day (see countedAt) whose due-by falls in it; once closed or soft-closed, the figure fixed for them then (0.00 for a person
 *  added after). */
export function personPeriodDueTotal(period: Period, person: Person, claims: Claim[], people: Person[], rates: Rate[],
    floor: DatedValue, late: Late): string {
  if (period.closed || period.soft) return period.personTotals?.[String(person.id)] ?? '0.00';
  return sumDue(
    countedAt(claims.filter((c) => c.personId === person.id), people, period.to, late.today)
      .filter((c) => periodOf([period], dueBy(c)))
      .map((c) => claimDue(c, rateOn(rates, gradeOn(person, c.date), c.date), floor, late)),
  );
}

/** A period's due-total as shown on its row: the people's due-totals for it (see personPeriodDueTotal) added up. */
export function periodDueTotal(period: Period, claims: Claim[], people: Person[], rates: Rate[], floor: DatedValue,
    late: Late): string {
  return sumDue(people.map((p) => personPeriodDueTotal(period, p, claims, people, rates, floor, late)));
}
