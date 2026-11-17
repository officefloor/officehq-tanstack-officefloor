import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { bound, counts, isOpen, type Claim, type ClaimStatus } from '../claims/claims';
import type { Late } from '../overdue/late';
import { gradeOn, retiredOn, type Person } from '../people/people';
import { claimDue, claimRate, sum, type Rate } from '../rates/rates';
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
  /** Soft-closed (not closed): its figures are fixed as for a close, but claims dated inside it can still change. */
  soft: boolean;
  closedTotal: string | null;
  /** Closed or soft-closed, each person's due-total for the period fixed when it was closed, by person id. */
  personTotals: Record<string, string>;
  /** The most OPEN or PENDING claims a person may have in it (by due-by); null for no cap. */
  cap: number | null;
  /** Closed or soft-closed, each person's carried figure for the period fixed when it was closed, by person id (empty
   *  for one closed before carried figures were kept). */
  personCarried: Record<string, string>;
  /** Each person's opening balances pasted for the period added up, by person id (added to their live carried figure). */
  openings: Record<string, string>;
  /** Locked by a steward: it cannot be reopened until unlocked. */
  locked: boolean;
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

/** The fiscal year a date (yyyy-MM-dd) falls in, named FY<yyyy> by the calendar year its first day falls in: a
 *  fiscal year starts on the first day of the start month (1 to 12). */
export function fiscalYearOf(date: string, startMonth: number): string {
  const year = Number(date.slice(0, 4));
  const month = Number(date.slice(5, 7));
  return `FY${month >= startMonth ? year : year - 1}`;
}

/** The status a claim had at the end of a day (yyyy-MM-dd), as its status log records it: the last line made on
 *  or before that day; undefined when the claim was added after it. */
export function statusAsOf(claim: Claim, day: string): ClaimStatus | undefined {
  const made = claim.log.filter((l) => l.date <= day);
  return made.length === 0 ? undefined : made[made.length - 1].status;
}

/** A person's due-total for a period: the figures (each due as shown now, with its late charge) of their claims
 *  whose due-by falls in it and that counted by the status each had at the end of the period's last day (see
 *  statusAsOf; a claim added after it does not count), the live rules otherwise applying (a deferred claim does
 *  not count, nor a claim open now of a person whose retirement date is reached,
 *  not bound by a statement), added up (skipping
 *  n/a); for a closed or soft-closed period, their figure fixed when it was (soft-)closed (0.00 for a person added after).
 *  With `statusNow`, each claim counts by its status now instead (the classic summary). */
export function personPeriodDue(
  period: Period,
  person: Person,
  claims: Claim[] | undefined,
  rates: Rate[] | undefined,
  floor: Dated,
  late: Late,
  statusNow = false,
): string {
  if (period.closed || period.soft) return period.personTotals?.[String(person.id)] ?? '0.00';
  const mine = (claims ?? []).filter((c) => {
    if (c.personId !== person.id || c.dueBy < period.from || period.to < c.dueBy) return false;
    const status = statusNow ? c.status : statusAsOf(c, period.to);
    return status !== undefined && counts({ ...c, status }) && !(isOpen(c) && !bound(c) && retiredOn(person, late.today));
  });
  return sum(mine.map((c) => claimDue(c, claimRate(rates, gradeOn(person, c.date), c), floor, late)));
}

/** A person's carried figure for a period: the figures (each due as shown now, with its late charge) of their claims
 *  that count and belong to an earlier period of the sheet (their due-by falls in a period ending before this one
 *  starts) and that were still OPEN or PENDING at the end of this period's last day (see statusAsOf), the live rules
 *  otherwise applying as for personPeriodDue, added up (skipping n/a) with their opening balances pasted for the period;
 *  0.00 when nothing is carried. For a closed or
 *  soft-closed period, their figure fixed when it was (soft-)closed (0.00 for a person added after). */
export function personPeriodCarried(
  period: Period,
  periods: Period[] | undefined,
  person: Person,
  claims: Claim[] | undefined,
  rates: Rate[] | undefined,
  floor: Dated,
  late: Late,
): string {
  const fixed = period.personCarried ?? {};
  if ((period.closed || period.soft) && Object.keys(fixed).length > 0) return fixed[String(person.id)] ?? '0.00';
  const earlier = (periods ?? []).filter((p) => p.to < period.from);
  const mine = (claims ?? []).filter((c) => {
    if (c.personId !== person.id || !earlier.some((p) => p.from <= c.dueBy && c.dueBy <= p.to)) return false;
    const status = statusAsOf(c, period.to);
    if (status !== 'OPEN' && status !== 'PENDING') return false;
    return counts({ ...c, status }) && !(isOpen(c) && !bound(c) && retiredOn(person, late.today));
  });
  const opening = period.openings?.[String(person.id)] ?? '0.00';
  return sum([...mine.map((c) => claimDue(c, claimRate(rates, gradeOn(person, c.date), c), floor, late)), opening]);
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

/** A period's classic due-total: as periodDue, but counting each claim by its status now (a closed or soft-closed
 *  period still shows the figures fixed when it was closed). */
export function classicPeriodDue(
  period: Period,
  claims: Claim[] | undefined,
  people: Person[] | undefined,
  rates: Rate[] | undefined,
  floor: Dated,
  late: Late,
): string {
  return sum((people ?? []).map((person) => personPeriodDue(period, person, claims, rates, floor, late, true)));
}
