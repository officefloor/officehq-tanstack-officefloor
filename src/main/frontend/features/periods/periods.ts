import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { live, type Claim } from '../claims/claims';
import { gradeOn, type Person } from '../people/people';
import { claimDue, rateOn, sumDue, type Late, type Rate } from '../rates/rates';

/** A period on the sheet: a name unique on the sheet and an inclusive from and to date (yyyy-mm-dd). No two
 *  periods overlap. A closed period keeps the due-total fixed when it was closed (null while open), and each
 *  person's due-total fixed then, by person id (empty while open). */
export type Period = { name: string; from: string; to: string; closed: boolean; closedTotal: string | null;
  personTotals: Record<string, string> };

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

/** A person's due-total for a period: while it is open, the dues (as shown on their rows) of the person's claims
 *  that count (see live) whose date falls in it; once closed, the figure fixed for them then (0.00 for a person
 *  added after). */
export function personPeriodDueTotal(period: Period, person: Person, claims: Claim[], people: Person[], rates: Rate[],
    floor: string, late: Late): string {
  if (period.closed) return period.personTotals?.[String(person.id)] ?? '0.00';
  return sumDue(
    live(claims.filter((c) => c.personId === person.id), people)
      .filter((c) => periodOf([period], c.date))
      .map((c) => claimDue(c, rateOn(rates, gradeOn(person, c.date), c.date), floor, late)),
  );
}

/** A period's due-total as shown on its row: the people's due-totals for it (see personPeriodDueTotal) added up. */
export function periodDueTotal(period: Period, claims: Claim[], people: Person[], rates: Rate[], floor: string,
    late: Late): string {
  return sumDue(people.map((p) => personPeriodDueTotal(period, p, claims, people, rates, floor, late)));
}
