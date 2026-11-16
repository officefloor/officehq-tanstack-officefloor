import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { live, type Claim } from '../claims/claims';
import type { Late } from '../overdue/late';
import { gradeOn, type Person } from '../people/people';
import { claimDue, rateFor, sum, type Rate } from '../rates/rates';

/** A period of the sheet: `name` is unique on the sheet (case ignored); `from` and `to` (yyyy-MM-dd) are both
 *  inclusive. No two periods of a sheet overlap. A closed period keeps `closedTotal`, the due-total fixed when it
 *  was closed; no claim dated inside it can be added, edited, opened, settled or voided. */
export type Period = {
  id: number;
  sheetId: string;
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
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

/** A period's due-total: the figures of the claims that count, each with its late charge (a held person's OPEN
 *  claims do not), whose date falls in it, added up (skipping n/a); for a closed period, the due-total fixed when it
 *  was closed. */
export function periodDue(
  period: Period,
  claims: Claim[] | undefined,
  people: Person[] | undefined,
  rates: Rate[] | undefined,
  floor: string,
  late: Late,
): string {
  if (period.closed && period.closedTotal !== null) return period.closedTotal;
  const byId = new Map((people ?? []).map((person) => [person.id, person]));
  const inPeriod = live(claims ?? [], people).filter((c) => period.from <= c.date && c.date <= period.to);
  return sum(
    inPeriod.map((c) => {
      const person = byId.get(c.personId);
      return claimDue(c, person === undefined ? undefined : rateFor(rates, gradeOn(person, c.date), c.date), floor, late);
    }),
  );
}
