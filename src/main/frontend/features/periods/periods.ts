import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts } from '../claims/claims';
import type { Claim } from '../claims/claims';
import type { Person } from '../people/people';
import { claimDue, total } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { sheetKey, useCurrentSheet } from '../session/session';
import type { DueRules } from '../settings/settings';

/**
 * A period as the store holds it: its name (unique on the sheet), its from and to dates (yyyy-MM-dd),
 * both inclusive, whether it is closed and, when closed, the due-total fixed then (null while open) and each
 * person's due-total fixed then, by person id (empty while open).
 */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
  personTotals: Record<string, string>;
};

/** The fields the period form submits. */
export type PeriodInput = { name: string; from: string; to: string };

/** The query key for the sheet's periods; under sheetKey, so re-loading the sheet re-reads them. */
export const periodsKey = (sheet: string) => [...sheetKey(sheet), 'periods'] as const;

/** The sheet's periods, earliest first. */
export function usePeriods() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: periodsKey(sheet),
    queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheet)}/periods`),
    enabled: !!sheet,
  });
}

/** Whether the date (yyyy-MM-dd) falls in the period, both ends included. */
export const contains = (p: Period, date: string) => p.from <= date && date <= p.to;

/** The period whose dates contain the date; undefined with none. Periods never overlap, so there is at most one. */
export const periodOf = (periods: Period[], date: string) => periods.find((p) => contains(p, date));

/**
 * A person's due-total for a period: the dues (late charges included) of their claims that count whose date falls
 * in it (open ones a hold covers left out), added up; a closed period keeps the figure fixed when it was closed
 * (0.00 for a person added since).
 */
export function personPeriodDue(period: Period, person: Person, claims: Claim[], rates: Rate[], rules: DueRules): string {
  if (period.closed) return period.personTotals?.[String(person.id)] ?? '0.00';
  const figures = claims
    .filter((c) => c.personId === person.id && counts(c, person.holds) && contains(period, c.date))
    .map((c) => claimDue(c, rates, person.grades, rules));
  return total(figures);
}

/** A period's due-total: the people's due-totals for it, added up. */
export function periodDue(period: Period, claims: Claim[], people: Person[], rates: Rate[], rules: DueRules): string {
  return total(people.map((p) => personPeriodDue(period, p, claims, rates, rules)));
}
