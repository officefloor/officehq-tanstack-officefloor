import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts, countedOn, statusOn } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { gradeOn, liveHolds } from '../people/people';
import type { Person } from '../people/people';
import { claimDue, rateOn, sumDue } from '../rates/rates';
import type { Dated, Late, Rate } from '../rates/rates';

/**
 * A period: its name (unique on the sheet), its from and to dates (yyyy-MM-dd), inclusive, whether
 * it is closed, the due-total fixed when it was closed ('' while open), and each person's due-total
 * fixed when it was closed, by person id (empty while open), and whether it is soft-closed: closed
 * with its figures fixed, but its claims can still be changed; and the fiscal year its from-date falls in.
 */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string;
  personTotals: Record<string, string>;
  soft: boolean;
  /** The fiscal year its from-date falls in, FY<yyyy>. */
  fy: string;
  /** The most OPEN or PENDING claims a person may have in it ('' for no cap). */
  cap: string;
  /** Each person's carried figure fixed when it was closed, by person id (empty while open). */
  personCarried: Record<string, string>;
  /** Each person's opening balance for it, by person id (none for a person without one). */
  personOpenings: Record<string, string>;
  /** Whether it is locked: a locked period cannot be reopened. */
  locked: boolean;
  /** The order it was added in: a larger one added later. */
  added: number;
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
      .map((c) => claimDue(c, rateOn(rates, gradeOn(person.grades, c.date), c.date, c.id), floor, late, gradeOn(person.grades, c.date))),
  );
}

/**
 * A person's carried figure for a period: the one fixed when it was closed (0.00 for a person added
 * after), else the due as shown now of their claims that count and belong
 * to a period (by due-by) ending before this one starts, and were still OPEN or PENDING at the end of
 * this period's last day by the status their log records then, added up, with their opening balance
 * for it.
 */
export function personPeriodCarried(
  period: Period,
  periods: Period[],
  person: Person,
  claims: Claim[],
  rates: Rate[],
  floor: Dated,
  late: Late,
): string {
  if (period.closed) return period.personCarried?.[person.id] ?? '0.00';
  const holds = liveHolds(person, late.today);
  return sumDue([
    period.personOpenings?.[person.id] ?? '0.00',
    ...claims
      .filter((c) => {
        if (c.personId !== person.id) return false;
        const own = periodOf(periods, c.dueBy);
        if (own === null || own.to >= period.from) return false;
        const status = statusOn(c, period.to);
        return (status === 'OPEN' || status === 'PENDING') && countedOn(c, period.to, holds, late.today);
      })
      .map((c) => claimDue(c, rateOn(rates, gradeOn(person.grades, c.date), c.date, c.id), floor, late, gradeOn(person.grades, c.date))),
  ]);
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

/**
 * A period's classic due-total: the due-total fixed when it was closed, else the due as shown now
 * of the claims due by a date in it that count by their status now (OPEN, PENDING and SETTLED),
 * added up over everyone.
 */
export function classicPeriodDueTotal(
  period: Period,
  people: Person[],
  claims: Claim[],
  rates: Rate[],
  floor: Dated,
  late: Late,
): string {
  if (period.closed) return period.closedTotal !== '' ? period.closedTotal : periodDueTotal(period, people, claims, rates, floor, late);
  return sumDue(
    people.flatMap((person) =>
      claims
        .filter((c) => c.personId === person.id && counts(c, liveHolds(person, late.today), late.today) && period.from <= c.dueBy && c.dueBy <= period.to)
        .map((c) => claimDue(c, rateOn(rates, gradeOn(person.grades, c.date), c.date, c.id), floor, late, gradeOn(person.grades, c.date))),
    ),
  );
}

/**
 * A period's claim count: the claims belonging to it (a claim's period is the one containing its
 * due-by), VOID and DELETED claims left out.
 */
export function periodClaimCount(period: Period, claims: Claim[]): number {
  return claims.filter(
    (c) => c.status !== 'VOID' && c.status !== 'DELETED' && period.from <= c.dueBy && c.dueBy <= period.to,
  ).length;
}
