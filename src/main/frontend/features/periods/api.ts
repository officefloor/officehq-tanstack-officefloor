import { getJson, postJson } from '../../api/http';
import { counts, liveClaims, type Claim } from '../claims/api';
import { gradeOn, type Person } from '../people/api';
import { claimDue, sumDues, type Late, type Rate } from '../rates/api';
import { sheetKey } from '../sheet/api';

/** A period on the sheet: a name (unique on the sheet) and its from and to dates (yyyy-MM-dd), both
 *  inclusive. No two periods overlap. Once closed, its due-total is the one fixed when it was closed. */
export type Period = {
  sheetId: string; name: string; from: string; to: string; closed: boolean; closedTotal: string | null;
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

/** The period whose dates contain the date (yyyy-MM-dd), or undefined for none. */
export function periodOf(periods: Period[] | undefined, date: string): Period | undefined {
  return (periods ?? []).find((p) => p.from <= date && date <= p.to);
}

/** Close a period, fixing its due-total; refused (rejects) when it is already closed. No claim dated
 *  inside a closed period can then be added, edited, opened, settled or voided. */
export function closePeriod(sheetId: string, by: string, name: string): Promise<Period> {
  return postJson<Period>(`${base(sheetId)}/${encodeURIComponent(name)}/close?by=${encodeURIComponent(by)}`, {});
}

/** Reopen a closed period, making its due-total live again; refused (rejects) for anyone but a
 *  steward, and when it is not closed. */
export function reopenPeriod(sheetId: string, by: string, name: string): Promise<Period> {
  return postJson<Period>(`${base(sheetId)}/${encodeURIComponent(name)}/reopen?by=${encodeURIComponent(by)}`, {});
}

/** A period's due-total as its row shows it: once closed, the one fixed when it was closed; else the
 *  due of the live claims that count whose date falls in it (n/a ones skipped). */
export function periodDueTotal(p: Period, claims: Claim[], people: Person[] | undefined, rates: Rate[] | undefined,
  floor: string, late: Late): string {
  if (p.closed && p.closedTotal !== null) return p.closedTotal;
  const personOf = new Map((people ?? []).map((q) => [q.id, q]));
  return sumDues(liveClaims(claims, people).filter(counts).filter((c) => p.from <= c.date && c.date <= p.to)
    .map((c) => claimDue(c, rates, (d) => { const q = personOf.get(c.personId); return q ? gradeOn(q, d) : ''; }, floor, late)));
}
