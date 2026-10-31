import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Late } from '../claims/useClaims';
import { useSheetToday } from '../sheet/useSheet';

/** A role given to a user name: clerk (the default for any name) or steward. */
export type RoleEntry = { name: string; role: string };

/** The sheet's settings as the server sends them: `clerkLimit` with two decimals, or null for no
 *  limit, `floor` with two decimals `terms` in days, `latePct` with two decimals and `policyDate` as
 *  set (yyyy-mm-dd), or null when blank: then the policy date is the sheet's today, and `periodNumberingFrom`
 *  (yyyy-mm-dd), or null when off, and `gradeTerms` the terms set per grade (a grade not in it uses the
 *  sheet's terms), and `cap` with two decimals, or null for no cap — each dated one as in force on the
 *  sheet's today; and `dated`, every value recorded for the dated settings by key (clerk-limit, floor,
 *  terms, terms-<grade>, late-pct, cap, proof-threshold), oldest from-date first; and `proofThreshold` with
 *  two decimals, or null for none, as in force on the sheet's today; and `perms`, the roles set for each
 *  action of the permission matrix, comma separated (an action not in it keeps the standing rule); and `graceDays`, the grace days, or null when off (not dated); and `delegations`, every delegation of the steward role, oldest first. */
/** A delegation of the steward role: the user acts as a steward while the sheet's today is from
 *  `from` to `to` (yyyy-mm-dd, inclusive), for the steward `by` who delegated. */
export type DelegationEntry = { name: string; from: string; to: string; by: string };

export type Settings = { clerkLimit: string | null; floor: string; terms: number; latePct: string; policyDate: string | null; periodNumberingFrom: string | null; gradeTerms: Record<string, number>; cap: string | null; roles: RoleEntry[]; dated: Record<string, DatedValue[]>; proofThreshold: string | null; perms: Record<string, string>; graceDays: number | null; delegations: DelegationEntry[] };

/** A dated setting's value from a from-date (yyyy-mm-dd); null for blank. */
export type DatedValue = { from: string; value: string | null };

/** The floor of a sheet that has not set one. */
export const DEFAULT_FLOOR = '1.00';

/** The terms, in days, of a sheet that has not set them. */
export const DEFAULT_TERMS = 14;

export const settingsKey = (sheet: string) => ['sheet', sheet, 'settings'] as const;

/** The sheet's settings, re-read whenever the sheet is loaded. */
export function useSettings(sheet: string) {
  return useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`),
    staleTime: 0,
    gcTime: 0,
  });
}

/** The value of a dated setting in force on `date`: the one with the latest from-date on or before
 *  it, else `start` (the setting's starting value; null when it has none). A blank value is null. */
export function valueOn(settings: Settings | undefined, key: string, date: string, start: string | null): string | null {
  let found: DatedValue | undefined;
  for (const v of settings?.dated?.[key] ?? []) if (v.from <= date) found = v;
  return found === undefined ? start : (found.value ?? start);
}

/** The sheet's floor in force on a date: a claim's live due is at least the one on its date. */
export type FloorOn = (date: string) => string;

/** The sheet's floor, by date. */
export function useFloor(sheet: string): FloorOn {
  const settings = useSettings(sheet).data;
  return (date) => valueOn(settings, 'floor', date, DEFAULT_FLOOR) ?? DEFAULT_FLOOR;
}

/** The sheet's cap in force on its today: a person's due-total is at most it. Null for no cap. */
export function useCap(sheet: string): string | null {
  const settings = useSettings(sheet).data;
  const today = useSheetToday(sheet);
  return today === '' ? (settings?.cap ?? null) : valueOn(settings, 'cap', today, null);
}

/** The sheet's terms: a claim is due by its date plus this many days. */
export function useTerms(sheet: string): number {
  return useSettings(sheet).data?.terms ?? DEFAULT_TERMS;
}

/** The late percent of a sheet that has not set one. */
export const DEFAULT_LATE_PCT = '0.00';

/** What makes an overdue claim's due grow: the sheet's today and its late percent, by date. */
export function useLate(sheet: string): Late {
  const settings = useSettings(sheet).data;
  return {
    today: useSheetToday(sheet),
    pctOn: (date) => valueOn(settings, 'late-pct', date, DEFAULT_LATE_PCT) ?? DEFAULT_LATE_PCT,
  };
}

export const ROLES = ['clerk', 'steward', 'viewer'] as const;

/** A clerk limit the server accepts: an amount with up to two decimals, or blank for no limit. */
export const LIMIT = /^(\d{1,10}(\.\d{1,2})?)?$/;

/** A late percent the server accepts: a percent with up to two decimals, or blank for 0.00. */
export const PERCENT = /^(\d{1,5}(\.\d{1,2})?)?$/;

/** A policy date the server accepts: yyyy-mm-dd, or blank for the sheet's today. */
export const POLICY_DATE = /^(\d{4}-\d{2}-\d{2})?$/;

/** The grades terms can be set for. */
export const TERM_GRADES = ['A', 'B', 'C'] as const;

/** Terms the server accepts: a whole number of days, or blank for the default 14. */
export const DAYS = /^(\d{1,4})?$/;

/** A from-date the server accepts: yyyy-mm-dd (a blank one is not sent: the sheet's today). */
export const FROM_DATE = /^\d{4}-\d{2}-\d{2}$/;

/** The actions of the permission matrix: each may be given the roles allowed to take it. */
export const PERM_ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen'] as const;

/** A permission the server accepts: a comma separated list of roles, or blank for the standing rule. */
export const PERM_ROLES = /^(\s*(clerk|steward)\s*(,\s*(clerk|steward)\s*)*)?$/i;
