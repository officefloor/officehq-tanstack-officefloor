import { getJson } from '../../api/http';
import { counts, isOpen } from '../claims/claimsQuery';
import type { Claim, ClaimStatus } from '../claims/claimsQuery';
import type { Person } from '../people/peopleQuery';
import { figure, sumDue } from '../rates/ratesQuery';
import type { Late, Rate } from '../rates/ratesQuery';
import { sheetKey } from '../sheet/sheetQuery';
import type { DatedValue } from '../settings/dated';

/**
 * A sheet's period: a name unique on the sheet, `from` and `to` (yyyy-mm-dd) inclusive. Periods never overlap.
 * A closed period's due-total was fixed when it was closed (`closedTotal`; null while open), and so was each
 * person's due-total for it (`personTotals`, by person id; empty while open, and a person added later has none), and
 * each person's carried figure for it (`personCarried`, the same way; a period fixed before carried figures were has none).
 * A soft-closed period (`soft`, not `closed`) has its figures fixed the same way, while its claims can still change.
 * Each person's pasted opening balance for it (`personOpenings`, by person id; none is 0.00) is part of their carried figure.
 */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
  personTotals: Record<string, string>;
  personCarried: Record<string, string>;
  soft: boolean;
  /** How many OPEN or PENDING claims each person may have in the period; null for no cap. */
  cap: number | null;
  personOpenings: Record<string, string>;
};

/** Whether the period's figures are fixed: it is closed or soft-closed. */
export const isFixed = (period: Period) => period.closed || period.soft;

/** Under the sheet's key, so re-loading the sheet re-reads its periods. */
export const periodsKey = (sheetId: string) => [...sheetKey(sheetId), 'periods'] as const;

/** The sheet's periods, by from-date. */
export const periodsQuery = (sheetId: string) => ({
  queryKey: periodsKey(sheetId),
  queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`),
});

/** The period whose dates contain the date (yyyy-mm-dd), or undefined when none does. A claim's period is the one containing its due-by. */
export function periodOf(periods: Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}

/**
 * The claim's status at the end of the day `on` (yyyy-mm-dd), as its log records it: the last status logged on or
 * before it, DRAFT (counting nowhere) when it was added after. A claim with no log has its status now.
 */
export function statusOn(c: Claim, on: string): ClaimStatus {
  const log = c.log ?? [];
  if (log.length === 0) return c.status;
  let status: ClaimStatus = 'DRAFT';
  for (const line of log) {
    const m = /^(\S+) by .* on (\d{4}-\d{2}-\d{2}) due=\S*$/.exec(line);
    if (m && m[2] <= on) status = m[1] as ClaimStatus;
  }
  return status;
}

/**
 * A person's due-total for a period: the figures (as shown now) of their claims in it (by due-by) that counted by the
 * status they had at the end of its last day. A hold leaves out only a claim that is open now. While the period is
 * closed or soft-closed it is the figure fixed then (0.00 for a person added since).
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
  if (isFixed(period)) return period.personTotals[String(person.id)] ?? '0.00';
  return sumDue(
    claims
      .filter(
        (c) =>
          c.personId === person.id &&
          period.from <= c.dueBy &&
          c.dueBy <= period.to &&
          counts({ ...c, status: statusOn(c, period.to), held: c.held && isOpen(c) }, isOpen(c) ? people : [], late.today),
      )
      .map((c) => figure(c, rates, person, floor, late)),
  );
}

/**
 * A person's carried figure for a period: the figures (as shown now) of their claims that count which belong (by due-by)
 * to a period ending before this one starts and were still OPEN or PENDING at the end of its last day, by their log,
 * and their opening balance for it. While the period is closed or soft-closed it is the figure fixed then (0.00 for a person added since).
 */
export function personPeriodCarried(
  period: Period,
  periods: Period[],
  person: Person,
  claims: Claim[],
  people: Person[],
  rates: Rate[],
  floor: DatedValue[],
  late: Late,
): string {
  if (isFixed(period)) {
    const id = String(person.id);
    if (id in period.personCarried) return period.personCarried[id];
    if (!(id in period.personTotals)) return '0.00';
  }
  const opening = period.personOpenings?.[String(person.id)] ?? '0.00';
  return sumDue([
    opening,
    ...claims
      .filter((c) => {
        if (c.personId !== person.id) return false;
        const own = periodOf(periods, c.dueBy);
        if (!own || own.to >= period.from) return false;
        const status = statusOn(c, period.to);
        return (status === 'OPEN' || status === 'PENDING') && counts({ ...c, status, held: c.held && isOpen(c) }, isOpen(c) ? people : [], late.today);
      })
      .map((c) => figure(c, rates, person, floor, late)),
  ]);
}

/** A period's due-total: its people's due-totals for it added up. */
export function periodDue(period: Period, claims: Claim[], people: Person[], rates: Rate[], floor: DatedValue[], late: Late): string {
  return sumDue(people.map((p) => personPeriodDue(period, p, claims, people, rates, floor, late)));
}

/**
 * A period's classic due-total: the figures (as shown now) of its claims (by due-by) that count by their status now
 * (OPEN, SETTLED and PENDING; a hold or retirement leaves out an open claim as everywhere else). While the period is
 * closed or soft-closed it is the figure fixed then.
 */
export function classicPeriodDue(period: Period, claims: Claim[], people: Person[], rates: Rate[], floor: DatedValue[], late: Late): string {
  if (isFixed(period)) return period.closedTotal ?? sumDue(Object.values(period.personTotals));
  return sumDue(
    claims
      .filter((c) => period.from <= c.dueBy && c.dueBy <= period.to && counts(c, people, late.today))
      .map((c) => figure(c, rates, people.find((p) => p.id === c.personId), floor, late)),
  );
}
