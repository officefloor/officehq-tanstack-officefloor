import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { claimDueCents, counted } from '../claims/claims';
import type { Claim, Late } from '../claims/claims';
import { gradeOn } from '../people/people';
import type { Person } from '../people/people';
import { rateOf, twoDecimals } from '../rates/rates';
import type { Rate } from '../rates/rates';

/** A period on the sheet: its name (unique on the sheet) and its from and to date (yyyy-MM-dd), inclusive;
 *  whether it is closed, and its due-total fixed when closed (two decimals; null while open). */
export type Period = { name: string; from: string; to: string; closed: boolean; closedTotal: string | null };

export const periodsKey = (sheet: string) => ['periods', sheet] as const;

export const periodsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/periods`;

/** Where a period is closed. */
export const closePeriodPath = (sheet: string, name: string) =>
  `${periodsPath(sheet)}/${encodeURIComponent(name)}/close`;

/** Where a closed period is reopened (a steward only). */
export const reopenPeriodPath = (sheet: string, name: string) =>
  `${periodsPath(sheet)}/${encodeURIComponent(name)}/reopen`;

/** Every period on the sheet, by from-date; no two overlap. */
export function usePeriods(sheet: string) {
  return useQuery({
    queryKey: periodsKey(sheet),
    queryFn: () => getJson<Period[]>(periodsPath(sheet)),
  });
}

/** The period whose dates contain the date (yyyy-MM-dd), or undefined when none does. */
export function periodOf(periods: Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}

/** Each period's live due-total (in hundredths, by name): the due of the counted (OPEN and SETTLED, but not an unbound
 *  OPEN one within its person's hold) claims whose date falls in it (a claim with no rate adds nothing). */
export function periodDueCents(
  periods: Period[],
  claims: Claim[],
  people: Person[],
  rates: Rate[],
  floor: bigint,
  late: Late,
): Map<string, bigint> {
  const byId = new Map(people.map((p) => [p.id, p]));
  const totals = new Map<string, bigint>();
  for (const c of counted(claims, people)) {
    const period = periodOf(periods, c.date);
    const person = byId.get(c.person);
    if (period === undefined || person === undefined) continue;
    const due = claimDueCents(c, rateOf(rates, gradeOn(person, c.date), c.date), floor, late) ?? 0n;
    totals.set(period.name, (totals.get(period.name) ?? 0n) + due);
  }
  return totals;
}

/** A period's due-total as shown: the one fixed when it was closed, else its live one. */
export function shownDueTotal(period: Period, liveCents: bigint): string {
  return period.closed && period.closedTotal !== null ? period.closedTotal : twoDecimals(liveCents);
}
