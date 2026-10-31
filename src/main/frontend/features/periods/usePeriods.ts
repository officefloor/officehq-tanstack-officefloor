import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { claimDueCents, type Claim, type ClaimStatus, type Late } from '../claims/useClaims';
import { gradeOn, heldOn, type Person } from '../people/usePeople';
import { hundredths, rateOn, totalDue, type Rate } from '../rates/useRates';
import type { FloorOn } from '../settings/useSettings';

/** A period as the server sends it: its name, its from and to dates (yyyy-mm-dd), inclusive, whether
 *  it is closed and, when it is, the due-total fixed when it was closed and each person's (by person
 *  id; a person not in it, added since, has 0.00). */
export type Period = {
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
  closedPersonTotals: Record<string, string>;
};

export const periodsKey = (sheet: string) => ['sheet', sheet, 'periods'] as const;

/** The sheet's periods, by from-date, re-read whenever the sheet is loaded. */
export function usePeriods(sheet: string) {
  return useQuery({
    queryKey: periodsKey(sheet),
    queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheet)}/periods`),
    staleTime: 0,
    gcTime: 0,
  });
}

/** A date the server accepts: yyyy-mm-dd. */
export const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** The period whose dates contain `date` (yyyy-mm-dd), or undefined when none does. Periods never
 *  overlap, so there is at most one. */
export function periodOf(periods: Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}

/** The status a claim had at the end of `date` (yyyy-mm-dd), as its status log records it: the last
 *  line dated on or before it; null when the claim was added after it. A claim kept before the log
 *  began (no lines) goes by its status now. */
export function statusAt(claim: Claim, date: string): ClaimStatus | null {
  if (claim.log.length === 0) return claim.status;
  let status: ClaimStatus | null = null;
  for (const line of claim.log) {
    const m = /^(\S+) by .* on (\d{4}-\d{2}-\d{2}) due=/.exec(line);
    if (m && m[2] <= date) status = m[1] as ClaimStatus;
  }
  return status;
}

/** Whether a person's claim counts in a period's due-total: dated in it, and counted (OPEN, PENDING
 *  or SETTLED) by the status it had at the end of the period's last day — not a deferred one, nor
 *  one open then and dated within the person's hold, unless a statement binds it. */
function countsIn(period: Period, person: Person, claim: Claim): boolean {
  if (claim.personId !== person.id || periodOf([period], claim.date) === undefined || claim.deferred) return false;
  const status = statusAt(claim, period.to);
  if (status !== 'OPEN' && status !== 'PENDING' && status !== 'SETTLED') return false;
  if (status === 'SETTLED' || !heldOn(person, claim.date)) return true;
  return claim.stated && status !== 'PENDING';
}

/** A person's due-total for a period, in cents: the one fixed when the period was closed (0 for a
 *  person added since), else the dues, as shown now, of their claims that count in it (see
 *  `countsIn`: by the status each had at the period's end), n/a ones skipped. */
export function personPeriodDueCents(period: Period, person: Person, claims: Claim[], rates: Rate[], floor: FloorOn, late: Late): bigint {
  if (period.closed) {
    const fixed = period.closedPersonTotals?.[String(person.id)];
    return fixed === undefined ? 0n : hundredths(fixed);
  }
  return claims
    .filter((c) => countsIn(period, person, c))
    .reduce<bigint>((sum, c) => sum + (claimDueCents(c, floor, rateOn(rates, gradeOn(person, c.date), c.date), late) ?? 0n), 0n);
}

/** A period's due-total: everyone's due-total for it added up. */
export function periodDue(period: Period, claims: Claim[], people: Person[], rates: Rate[], floor: FloorOn, late: Late): string {
  return totalDue(people.map((p) => personPeriodDueCents(period, p, claims, rates, floor, late)));
}
