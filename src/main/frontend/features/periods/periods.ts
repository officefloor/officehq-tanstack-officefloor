import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts } from '../claims/claims';
import type { Claim } from '../claims/claims';
import type { Person } from '../people/people';
import { claimDue, total } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { sheetKey, useCurrentSheet } from '../session/session';

/** A period as the store holds it: its name (unique on the sheet) and its from and to dates (yyyy-MM-dd), both inclusive. */
export type Period = { name: string; from: string; to: string };

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

/** A period's due-total: the figures of the claims that count whose date falls in it, added up. */
export function periodDue(period: Period, claims: Claim[], people: Person[], rates: Rate[]): string {
  const grades = new Map(people.map((p) => [p.id, p.grade]));
  const figures = claims
    .filter((c) => counts(c) && contains(period, c.date) && grades.has(c.personId))
    .map((c) => claimDue(c, rates, grades.get(c.personId)!));
  return total(figures);
}
