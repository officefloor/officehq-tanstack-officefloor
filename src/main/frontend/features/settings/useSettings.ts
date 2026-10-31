import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { readPath, useAsOf } from '../asof/useAsOf';
import { useSheetToday } from '../sheet/useSheetToday';
import type { Policy } from '../rates/due';

/** A role a user name holds on the sheet: clerk (any name not listed), steward, viewer (sees everything, changes nothing)
 *  or restricted (sees on the people list only the people of their crews and themselves). */
export type Role = { user: string; role: string };

/** The sheet's settings, each dated one as in force on the sheet's today: the clerk limit with two decimals ("" for no limit), the floor with two
 *  decimals (a claim's live due is at least it), the terms in days (a new claim's due-by is its date
 *  plus them, unless its person's grade has its own terms), the late percent with two decimals (an overdue claim gains it of its figure for every
 *  full 30 days overdue), the policy date ("" for blank: the sheet's today), the period numbering from ("" for off: from it,
 *  a claim opened is numbered within its period), the roles set and each grade's own terms in days, by grade
 *  (a grade not listed uses the sheet's terms), the cap with two decimals ("" for none: a person's due-total is at most it) and the
 *  proof threshold with two decimals ("" for none: above the one in force on its date a claim needs a proof to be settled or approved)
 *  and each action's permission, by action: the roles that may take it, comma separated ("" for the standing rule),
 *  and every delegation of the steward role, in the order given, and the note threshold with two decimals ("" for none: above the one
 *  in force on its date a claim cannot be opened with an empty note), and the fiscal year start month, 1 to 12 ("1" unless set), and the rounding mode in force
 *  on the sheet's today (half-up or half-even; "" for none), and the review SLA in days ("" for none; not dated), and the proof kinds,
 *  comma separated ("" for none; not dated): above the proof threshold a claim needs one proof of each to be settled or approved,
 *  and the score thresholds, whole numbers ("" for blank; not dated): while both are set each person shows a band (see scoreBand). */
export type Settings = { clerkLimit: string; floor: string; terms: string; latePct: string; policyDate: string; periodNumberingFrom: string; roles: Role[]; gradeTerms: Record<string, string>; cap: string; dated: Record<string, Dated[]>; proofThreshold: string; perms: Record<string, string>; graceDays: string; delegations: Delegation[]; noteThreshold: string; entryDate: string; pot: string; parent: string; fiscalStartMonth: string; alertThreshold: string; rounding: string; reviewThreshold: string; escalateAfter: string; reviewSla: string; proofKinds: string; defaultGrade: string; scoreMid: string; scoreHigh: string };

/** A steward's delegation of the steward role: while the sheet's today is from..to (inclusive) the user acts as a
 *  steward, and the audit lines of their changes end with for=<by>. */
export type Delegation = { user: string; from: string; to: string; by: string };

/** A value recorded for a dated setting (clerk-limit, floor, terms, terms-<grade>, late-pct, cap): the date
 *  it is in force from and the value ("" for none). The figures shown in Settings are those in force on the
 *  sheet's today; these are every value recorded, oldest first. */
export type Dated = { from: string; value: string };

/** The value of a dated setting in force on a date: the one with the latest from-date on or before it,
 *  else the sheet's starting value. */
export function inForce(settings: Settings | undefined, key: string, date: string, start: string): string {
  const before = (settings?.dated?.[key] ?? []).filter((d) => d.from <= date);
  const found = before[before.length - 1];
  return found === undefined || found.value === '' ? start : found.value;
}

/** A person's band for their score: HIGH at or above the score-high, MID at or above the score-mid, else LOW;
 *  "" while either threshold is blank. */
export function scoreBand(settings: Settings | undefined, score: string): string {
  const mid = settings?.scoreMid ?? '';
  const high = settings?.scoreHigh ?? '';
  if (mid === '' || high === '') return '';
  const value = Number(score);
  return value >= Number(high) ? 'HIGH' : value >= Number(mid) ? 'MID' : 'LOW';
}

/** The floor a sheet starts with, used until its settings have loaded. */
export const DEFAULT_FLOOR = '1.00';

/** The actions the permission matrix covers; a blank entry keeps the standing rule: open, settle, void and close
 *  by clerk and steward, reassign and reopen by steward. */
export const PERM_ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen'] as const;

export const settingsKey = (sheet: string, asOf = '') => ['sheet', sheet, 'settings', asOf] as const;

export function useSettings(sheet: string): Settings | undefined {
  const asOf = useAsOf();
  const { data } = useQuery({
    queryKey: settingsKey(sheet, asOf),
    queryFn: () => getJson<Settings>(readPath(`/api/sheets/${encodeURIComponent(sheet)}/settings`, asOf)),
    enabled: sheet !== '',
  });
  return data;
}

/** The sheet's floor in force on a date (a claim's: its date). */
export function useFloor(sheet: string): (date: string) => string {
  const settings = useSettings(sheet);
  return (date) => inForce(settings, 'floor', date, DEFAULT_FLOOR);
}

/** The late percent a sheet starts with, used until its settings have loaded. */
export const DEFAULT_LATE_PCT = '0.00';

/** The sheet's late percent in force on a date (a late charge's: each 30-day boundary of an overdue claim). */
export function useLatePct(sheet: string): (date: string) => string {
  const settings = useSettings(sheet);
  return (date) => inForce(settings, 'late-pct', date, DEFAULT_LATE_PCT);
}

/** How the sheet's claims round: its policy date (the one set, or the sheet's today when it is blank; undefined while
 *  loading) and every rounding mode recorded, by from-date (see roundsHalfUp). */
export function usePolicy(sheet: string): Policy {
  const settings = useSettings(sheet);
  const today = useSheetToday(sheet);
  return { date: settings?.policyDate ? settings.policyDate : today, rounding: settings?.dated?.rounding ?? [] };
}

/** The sheet's cap in force on its today ("" for none): a person's due-total is at most it. */
export function useCap(sheet: string): string {
  return useSettings(sheet)?.cap ?? '';
}
