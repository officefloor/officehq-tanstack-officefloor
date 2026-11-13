import { useCallback, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';
import { useSheetToday } from '../session/sheetToday';
import { useClaims } from '../claims/claims';
import { gradeOn } from '../people/people';
import type { GradeFrom } from '../people/people';

/** A role given to a user name: clerk, steward or viewer (sees everything, changes nothing). A name with none given is a clerk. */
export type UserRole = { user: string; role: string };

/**
 * The sheet's settings as the store holds them: the clerk limit (two decimals; null: no limit), above
 * which a clerk cannot settle a claim, the floor (two decimals), below which no live due goes, the terms
 * (whole days) a claim is due by after its date, the terms set
 * for each grade that has its own (by grade; any other grade uses the sheet's terms), the late percent (two decimals) an overdue claim
 * gains of its figure every full 30 days, the rounding policy date (yyyy-MM-dd; '': blank, the
 * sheet's today), the period numbering from date (yyyy-MM-dd; '': off — on or after it, an opened claim is
 * numbered within its period), the entry date (yyyy-MM-dd; '': off — a new claim left without a date takes it instead of the
 * sheet's today), the cap (two decimals; '': none — a person's due-total is at most it), the proof threshold (two decimals; '': none — a
 * claim whose due is above the one in force on its date is settled or approved only once it has a proof), the note threshold (two decimals; '': none — a
 * claim whose due is above the one in force on its date is opened only with a note), and the roles
 * given to user names. The clerk limit, floor, terms, grade terms, late percent, cap, proof threshold and note threshold are those in force on the sheet's
 * today; `history` holds every value recorded for them, by key (floor, late-pct, terms, terms-<grade>, grace-<grade>, clerk-limit,
 * cap, proof-threshold, proof-threshold-<grade>, note-threshold), oldest from-date first (value '': blank). `permissions` is the permission matrix: by action, the
 * roles allowed it, comma separated ('': blank, the standing rule); it is not dated.
 * `graceDays` is the grace days (whole days; '': off — when the sheet's today is changed, an open period whose
 * to-date plus them is before the new today is closed); it is not dated.
 * `delegations` is the steward role delegated to user names, in the order made.
 * `pot` is the pot ('': none), shared out among each balanced pool's members; it is not dated.
 * `parent` is the parent sheet's id ('': none), whose rates a grade with none of its own in force takes; it is not dated.
 */
export type Settings = { clerkLimit: string | null; floor: string; terms: number; gradeTerms: Record<string, number>; latePct: string; policyDate: string; periodNumberingFrom: string; entryDate: string; cap: string; proofThreshold: string; noteThreshold: string; roles: UserRole[]; history: Record<string, FromValue[]>; permissions: Record<string, string>; graceDays: string; delegations: Delegation[]; pot: string; parent: string };

/**
 * The steward role delegated by a steward (`by`) to a user name, from a from-date to a to-date (yyyy-MM-dd), both
 * inclusive: while the sheet's today is inside them the user acts as a steward.
 */
export type Delegation = { user: string; from: string; to: string; by: string };

/** A dated setting's value recorded from a date (yyyy-MM-dd); '': blank. */
export type FromValue = { from: string; value: string };

/** The value of a dated setting in force on a date: the one with the latest from-date on or before it, else `start`. */
export function valueOn(history: FromValue[] | undefined, date: string, start: string): string {
  let found = start;
  for (const v of history ?? []) {
    if (v.from <= date) found = v.value === '' ? start : v.value;
  }
  return found;
}

/** The floor a sheet starts with: a claim's live due is at least it. */
export const DEFAULT_FLOOR = '1.00';

/** The terms a sheet starts with, in days: a claim is due by its date plus them. */
export const DEFAULT_TERMS = 14;

/** The late percent a sheet starts with. */
export const DEFAULT_LATE_PCT = '0.00';

/** The roles a user name can be given. */
export const ROLES = ['clerk', 'steward', 'viewer'];

/** The actions the permission matrix covers. */
export const PERMISSION_ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen'];

/** The roles allowed an action when its permission entry is blank. */
export const STANDING_PERMISSIONS: Record<string, string> = {
  open: 'clerk,steward',
  settle: 'clerk,steward',
  void: 'clerk,steward',
  reassign: 'steward',
  close: 'clerk,steward',
  reopen: 'steward',
};

/** Roles as a permission entry takes them: clerk or steward, comma separated. */
export const ROLE_LIST = /^\s*(clerk|steward)\s*(,\s*(clerk|steward)\s*)*$/i;

/** 0.00 or more, with at most two decimals. */
export const AMOUNT = /^(\d{1,13}(\.\d{1,2})?|\.\d{1,2})$/;

/** A percent: 0.00 or more, with at most two decimals. */
export const PERCENT = /^(\d{1,5}(\.\d{1,2})?|\.\d{1,2})$/;

/** 0 or more whole days. */
export const DAYS = /^\d{1,5}$/;

/** The query key for the sheet's settings; under sheetKey, so re-loading the sheet re-reads them. */
export const settingsKey = (sheet: string) => [...sheetKey(sheet), 'settings'] as const;

/** The sheet's settings. */
export function useSettings() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`),
    enabled: !!sheet,
  });
}

/**
 * The grace days of the grade of a claim's person in force on its date, in force on that date (0: none): the claim is
 * overdue only once its due-by plus them is before the sheet's today.
 */
export function claimGrace(claim: { date: string }, grades: GradeFrom[], rules: DueRules): number {
  return rules.graceOn(gradeOn(grades, claim.date), claim.date);
}

/** The sheet's floor (two decimals): a claim's live due is at least it. */
export function useFloor(): string {
  return useSettings().data?.floor ?? DEFAULT_FLOOR;
}


/** A date as the settings form takes it: yyyy-MM-dd. */
export const DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * What a claim's due is worked out from: the floor and late percent in force on a claim's date, the grace days of a grade
 * in force on a date (0: none), the sheet's today, its policy
 * date in force (blank: today), its cap in force on today ('': none) and the dates of its claims by number (a correction rounds by the date
 * of the claim it corrects).
 */
export type DueRules = {
  floorOn: (date: string) => string;
  latePctOn: (date: string) => string;
  graceOn: (grade: string | undefined, date: string) => number;
  today: string;
  policyDate: string;
  cap: string;
  claimDates: Map<string, string>;
};

/** The sheet's rules for working out a claim's due. */
export function useDueRules(): DueRules {
  const settings = useSettings().data;
  const today = useSheetToday();
  const claims = useClaims().data;
  const claimDates = useMemo(
    () => new Map((claims ?? []).filter((c) => c.number !== null).map((c) => [c.number!, c.date])),
    [claims],
  );
  const history = settings?.history;
  const floorOn = useCallback((date: string) => valueOn(history?.floor, date, DEFAULT_FLOOR), [history]);
  const latePctOn = useCallback((date: string) => valueOn(history?.['late-pct'], date, DEFAULT_LATE_PCT), [history]);
  const graceOn = useCallback(
    (grade: string | undefined, date: string) => (grade ? Number(valueOn(history?.[`grace-${grade}`], date, '0')) : 0),
    [history],
  );
  return {
    floorOn,
    latePctOn,
    graceOn,
    today,
    policyDate: settings?.policyDate || today,
    cap: settings?.cap ?? '',
    claimDates,
  };
}
