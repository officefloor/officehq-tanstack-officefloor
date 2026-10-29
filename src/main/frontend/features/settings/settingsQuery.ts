import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import type { Late } from '../rates/ratesQuery';
import { claimsQuery } from '../claims/claimsQuery';
import { GRADES, peopleQuery } from '../people/peopleQuery';
import type { Grade } from '../people/peopleQuery';
import { valueOn } from './dated';
import type { DatedValue } from './dated';


/** `clerkLimit` has two decimals ("1.50"), or is "" when there is no limit; `floor` has two decimals;
 *  `terms` is a whole number of days ("14"), and `gradeTerms` the terms in force for each grade (its own, else the sheet's); `latePct` is a percent with two decimals ("0.00");
 *  `policyDate` is yyyy-mm-dd, or "" when there is none (the sheet's today is the policy date); `periodNumberingFrom` is yyyy-mm-dd, or "" when numbering by period is off; `cap` has two decimals, or is "" when there is none; `graceDays` is a whole number of days (not dated), or "" when closing periods by today is off; `entryDate` is yyyy-mm-dd, or "" when it is off (a new claim left undated takes the sheet's today); `pot` has two decimals (not dated), or is "" when there is none; `alertThreshold` has two decimals (not dated), or is "" when alerts are off; `escalateAfter` is a whole number of days (not dated), or "" when escalation is off; `reviewSla` is a whole number of days (not dated), or "" when there is none; `proofKinds` are the proof kinds, comma separated (not dated), or "" when there are none; `parent` is the id of the sheet whose rates are used where this one has none (not dated), or "" when there is none; `fiscalStartMonth` is the month (1 to 12) the sheet's fiscal year starts on (not dated; "1" unless set).
 *  Those that carry a figure are as in force on the sheet's today when they were read; `dated` holds every value recorded for each of them, by key
 *  (clerk-limit, floor, terms, terms-<grade>, late-pct, cap, proof-threshold, proof-threshold-<grade>, note-threshold, review-threshold, grace-<grade>, rounding; a blank grace or grade's proof threshold is none (a grade with none uses the sheet's proof threshold); a blank clerk limit, cap, proof threshold, note threshold or review threshold is none).
 *  `perms` is the roles each action is allowed to, as a comma separated list, or "" for the standing rule (open, settle, void and close
 *  by clerk and steward; reassign and reopen by steward). */
export type Settings = { clerkLimit: string; floor: string; terms: string; latePct: string; policyDate: string; periodNumberingFrom: string; gradeTerms: Record<Grade, string>; cap: string; graceDays: string; entryDate: string; pot: string; alertThreshold: string; escalateAfter: string; reviewSla: string; proofKinds: string; parent: string; fiscalStartMonth: string; dated: Record<string, DatedValue[]>; perms: Record<PermAction, string> };

/** The actions the permission matrix decides; each is kept (not dated) under perm-<action>. */
export const PERM_ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen'] as const;
export type PermAction = (typeof PERM_ACTIONS)[number];

/** What the request carries: a setting left out (or blank, for the floor, terms, a grade's terms and late percent) is left as it is; a blank policy date is none.
 *  `from` is the from-date (yyyy-mm-dd) a dated setting is recorded from, by key; left out, it is the sheet's today.
 *  `perms` are the roles each action is allowed to, by action: left out is left as it is, blank is the standing rule. */
export type SettingsChange = {
  clerkLimit?: string;
  floor?: string;
  terms?: string;
  latePct?: string;
  policyDate?: string;
  periodNumberingFrom?: string;
  entryDate?: string;
  gradeTerms?: Partial<Record<Grade, string>>;
  /** A grade's grace days (days past a claim's due-by before it is overdue), by grade: left out is left as it is, blank is none. */
  gradeGrace?: Partial<Record<Grade, string>>;
  cap?: string;
  proofThreshold?: string;
  /** A grade's own proof threshold, by grade: left out is left as it is, blank is none (the sheet's is in force). */
  gradeProof?: Partial<Record<Grade, string>>;
  noteThreshold?: string;
  /** The due above which a claim opened goes to REVIEW instead of OPEN (dated): blank is none. */
  reviewThreshold?: string;
  /** How a claim's live figures round (dated): half-up or half-even; blank is none (the policy date decides). */
  rounding?: string;
  graceDays?: string;
  /** The pot a balanced pool's members cut by their shares (not dated): left out is left as it is, blank is none. */
  pot?: string;
  /** The due-total above which a person is alerted (not dated): left out is left as it is, blank is off. */
  alertThreshold?: string;
  /** The escalation gap, in days (not dated): left out is left as it is, blank is off. */
  escalateAfter?: string;
  /** The review SLA, in days (not dated): left out is left as it is, blank is none. */
  reviewSla?: string;
  /** The proof kinds, comma separated, a claim above the proof threshold needs one proof of each of (not dated): left out is left as it is, blank is none. */
  proofKinds?: string;
  /** The id of the parent sheet (not dated): left out is left as it is, blank is none. */
  parent?: string;
  /** The month (1 to 12) the fiscal year starts on (not dated): left out or blank is left as it is. */
  fiscalStartMonth?: string;
  from?: Record<string, string>;
  perms?: Partial<Record<PermAction, string>>;
};

