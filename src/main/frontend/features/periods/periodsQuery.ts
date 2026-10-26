import { getJson } from '../../api/http';
import { counts } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import type { Person } from '../people/peopleQuery';
import { figure, sumDue } from '../rates/ratesQuery';
import type { Late, Rate } from '../rates/ratesQuery';
import { sheetKey } from '../sheet/sheetQuery';
import type { DatedValue } from '../settings/dated';

/**
 * A sheet's period: a name unique on the sheet, `from` and `to` (yyyy-mm-dd) inclusive. Periods never overlap.
 * A closed period's due-total was fixed when it was closed (`closedTotal`; null while open), and so was each
 * person's due-total for it (`personTotals`, by person id; empty while open, and a person added later has none).
 */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
  personTotals: Record<string, string>;
};

/** Under the sheet's key, so re-loading the sheet re-reads its periods. */
export const periodsKey = (sheetId: string) => [...sheetKey(sheetId), 'periods'] as const;

/** The sheet's periods, by from-date. */
export const periodsQuery = (sheetId: string) => ({
  queryKey: periodsKey(sheetId),
  queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`),
});

/** The period whose dates contain the date (yyyy-mm-dd), or undefined when none does. */
export function periodOf(periods: Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}

/**
 * A person's due-total for a period: the figures of their counting claims dated in it. While the period is
 * closed it is the figure fixed when it was closed (0.00 for a person added since).
 */
export function personPeriodDue(
  period: Period,
  person: Person,
  claims: Claim[],
  people: Person[],
  rates: Rate[],
  floor: DatedValue[],
  late: Late,
): string {
  if (period.closed) return period.personTotals[String(person.id)] ?? '0.00';
  return sumDue(
    claims
      .filter((c) => c.personId === person.id && counts(c, people, late.today) && period.from <= c.date && c.date <= period.to)
      .map((c) => figure(c, rates, person, floor, late)),
  );
}

/** A period's due-total: its people's due-totals for it added up. */
export function periodDue(period: Period, claims: Claim[], people: Person[], rates: Rate[], floor: DatedValue[], late: Late): string {
  return sumDue(people.map((p) => personPeriodDue(period, p, claims, people, rates, floor, late)));
}
