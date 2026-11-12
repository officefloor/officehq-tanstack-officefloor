import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';
import { type Claim, counts, live } from '../claims/claims';
import type { Person } from '../people/people';
import { type Late, type Rate, claimDue, dueTotal } from '../rates/rates';

/** A period: its name (unique on the sheet), its from and to dates (yyyy-MM-dd, inclusive), whether
 *  it is closed and, once closed, the due-total fixed on closing (null while open). No two periods on
 *  a sheet overlap. */
export type Period = { name: string; from: string; to: string; closed: boolean; total: string | null };

/** Under the sheet's key, so loading the sheet again re-reads its periods. */
export const periodsKey = (sheet: string) => [...sheetKey(sheet), 'periods'] as const;

/** Every period on the sheet, by from-date. */
export function usePeriods(sheet: string) {
  return useQuery({
    queryKey: periodsKey(sheet),
    queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheet)}/periods`),
    enabled: sheet !== '',
  });
}

/** Whether the date (yyyy-MM-dd) falls in the period. */
export const contains = (period: Period, date: string) => period.from <= date && date <= period.to;

/** The period whose dates contain the date, or undefined when none does. */
export function periodOf(periods: Period[] | undefined, date: string): Period | undefined {
  return (periods ?? []).find((p) => contains(p, date));
}

/** A period's due-total as the list shows it: the total fixed on closing, else the sum of the figures
 *  of the claims that count whose date falls in it, skipping n/a. */
export function periodDue(
  period: Period,
  claims: Claim[] | undefined,
  people: Person[] | undefined,
  rates: Rate[] | undefined,
  floor: string | undefined,
  late: Late,
): string {
  if (period.closed && period.total !== null) return period.total;
  const gradesOf = new Map((people ?? []).map((p) => [p.id, p.grades]));
  return dueTotal(
    live(claims ?? [], people)
      .filter((c) => counts(c) && contains(period, c.date) && gradesOf.has(c.personId))
      .map((c) => claimDue(c, rates, gradesOf.get(c.personId)!, floor, late)),
  );
}
