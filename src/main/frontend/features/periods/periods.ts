import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { claimDueCents, counted } from '../claims/claims';
import type { Claim, Late } from '../claims/claims';
import { gradeOn } from '../people/people';
import type { Person } from '../people/people';
import { rateOf, twoDecimals } from '../rates/rates';
import type { Rate } from '../rates/rates';
import type { DatedCents } from '../settings/settings';

/** A period on the sheet: its name (unique on the sheet) and its from and to date (yyyy-MM-dd), inclusive;
 *  whether it is closed, its due-total fixed when closed (two decimals; null while open), and each person's due-total
 *  for it fixed then (by person id; empty while open, and a person added since has none). */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
  closedPeople: Record<string, string>;
};

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
  floor: DatedCents,
  late: Late,
): Map<string, bigint> {
  const totals = new Map<string, bigint>();
  for (const [name, byPerson] of personPeriodDueCents(periods, claims, people, rates, floor, late)) {
    totals.set(name, [...byPerson.values()].reduce((sum, due) => sum + due, 0n));
  }
  return totals;
}

/** Each period's live due-total per person (in hundredths, by period name then person id): the part of the period's
 *  due-total from the person's claims (see {@link periodDueCents}), so the people's add up to the period's. */
export function personPeriodDueCents(
  periods: Period[],
  claims: Claim[],
  people: Person[],
  rates: Rate[],
  floor: DatedCents,
  late: Late,
): Map<string, Map<number, bigint>> {
  const byId = new Map(people.map((p) => [p.id, p]));
  const totals = new Map<string, Map<number, bigint>>();
  for (const c of counted(claims, people)) {
    const period = periodOf(periods, c.date);
    const person = byId.get(c.person);
    if (period === undefined || person === undefined) continue;
    const due = claimDueCents(c, rateOf(rates, gradeOn(person, c.date), c.date), floor, late) ?? 0n;
    const byPerson = totals.get(period.name) ?? new Map<number, bigint>();
    byPerson.set(person.id, (byPerson.get(person.id) ?? 0n) + due);
    totals.set(period.name, byPerson);
  }
  return totals;
}

/** A person's due-total for a period as shown: the one fixed when it was closed (0.00 for a person added since), else
 *  their live one. */
export function shownPersonDueTotal(period: Period, personId: number, liveCents: bigint): string {
  return period.closed ? (period.closedPeople[String(personId)] ?? '0.00') : twoDecimals(liveCents);
}

/** A period's due-total as shown: the one fixed when it was closed, else its live one. */
export function shownDueTotal(period: Period, liveCents: bigint): string {
  return period.closed && period.closedTotal !== null ? period.closedTotal : twoDecimals(liveCents);
}
