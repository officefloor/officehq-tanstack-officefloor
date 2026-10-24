import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { countedAt, dueBy, live, statusAt, type Claim } from '../claims/claims';
import { gradeOn, type Person } from '../people/people';
import { claimDue, rateOn, sumDue, type Late, type Rate } from '../rates/rates';
import type { DatedValue } from '../settings/settings';

/** A period on the sheet: a name unique on the sheet and an inclusive from and to date (yyyy-mm-dd). No two
 *  periods overlap. A closed period keeps the due-total fixed when it was closed (null while open), and each
 *  person's due-total and carried figure fixed then, by person id (empty while open). A soft-closed period keeps them fixed the same
 *  way, while its claims can still be added and changed. A period may cap the OPEN or PENDING claims each person has in
 *  it (null: no cap); opening one past the cap is refused. A steward may lock a period: a locked one cannot be reopened. Each person's opening balance pasted for the period, by person
 *  id (none: 0.00), adds to their carried figure. Its position in the order the periods were added (0 first) is in added. */
export type Period = { name: string; from: string; to: string; closed: boolean; closedTotal: string | null;
  personTotals: Record<string, string>; soft: boolean; cap: number | null; personCarried: Record<string, string>;
  personOpenings?: Record<string, string>; locked?: boolean; added?: number | null };

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
      .map((c) => claimDue(c, rateOn(rates, gradeOn(person, c.date), c.date, c.id), floor, late)),
  );
}

/** A person's carried figure for a period: the dues (as shown on their rows now) of the person's claims that count
 *  (see countedAt) whose due-by falls in a period ending before this one starts, and whose status at the end of this
 *  period's last day (see statusAt) is OPEN or PENDING, added up, with their opening balance for it; once closed or soft-closed, the figure fixed for
 *  them then (0.00 for a person added after). */
export function personPeriodCarried(period: Period, periods: Period[], person: Person, claims: Claim[],
    people: Person[], rates: Rate[], floor: DatedValue, late: Late): string {
  if ((period.closed || period.soft) && period.personCarried) {
    const fixed = period.personCarried[String(person.id)];
    if (fixed !== undefined || period.personTotals?.[String(person.id)] === undefined) return fixed ?? '0.00';
  }
  const earlier = periods.filter((p) => p.to < period.from);
  return sumDue([
    ...countedAt(claims.filter((c) => c.personId === person.id), people, period.to, late.today)
      .filter((c) => { const at = statusAt(c, period.to); return at === 'OPEN' || at === 'PENDING'; })
      .filter((c) => periodOf(earlier, dueBy(c)))
      .map((c) => claimDue(c, rateOn(rates, gradeOn(person, c.date), c.date, c.id), floor, late)),
    period.personOpenings?.[String(person.id)] ?? '0.00',
  ]);
}

/** A period's due-total as shown on its row: the people's due-totals for it (see personPeriodDueTotal) added up. */
export function periodDueTotal(period: Period, claims: Claim[], people: Person[], rates: Rate[], floor: DatedValue,
    late: Late): string {
  return sumDue(people.map((p) => personPeriodDueTotal(period, p, claims, people, rates, floor, late)));
}

/** A period's claim count as shown on its row: the claims whose due-by falls in it, VOID and DELETED ones left out. */
export function periodClaimsCount(period: Period, claims: Claim[]): number {
  return claims.filter((c) => c.status !== 'VOID' && c.status !== 'DELETED' && periodOf([period], dueBy(c))).length;
}

/** A period's classic due-total: while it is open, the dues (as shown on their rows now) of every person's claims that
 *  count by their status now (see live) whose due-by falls in it, added up; once closed or soft-closed, the figure
 *  fixed then (see personPeriodDueTotal). */
export function classicPeriodDueTotal(period: Period, claims: Claim[], people: Person[], rates: Rate[],
    floor: DatedValue, late: Late): string {
  if (period.closed || period.soft) return periodDueTotal(period, claims, people, rates, floor, late);
  return sumDue(people.flatMap((p) => live(claims.filter((c) => c.personId === p.id), [p], late.today)
    .filter((c) => periodOf([period], dueBy(c)))
    .map((c) => claimDue(c, rateOn(rates, gradeOn(p, c.date), c.date, c.id), floor, late))));
}
