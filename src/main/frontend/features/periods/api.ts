import { getJson, postJson } from '../../api/http';
import { liveClaims, type Claim, type ClaimStatus } from '../claims/api';
import { gradeOn, type Person } from '../people/api';
import { claimDue, sumDues, type Late, type Rate } from '../rates/api';
import type { DatedOn } from '../settings/api';
import { sheetKey } from '../sheet/api';

/** A period on the sheet: a name (unique on the sheet) and its from and to dates (yyyy-MM-dd), both
 *  inclusive. No two periods overlap. Once closed, its due-total is the one fixed when it was closed. */
export type Period = {
  sheetId: string; name: string; from: string; to: string; closed: boolean; closedTotal: string | null;
  /** Once closed, each person's due-total for it fixed when it was closed, by person id. */
  personTotals: Record<string, string>;
  /** Soft-closed: its figures are fixed as when closed, but claims due by a day in it can still be added and changed. */
  soft: boolean;
};
export type PeriodInput = { name: string; from: string; to: string };

const base = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/periods`;

export const periodsKey = (sheetId: string) => [...sheetKey(sheetId), 'periods'] as const;

/** The sheet's periods, by from-date. */
export function listPeriods(sheetId: string): Promise<Period[]> {
  return getJson<Period[]>(base(sheetId));
}

/** Add a period; refused (rejects) when the name is missing or already in use, a date is not a date,
 *  the from-date is after the to-date, or the dates overlap another period's. */
export function addPeriod(sheetId: string, by: string, input: PeriodInput): Promise<Period> {
  return postJson<Period>(`${base(sheetId)}?by=${encodeURIComponent(by)}`, input);
}

/** Make one period per calendar month from the from-date's month to the to-date's month (a blank date
 *  is the sheet's today), each from the first to the last day of its month and named yyyy-mm, leaving out
 *  a month whose name is taken or whose days overlap a period; refused (rejects) when the to-date is
 *  before the from-date. Resolves to how many were made. */
export function generatePeriods(sheetId: string, by: string, from: string, to: string): Promise<{ made: number }> {
  return postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/generate-periods?by=${encodeURIComponent(by)}`,
    { from, to });
}

/** Make fiscal year FY<year>'s twelve monthly periods, from the sheet's fiscal year start month of that
 *  year, exactly as `generatePeriods` does; refused (rejects) when the year is not yyyy. Resolves to how
 *  many were made. */
export function generateFiscalYear(sheetId: string, by: string, year: string): Promise<{ made: number }> {
  return postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/generate-fiscal-year?by=${encodeURIComponent(by)}`,
    { year });
}

/** The period whose dates contain the date (yyyy-MM-dd), or undefined for none. A claim belongs to the
 *  period its due-by falls in. */
export function periodOf(periods: Period[] | undefined, date: string): Period | undefined {
  return (periods ?? []).find((p) => p.from <= date && date <= p.to);
}

/** Close a period, fixing its due-total; refused (rejects) when it is already closed. No claim due by a day
 *  inside a closed period can then be added, edited, opened, settled or voided. */
export function closePeriod(sheetId: string, by: string, name: string): Promise<Period> {
  return postJson<Period>(`${base(sheetId)}/${encodeURIComponent(name)}/close?by=${encodeURIComponent(by)}`, {});
}

/** Soft-close a period, fixing its due-total and everyone's figure for it; claims due by a day in it can still be
 *  added and changed. Refused (rejects) when it is already closed or soft-closed. */
export function softClosePeriod(sheetId: string, by: string, name: string): Promise<Period> {
  return postJson<Period>(`${base(sheetId)}/${encodeURIComponent(name)}/soft-close?by=${encodeURIComponent(by)}`, {});
}

/** Reopen a closed or soft-closed period, making its due-total live again; refused (rejects) for anyone but a
 *  steward, and when it is not closed. */
export function reopenPeriod(sheetId: string, by: string, name: string): Promise<Period> {
  return postJson<Period>(`${base(sheetId)}/${encodeURIComponent(name)}/reopen?by=${encodeURIComponent(by)}`, {});
}

/** A claim's status at the end of a day (yyyy-MM-dd), as its status log records it: the last status
 *  logged on or before that day; null when it was added after it. A claim with no log (added before
 *  the log) has its status now. */
export function statusOn(c: Claim, date: string): ClaimStatus | null {
  if (c.log.length === 0) return c.status;
  let status: ClaimStatus | null = null;
  for (const line of c.log) {
    const m = / on (\d{4}-\d{2}-\d{2}) due=[^ ]*$/.exec(line);
    if (m && m[1] <= date) status = line.slice(0, line.indexOf(' ')) as ClaimStatus;
  }
  return status;
}

/** Whether a claim counted at the end of a day: its status then was OPEN, PENDING or SETTLED. */
export const countedOn = (c: Claim, date: string) => {
  const s = statusOn(c, date);
  return s === 'OPEN' || s === 'PENDING' || s === 'SETTLED';
};

/** A person's due-total for a period: once closed or soft-closed, the one fixed for them when it was closed (0.00
 *  for a person added later); else the due as shown now of their claims due by a day in it that counted by
 *  their status at the end of its last day (see `statusOn`), less those the live rules leave out
 *  (deferred, held; see `liveClaims`), n/a ones skipped. */
export function personPeriodDue(p: Period, person: Person, claims: Claim[], people: Person[] | undefined,
  rates: Rate[] | undefined, floor: DatedOn, late: Late): string {
  if (p.closed || p.soft) return p.personTotals[String(person.id)] ?? '0.00';
  return sumDues(liveClaims(claims.filter((c) => c.personId === person.id), people).filter((c) => countedOn(c, p.to))
    .filter((c) => p.from <= c.dueBy && c.dueBy <= p.to)
    .map((c) => claimDue(c, rates, (d) => gradeOn(person, d), floor, late)));
}

/** A period's due-total as its row shows it: the sum of everyone's due-total for it. */
export function periodDueTotal(p: Period, claims: Claim[], people: Person[] | undefined, rates: Rate[] | undefined,
  floor: DatedOn, late: Late): string {
  return sumDues((people ?? []).map((q) => personPeriodDue(p, q, claims, people, rates, floor, late)));
}
