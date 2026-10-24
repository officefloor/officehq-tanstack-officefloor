import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A user name's role on the sheet: clerk, steward or viewer. A name with none is a clerk. */
export type Role = { user: string; role: string };

/** The sheet's settings: its clerk limit with two decimals (null when there is none) and its floor with two
 *  decimals (a claim's live due is at least it), and its terms in days (a claim is due by its date plus them), and its late percent with two decimals (an overdue claim's due gains
 *  it of its figure for every full 30 days overdue), and its policy date (YYYY-MM-DD; "" when blank: the sheet's today — a claim
 *  dated before it rounds half up, one on or after it half to even), and its period numbering from (YYYY-MM-DD; "" when off — while the sheet's today is
 *  on or after it, a claim opened is numbered within its period), and the own terms in days of grades A, B and C ("" when the
 *  grade uses the sheet's terms; a claim's due-by is fixed when it is created, from its person's grade on its date), and its cap with
 *  two decimals ("" when there is none — a person's due-total is at most it), and its proof threshold with two decimals ("" when there is none — a claim whose due is
 *  above the one in force on its date cannot be settled or approved without a proof), and its grace days ("" when off; not dated). Each of those is the value in force on the
 *  sheet's today; `dated` holds the values recorded for each setting that carries a figure, by key (floor, late-pct,
 *  terms, terms-A/B/C, clerk-limit, cap, proof-threshold), each from its from-date on (null: from the beginning of time; a null value is blank). */
export type Settings = { clerkLimit: string | null; floor: string; terms: string;
  latePct: string; policyDate: string; periodNumberingFrom: string;
  termsA: string; termsB: string; termsC: string; cap: string; proofThreshold: string;
  dated: Record<string, { from: string | null; value: string | null }[]>;
  permOpen: string; permSettle: string; permVoid: string; permReassign: string; permClose: string; permReopen: string;
  graceDays: string };

/** The value of a dated setting in force on a date (YYYY-MM-DD): the one recorded with the latest from-date on or
 *  before it, else `start` (the sheet's starting value). */
export function settingOn(settings: Settings | undefined, key: string, date: string, start: string): string {
  let found: { from: string | null; value: string | null } | undefined;
  for (const d of settings?.dated?.[key] ?? []) {
    if (d.from !== null && d.from > date) continue;
    if (!found || (found.from ?? '') <= (d.from ?? '')) found = d;
  }
  return found ? (found.value ?? start) : start;
}

/** The terms a sheet starts at, in days. */
export const DEFAULT_TERMS = '14';

export const rolesQueryKey = (sheetId: string) => ['sheet', sheetId, 'roles'] as const;
export const rolesPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/roles`;

export const settingsQueryKey = (sheetId: string) => ['sheet', sheetId, 'settings'] as const;
export const settingsPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/settings`;

/** The user names given a role on the sheet, by name. */
export function useRoles() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: rolesQueryKey(sheet ?? ''),
    queryFn: () => getJson<Role[]>(rolesPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

export function useSettings() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: settingsQueryKey(sheet ?? ''),
    queryFn: () => getJson<Settings>(settingsPath(sheet ?? '')),
    enabled: !!sheet,
  });
}
