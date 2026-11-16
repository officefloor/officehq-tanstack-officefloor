import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { live, type Claim } from '../claims/claims';
import type { Late } from '../overdue/late';
import { gradeOn, type Person } from '../people/people';
import { claimDue, rateFor, sum, type Rate } from '../rates/rates';
import type { Dated } from '../settings/settings';

/** A period of the sheet: `name` is unique on the sheet (case ignored); `from` and `to` (yyyy-MM-dd) are both
 *  inclusive. No two periods of a sheet overlap. A closed period keeps `closedTotal`, the due-total fixed when it
 *  was closed, and `personTotals`, each person's; no claim dated inside it can be added, edited, opened, settled or voided. */
export type Period = {
  id: number;
  sheetId: string;
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
  /** Closed, each person's due-total for the period fixed when it was closed, by person id. */
  personTotals: Record<string, string>;
};

export const periodsKey = (sheetId: string) => ['sheet', sheetId, 'periods'];

/** The sheet's periods, by from-date. */
export function usePeriods(sheetId: string) {
  return useQuery({
    queryKey: periodsKey(sheetId),
    queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`),
  });
}

/** The period whose dates contain a date (yyyy-MM-dd), or undefined when none does. */
export function periodOf(periods: Period[] | undefined, date: string): Period | undefined {
  return periods?.find((p) => p.from <= date && date <= p.to);
}

/** A person's due-total for a period: the figures of their claims that count, each with its late charge (a
 *  person's OPEN claims dated within a hold of theirs, not bound by a statement, do not), whose date falls in it,
 *  added up (skipping n/a); for a closed period, their figure fixed when it was closed (0.00 for a person added
 *  after). */
export function personPeriodDue(
  period: Period,
  person: Person,
  claims: Claim[] | undefined,
  rates: Rate[] | undefined,
  floor: Dated,
  late: Late,
): string {
  if (period.closed) return period.personTotals?.[String(person.id)] ?? '0.00';
  const mine = live((claims ?? []).filter((c) => c.personId === person.id), [person]).filter(
    (c) => period.from <= c.date && c.date <= period.to,
  );
  return sum(mine.map((c) => claimDue(c, rateFor(rates, gradeOn(person, c.date), c.date), floor, late)));
}

/** A period's due-total: the people's due-totals for it (see personPeriodDue) added up. */
export function periodDue(
  period: Period,
  claims: Claim[] | undefined,
  people: Person[] | undefined,
  rates: Rate[] | undefined,
  floor: Dated,
  late: Late,
): string {
  return sum((people ?? []).map((person) => personPeriodDue(period, person, claims, rates, floor, late)));
}
