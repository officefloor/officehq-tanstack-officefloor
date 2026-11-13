import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts, heldIds } from '../claims/claims';
import type { Claim } from '../claims/claims';
import type { Person } from '../people/people';
import { claimDue, total } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { sheetKey, useCurrentSheet } from '../session/session';
import type { DueRules } from '../settings/settings';

/**
 * A period as the store holds it: its name (unique on the sheet), its from and to dates (yyyy-MM-dd),
 * both inclusive, whether it is closed and, when closed, the due-total fixed then (null while open).
 */
export type Period = { name: string; from: string; to: string; closed: boolean; closedTotal: string | null };

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
 * A period's due-total: the dues (late charges included) of the claims that count whose date falls in it (a held person's
 * open ones left out), added up; a
 * closed period keeps the total fixed when it was closed, whatever the rates do later.
 */
export function periodDue(period: Period, claims: Claim[], people: Person[], rates: Rate[], rules: DueRules): string {
  if (period.closed && period.closedTotal !== null) return period.closedTotal;
  const grades = new Map(people.map((p) => [p.id, p.grades]));
  const held = heldIds(people);
  const figures = claims
    .filter((c) => counts(c, held.has(c.personId)) && contains(period, c.date) && grades.has(c.personId))
    .map((c) => claimDue(c, rates, grades.get(c.personId)!, rules));
  return total(figures);
}
