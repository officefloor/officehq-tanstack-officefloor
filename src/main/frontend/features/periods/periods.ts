import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';
import { type Claim, type ClaimStatus } from '../claims/claims';
import { covers } from '../people/people';
import type { Person } from '../people/people';
import { type Floor, type Late, type Rate, claimDue, dueTotal } from '../rates/rates';

/** A period: its name (unique on the sheet), its from and to dates (yyyy-MM-dd, inclusive), whether
 *  it is closed and, once closed, the due-total fixed on closing (null while open) and each person's
 *  due-total fixed on closing, by person id (empty while open). No two periods on a sheet overlap. */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  total: string | null;
  people: Record<string, string>;
};

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

/** The claim's status at the end of the day (yyyy-MM-dd), as its status log records it (each line
 *  dated the sheet's today when the change was made); null when it had not been added by then. */
export function statusAt(claim: Claim, date: string): ClaimStatus | null {
  let status: ClaimStatus | null = null;
  for (const line of claim.log ?? []) {
    const m = /^(\S+) by .* on (\d{4}-\d{2}-\d{2}) due=/.exec(line);
    if (m && m[2] <= date) status = m[1] as ClaimStatus;
  }
  return status;
}

/** Whether the claim counts toward a period: by the status it had at the end of the period's last
 *  day it was OPEN, PENDING or SETTLED; and, as the live rules say, it is not deferred on the sheet's
 *  today, nor (while open then and unbound by a statement) dated within one of its person's holds. */
export function countsInPeriod(claim: Claim, period: Period, person: Person): boolean {
  const status = statusAt(claim, period.to);
  if (status === 'SETTLED') return true;
  if (status !== 'OPEN' && status !== 'PENDING') return false;
  return !claim.deferred && (claim.stated || !person.holds.some((h) => covers(h, claim.date)));
}

/** A person's due-total for a period: once closed, the figure fixed on closing (0.00 for a person
 *  added since); else the sum of the figures (as shown now) of their claims whose date falls in it
 *  and that count by their status at the period's end, skipping n/a. */
export function personPeriodDue(
  period: Period,
  person: Person,
  claims: Claim[] | undefined,
  rates: Rate[] | undefined,
  floor: Floor,
  late: Late,
): string {
  if (period.closed) return period.people?.[String(person.id)] ?? '0.00';
  return dueTotal(
    (claims ?? [])
      .filter((c) => c.personId === person.id && contains(period, c.date) && countsInPeriod(c, period, person))
      .map((c) => claimDue(c, rates, person.grades, floor, late)),
  );
}

/** A period's due-total as the list shows it: the sum of the people's due-totals for it. */
export function periodDue(
  period: Period,
  claims: Claim[] | undefined,
  people: Person[] | undefined,
  rates: Rate[] | undefined,
  floor: Floor,
  late: Late,
): string {
  return dueTotal((people ?? []).map((p) => personPeriodDue(period, p, claims, rates, floor, late)));
}
