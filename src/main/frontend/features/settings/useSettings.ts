import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Late } from '../claims/useClaims';
import { useSheetToday } from '../sheet/useSheet';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';
import { gradeOn, usePeople } from '../people/usePeople';

/** A role given to a user name: clerk (the default for any name) or steward. */
export type RoleEntry = { name: string; role: string };

/** The sheet's settings as the server sends them: `clerkLimit` with two decimals, or null for no
 *  limit, `floor` with two decimals `terms` in days, `latePct` with two decimals and `policyDate` as
 *  set (yyyy-mm-dd), or null when blank: then the policy date is the sheet's today, and `periodNumberingFrom`
 *  (yyyy-mm-dd), or null when off, and `gradeTerms` the terms set per grade (a grade not in it uses the
 *  sheet's terms), and `cap` with two decimals, or null for no cap — each dated one as in force on the
 *  sheet's today; and `dated`, every value recorded for the dated settings by key (clerk-limit, floor,
 *  terms, terms-<grade>, grace-<grade>, late-pct, cap, proof-threshold, proof-threshold-<grade>, note-threshold, review-threshold, rounding), oldest from-date first; and `proofThreshold` with
 *  two decimals, or null for none, as in force on the sheet's today; and `perms`, the roles set for each
 *  action of the permission matrix, comma separated (an action not in it keeps the standing rule); and `graceDays`, the grace days, or null when off (not dated); and `delegations`, every delegation of the steward role, oldest first; and `noteThreshold` with two decimals, or null for none, as in force on the sheet's today; and `entryDate` (yyyy-mm-dd), or null when off: while set, the default date of a new claim in place of the sheet's today (not dated); and `gradeGrace`, the grace days set per grade in force on the sheet's today (a grade not in it has none; dated, under grace-<grade>); and `gradeProof`, the proof threshold set per grade with two decimals in force on the sheet's today (a grade not in it uses the sheet's; dated, under proof-threshold-<grade>); and `pot` with two decimals, or null for none: shared out among the members of each balanced pool (not dated); and `parent`, the parent sheet's id, or null for none: a grade with no rate in force on a date on this sheet takes the parent's then, up to three levels up (not dated); and `fiscalStartMonth`, the month (1 to 12) the sheet's fiscal year starts on: 1 unless set (not dated); and `rounding`, the rounding mode (half-up or half-even) in force on the sheet's today, or null for none: a claim dated when one is in force rounds its live figures by it, else by the policy date (dated); and `reviewThreshold` with two decimals, or null for none, as in force on the sheet's today: a claim whose due on its date is above the one then goes to REVIEW when opened (dated); and `escalateAfter`, the escalation gap in days, or null when off: reminding a claim whose previous reminder was at least that many days before the sheet's today escalates it, once per claim (not dated); and `reviewSla`, the review SLA in days, or null for none: a REVIEW claim whose review day plus it is before the sheet's today is review-late (not dated); and `proofKinds`, the sheet's proof kinds, comma separated (receipt, photo), or null for none: while set, a claim above the proof threshold needs a proof of each before it can be settled or approved (not dated); and `gradeClerkLimit`, the clerk limit set per grade with two decimals in force on the sheet's today (a grade not in it uses the sheet's; dated, under clerk-limit-<grade>): a claim's clerk limit is its person's grade's when set, else the sheet's. */
/** A delegation of the steward role: the user acts as a steward while the sheet's today is from
 *  `from` to `to` (yyyy-mm-dd, inclusive), for the steward `by` who delegated. */
export type DelegationEntry = { name: string; from: string; to: string; by: string };

export type Settings = { clerkLimit: string | null; floor: string; terms: number; latePct: string; policyDate: string | null; periodNumberingFrom: string | null; gradeTerms: Record<string, number>; cap: string | null; roles: RoleEntry[]; dated: Record<string, DatedValue[]>; proofThreshold: string | null; noteThreshold: string | null; perms: Record<string, string>; graceDays: number | null; delegations: DelegationEntry[]; entryDate: string | null; gradeGrace: Record<string, number>; gradeProof: Record<string, string>; pot: string | null; parent: string | null; fiscalStartMonth: number; alertThreshold: string | null; rounding: string | null; reviewThreshold: string | null; escalateAfter: number | null; reviewSla: number | null; proofKinds: string | null; defaultGrade: string | null; gradeClerkLimit: Record<string, string> };

/** A dated setting's value from a from-date (yyyy-mm-dd); null for blank. */
export type DatedValue = { from: string; value: string | null };

/** The floor of a sheet that has not set one. */
export const DEFAULT_FLOOR = '1.00';

/** The terms, in days, of a sheet that has not set them. */
export const DEFAULT_TERMS = 14;

export const settingsKey = (sheet: string) => ['sheet', sheet, 'settings'] as const;

