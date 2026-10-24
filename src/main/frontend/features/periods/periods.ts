import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { gradeOn, type Person } from '../people/people';
import { countsLive, holdsOf, type Claim } from '../claims/claims';
import { chargedOf, sumDue, type Rate } from '../rates/rates';
import type { Settings } from '../roles/roles';

/** A sheet's period as the store holds it: its name (unique on the sheet) and its from and to date
 *  (YYYY-MM-DD), inclusive. No two periods overlap. A closed period keeps the due-total fixed when it was closed
 *  (`closedTotal`, null while open) until a steward reopens it. */
export type Period = { id: number; name: string; from: string; to: string; closed: boolean; closedTotal: string | null };

export const periodsQueryKey = (sheetId: string) => ['sheet', sheetId, 'periods'] as const;

export const periodsPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/periods`;

export const periodClosePath = (sheetId: string, periodId: number) => `${periodsPath(sheetId)}/${periodId}/close`;

export const periodReopenPath = (sheetId: string, periodId: number) => `${periodsPath(sheetId)}/${periodId}/reopen`;

/** The sheet's periods, by from-date. */
export function usePeriods() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: periodsQueryKey(sheet ?? ''),
    queryFn: () => getJson<Period[]>(periodsPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** The period whose dates contain a date (YYYY-MM-DD), or undefined for none. */
export function periodOn(periods: Period[] | undefined, date: string): Period | undefined {
  return (periods ?? []).find((p) => p.from <= date && date <= p.to);
}

/** A period's due-total: the dues (with any late charge) of the counted claims whose date falls in it (not a person's
 *  unbound OPEN ones dated within a hold of theirs), added up, skipping n/a; for a closed period, the one fixed when it was closed. */
export function periodDue(period: Period, people: Person[] | undefined, claims: Claim[], rates: Rate[] | undefined,
  settings: Settings | undefined, today: string | undefined): string {
  if (period.closedTotal !== null) return period.closedTotal;
  const personOf = new Map((people ?? []).map((p) => [p.id, p]));
  const held = holdsOf(people);
  return sumDue(claims
    .filter((c) => countsLive(c, held) && personOf.has(c.personId) && periodOn([period], c.date))
    .map((c) => chargedOf(c, rates, gradeOn(personOf.get(c.personId)!, c.date), settings, today)));
}
