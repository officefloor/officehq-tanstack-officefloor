import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A period on the sheet: a name (unique on the sheet) and its from and to dates (yyyy-MM-dd), both
 *  inclusive. No two periods overlap. */
export type Period = { sheetId: string; name: string; from: string; to: string };
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