/** The kinds a proof may be, receipt or photo; a proof added with none chosen is a receipt. */
export const PROOF_KINDS = ['receipt', 'photo'];

/** The rounding modes a sheet may have in force (dated); blank is none. */
export const ROUNDING_MODES = ['half-up', 'half-even'];

export type RoleName = 'clerk' | 'steward' | 'viewer';

/** The role a sheet gives a user name; a name with no entry is a clerk. */
export type Role = { user: string; role: RoleName };

export const ROLES: RoleName[] = ['clerk', 'steward', 'viewer'];

/** Under the sheet's key, so re-loading the sheet re-reads its settings and roles. */
export const settingsKey = (sheetId: string) => [...sheetKey(sheetId), 'settings'] as const;
export const rolesKey = (sheetId: string) => [...sheetKey(sheetId), 'roles'] as const;

export const settingsQuery = (sheetId: string) => ({
  queryKey: settingsKey(sheetId),
  queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
});

/** A dated setting's value in force on the sheet's today, or `starting` when none is recorded on or before it. */
export function useCurrent(sheetId: string, key: string, starting = ''): string {
  const today = useSheetToday(sheetId);
  return valueOn(useQuery(settingsQuery(sheetId)).data?.dated?.[key], today) ?? starting;
}

/** The sheet's floors by from-date: what a claim's live due is at least, as in force on the claim's date. */
export function useFloor(sheetId: string): DatedValue[] {
  return useQuery(settingsQuery(sheetId)).data?.dated?.floor ?? [];
}

/** The sheet's review SLA in days ("" for none, or until loaded). */
export function useReviewSla(sheetId: string): string {
  return useQuery(settingsQuery(sheetId)).data?.reviewSla ?? '';
}

/** The sheet's cap in force on its today: what a person's due-total is at most ("" for none). */
export function useCap(sheetId: string): string {
  return useCurrent(sheetId, 'cap');
}

/** The sheet's late percents by from-date: what an overdue claim gains of its figure for every full 30 days it is overdue, as in force on each 30-day boundary date (the due-by plus 30, 60, 90 ... days). */
export function useLatePct(sheetId: string): DatedValue[] {
  return useQuery(settingsQuery(sheetId)).data?.dated?.['late-pct'] ?? [];
}

/** The sheet's policy date (yyyy-mm-dd): the one set, or the sheet's today when there is none. */
export function usePolicyDate(sheetId: string): string {
  const today = useSheetToday(sheetId);
  return useQuery(settingsQuery(sheetId)).data?.policyDate || today;
}

/** What a claim's late charge and its rounding depend on, as the sheet has it now. */
export function useLate(sheetId: string): Late {
  const dated = useQuery(settingsQuery(sheetId)).data?.dated;
  return {
    grace: Object.fromEntries(GRADES.map((g) => [g, dated?.[`grace-${g}`] ?? []])),
    people: useQuery(peopleQuery(sheetId)).data ?? [],
    today: useSheetToday(sheetId),
    pct: useLatePct(sheetId),
    policy: usePolicyDate(sheetId),
    rounding: dated?.rounding ?? [],
    claims: useQuery(claimsQuery(sheetId)).data ?? [],
  };
}

/** The month (1 to 12) the sheet's fiscal year starts on. */
export function useFiscalStartMonth(sheetId: string): number {
  return Number(useQuery(settingsQuery(sheetId)).data?.fiscalStartMonth || '1');
}

/** The fiscal year a date (yyyy-mm-dd) falls in, named FY<yyyy> by the calendar year its first day falls in. */
export function fiscalYear(date: string, startMonth: number): string {
  const year = Number(date.slice(0, 4));
  const month = Number(date.slice(5, 7));
  return `FY${month >= startMonth ? year : year - 1}`;
}

export const rolesQuery = (sheetId: string) => ({
  queryKey: rolesKey(sheetId),
  queryFn: () => getJson<Role[]>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`),
});