/** The sheet's settings, re-read whenever the sheet is loaded. */
export function useSettings(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(settingsKey(sheet), asOf),
    queryFn: () => getJson<Settings>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/settings`, asOf)),
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

/** Whether a claim is review-late: REVIEW, and its review day (the sheet's today when it last went to
 *  REVIEW, from its status log) plus the sheet's review SLA days is before the sheet's today. Never
 *  while the sheet has no review SLA. */
export function useReviewLate(sheet: string): (claim: { status: string; log: string[] }) => boolean {
  const sla = useSettings(sheet).data?.reviewSla ?? null;
  const today = useSheetToday(sheet);
  return (claim) => {
    if (sla === null || today === '' || claim.status !== 'REVIEW') return false;
    const day = [...claim.log].reverse().map((l) => /^REVIEW by .* on (\d{4}-\d{2}-\d{2}) /.exec(l)).find((m) => m)?.[1];
    if (!day) return false;
    const due = new Date(`${day}T00:00:00Z`);
    due.setUTCDate(due.getUTCDate() + sla);
    return due.toISOString().slice(0, 10) < today;
  };
}

/** The sheet's terms: a claim is due by its date plus this many days. */
export function useTerms(sheet: string): number {
  return useSettings(sheet).data?.terms ?? DEFAULT_TERMS;
}

/** The late percent of a sheet that has not set one. */
export const DEFAULT_LATE_PCT = '0.00';

/** What makes an overdue claim's due grow: the sheet's today and its late percent, by date; and a
 *  claim's grace days: those of its person's grade in force on its date, in force on that date. */
export function useLate(sheet: string): Late {
  const settings = useSettings(sheet).data;
  const { data: people = [] } = usePeople(sheet);
  return {
    today: useSheetToday(sheet),
    pctOn: (date) => valueOn(settings, 'late-pct', date, DEFAULT_LATE_PCT) ?? DEFAULT_LATE_PCT,
    graceOn: (claim) => {
      const person = people.find((p) => p.id === claim.personId);
      if (person === undefined) return 0;
      const days = valueOn(settings, `grace-${gradeOn(person, claim.date)}`, claim.date, null);
      return days === null ? 0 : Number(days);
    },
  };
}

export const ROLES = ['clerk', 'steward', 'viewer'] as const;

/** The user's role on the sheet as every rule takes it: steward while a delegation to them covers
 *  the sheet's today, else the role given to their name (clerk unless given another). Under an as-of
 *  date, the role they had then. Null until the settings are read. */
export function useRoleOf(sheet: string, user: string): string | null {
  const settings = useSettings(sheet).data;
  const today = useSheetToday(sheet);
  if (settings === undefined || today === '') return null;
  if ((settings.delegations ?? []).some((d) => d.name === user && d.from <= today && today <= d.to)) return 'steward';
  return settings.roles.find((r) => r.name === user)?.role ?? 'clerk';
}


/** A clerk limit the server accepts: an amount with up to two decimals, or blank for no limit. */
export const LIMIT = /^(\d{1,10}(\.\d{1,2})?)?$/;

/** A late percent the server accepts: a percent with up to two decimals, or blank for 0.00. */
export const PERCENT = /^(\d{1,5}(\.\d{1,2})?)?$/;

/** A policy date the server accepts: yyyy-mm-dd, or blank for the sheet's today. */
export const POLICY_DATE = /^(\d{4}-\d{2}-\d{2})?$/;

/** The grades terms can be set for. */
export const TERM_GRADES = ['A', 'B', 'C'] as const;

/** Terms the server accepts: a whole number of days, or blank for the default 14. */
/** The fiscal year start month of a sheet that has not set one: January. */
export const DEFAULT_FISCAL_START_MONTH = 1;

/** A fiscal year start month: 1 to 12. */
export const MONTH = /^(0?[1-9]|1[0-2])$/;

/** A rounding mode: half-up or half-even, or blank for none. */
export const ROUNDING = /^(half-up|half-even)?$/;

export const DAYS = /^(\d{1,4})?$/;

/** A from-date the server accepts: yyyy-mm-dd (a blank one is not sent: the sheet's today). */
export const FROM_DATE = /^\d{4}-\d{2}-\d{2}$/;

/** The actions of the permission matrix: each may be given the roles allowed to take it. */
export const PERM_ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen'] as const;

/** A permission the server accepts: a comma separated list of roles, or blank for the standing rule. */
export const PERM_ROLES = /^(\s*(clerk|steward)\s*(,\s*(clerk|steward)\s*)*)?$/i;
/** The proof kinds: a comma separated list of receipt and photo, or blank for none. */
export const PROOF_KINDS = /^(\s*(receipt|photo)\s*(,\s*(receipt|photo)\s*)*)?$/i;
/** The default grade: one of the grades, or blank for A. */
export const DEFAULT_GRADE = /^(A|B|C)?$/;
