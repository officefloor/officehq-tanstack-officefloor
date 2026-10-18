import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { useClaimDue, useClaims, useCounts } from '../claims/claims';
import { hundredths, usePersonRate } from '../rates/rates';

// A period is a name, unique on the sheet, and a from and to date (yyyy-mm-dd), inclusive; no two
// periods of a sheet overlap. A claim belongs to the period whose dates contain its date, or to none.
// A period may be closed: its due-total is then the one fixed when it was closed (closedTotal), and
// no claim dated inside it can be added, edited, opened, settled or voided.
// A steward may reopen a closed period, making its due-total live again.
export type Period = { name: string; from: string; to: string; closed: boolean; closedTotal: string | null };
export type PeriodInput = { name: string; from: string; to: string };

const base = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/periods`;
export const periodsKey = (sheet: string) => ['sheet', sheet, 'periods'] as const;

/** The active sheet's periods, by from-date. */
export function usePeriods() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: periodsKey(sheet ?? ''),
    queryFn: () => getJson<{ periods: Period[] }>(base(sheet!)).then((r) => r.periods),
    enabled: !!sheet,
  });
}

export const addPeriod = (sheet: string, user: string, input: PeriodInput) => postJson<Period>(base(sheet), input, user);

export const closePeriod = (sheet: string, user: string, name: string) =>
  postJson<Period>(`${base(sheet)}/${encodeURIComponent(name)}/close`, {}, user);

export const reopenPeriod = (sheet: string, user: string, name: string) =>
  postJson<Period>(`${base(sheet)}/${encodeURIComponent(name)}/reopen`, {}, user);

/** The period whose dates contain the date, or undefined. */
export const periodOf = (periods: Period[], date: string): Period | undefined =>
  periods.find((p) => p.from <= date && date <= p.to);

/** The name of the period a date (yyyy-mm-dd) falls in on the active sheet, or undefined. */
export function usePeriodOf() {
  const periods = usePeriods().data ?? [];
  return (date: string) => periodOf(periods, date)?.name;
}

/** The closed period a date (yyyy-mm-dd) falls in on the active sheet, or undefined. */
export function useClosedPeriodOf() {
  const periods = usePeriods().data ?? [];
  return (date: string) => {
    const p = periodOf(periods, date);
    return p?.closed ? p : undefined;
  };
}

/** A period's due-total in cents: once closed, the one fixed then; otherwise the due of the claims that
 *  count whose date falls in it (n/a as 0). */
export function usePeriodDue() {
  const claims = useClaims().data ?? [];
  const rateOf = usePersonRate();
  const counts = useCounts();
  const claimDue = useClaimDue();
  return (period: Period) =>
    period.closed && period.closedTotal !== null
      ? hundredths(period.closedTotal)
      : claims
      .filter((c) => counts(c) && period.from <= c.date && c.date <= period.to)
      .reduce((n, c) => n + (claimDue(c, rateOf(c.personId, c.date)) ?? 0n), 0n);
}
