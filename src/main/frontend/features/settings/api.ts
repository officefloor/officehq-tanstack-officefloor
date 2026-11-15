import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { liveKey, sheetKey } from '../sheet/api';
import type { Late } from '../rates/api';
import { dueBy, type Claim } from '../claims/api';
import { gradeOn, listPeople, peopleKey } from '../people/api';

/** A value recorded for a sheet setting: its value as text ("" when blank), in force from its
 *  from-date (yyyy-MM-dd; null: from the beginning of time, and always for an undated key). A key
 *  may have several, one per from-date; a setting never set is not listed. */
export type Setting = { sheetId: string; key: string; value: string; from: string | null };
/** The role a user name has on the sheet; a name not listed is a clerk. */
export type Role = { sheetId: string; user: string; role: string };

/** The settings the screen offers: the clerk limit (two decimals; blank means no limit), the cap
 *  (two decimals; blank means none: the most a person's due-total can be), the pot (two decimals;
 *  blank means none; not dated: the sum a balanced pool's members cut between them), the
 *  floor (two decimals; the least a claim's live due can be) and the terms (whole days after its
 *  date by which a claim is due) and the late percent (two decimals; the share of an overdue claim's
 *  figure it gains for every full 30 days it is overdue) and the policy date (yyyy-MM-dd; blank means
 *  the sheet's today: a claim dated before it rounds half up, on or after it half to even) and the
 *  period numbering date (yyyy-MM-dd; blank means off: once the sheet's today is on or after it, a
 *  claim opened is numbered within its period, <period>-<n>, or X-<n> in no period). A grade's own terms
 *  (terms-<grade>; blank means the sheet's terms) fix the due-by of a claim added for a person of that
 *  grade on the sheet's today then (those in force that day): its date plus those terms, kept when the terms or its date change later. The proof
 *  threshold (two decimals; blank means none): a claim whose due is above the one in force on its date
 *  cannot be settled or approved while it has no proofs. A grade's proof threshold (proof-threshold-<grade>,
 *  two decimals; blank means none): a claim whose person's grade in force on its date has one in force on
 *  that date goes by it instead of the sheet's. The proof kinds (a comma separated list of receipt and photo;
 *  blank means none; not dated): while set, a claim above the proof threshold needs at least one proof of
 *  each before it can be settled or approved. The note threshold (two decimals; blank means none):
 *  a claim whose due is above the one in force on its date cannot be opened with an empty note. The review
 *  threshold (two decimals; blank means none): a claim whose due is above the one in force on its date goes
 *  to REVIEW when opened instead of OPEN, until a steward reviews it. The permission matrix (perm-<action>, a comma
 *  separated list of roles; blank means the standing rule, PERMISSION_STANDING): who may take each action.
 *  The entry date (yyyy-MM-dd; blank means off): while set, a new claim with a blank date (the claim form,
 *  the quick-add bar, a pasted line, a correction) takes it instead of the sheet's today.
 *  The grace days (whole days; blank means off): when the sheet's today changes, every open period whose
 *  to-date plus them is before the new today is closed. A grade's grace days (grace-<grade>, whole days;
 *  blank means none): a claim whose person's grade in force on its date has them in force on that date is
 *  overdue only once its due-by plus them is before the sheet's today, and its late charge's 30-day
 *  boundaries count from there; the due-by it shows is unchanged. The parent (a sheet id; blank means none;
 *  not dated): a grade with no rate in force on a date on this sheet takes its parent's, then its parent's
 *  parent's, up to three levels up; refused for the sheet itself, a sheet not loaded yet, or one below it.
 *  The fiscal year start month (1 to 12; 1 until set; not dated): a fiscal year runs twelve months from the
 *  first of it and is named FY<yyyy> by the calendar year its first day falls in (see `fiscalYearOf`).
 *  The alert threshold (two decimals; blank means off; not dated): a person whose due-total goes from at
 *  or below it to above it after a change is alerted (an ALERT audit line).
 *  The rounding mode (half-up or half-even; blank means none; dated): a claim whose round date has one in
 *  force rounds its live figures by it instead of by the policy date.
 *  The escalation gap (a number of days; blank means off; not dated): reminding a claim whose previous
 *  reminder was at least that many days before the sheet's today escalates it (an ESCALATE audit line),
 *  once per claim ever.
 *  The review SLA (a number of days; blank means none; not dated): a REVIEW claim whose review day (the
 *  sheet's today when it went to REVIEW) plus them is before the sheet's today is review-late (see `isReviewLate`).
 *  The default grade (A, B or C; blank means A; not dated): the grade the quick-add bar gives a new person,
 *  and an import line with a blank grade; changing it changes no one. */
export const SETTING_KEYS = ['clerk-limit', 'cap', 'pot', 'alert-threshold', 'floor', 'terms', 'terms-A', 'terms-B', 'terms-C', 'late-pct', 'policy-date', 'rounding',
  'period-numbering-from', 'entry-date', 'grace-days', 'escalate-after', 'review-sla', 'grace-A', 'grace-B', 'grace-C', 'proof-threshold', 'proof-threshold-A', 'proof-threshold-B', 'proof-threshold-C', 'proof-kinds', 'note-threshold', 'review-threshold', 'perm-open', 'perm-settle', 'perm-void', 'perm-reassign', 'perm-close', 'perm-reopen', 'parent', 'fiscal-start-month', 'default-grade'] as const;

/** The roles that may take each action of the permission matrix while its entry is blank. */
export const PERMISSION_STANDING: Record<string, string> = {
  open: 'clerk,steward', settle: 'clerk,steward', void: 'clerk,steward', close: 'clerk,steward',
  reassign: 'steward', reopen: 'steward',
};

/** A setting's value until it is first set. */
export const SETTING_DEFAULTS: Record<string, string> = { floor: '1.00', terms: '14', 'late-pct': '0.00', 'fiscal-start-month': '1' };

/** The settings that are not dated: every other one is saved from a from-date (blank: the sheet's today). */
export const UNDATED_KEYS: readonly string[] = ['pot', 'proof-kinds', 'alert-threshold', 'policy-date', 'period-numbering-from', 'entry-date', 'grace-days', 'escalate-after', 'review-sla', 'perm-open', 'perm-settle', 'perm-void',
  'perm-reassign', 'perm-close', 'perm-reopen', 'parent', 'fiscal-start-month', 'default-grade'];

/** The fiscal year a date (yyyy-MM-dd) falls in, FY<yyyy>, for a fiscal year start month (1 to 12). */
export function fiscalYearOf(date: string, startMonth: number): string {
  const year = Number(date.slice(0, 4));
  return `FY${Number(date.slice(5, 7)) < startMonth ? year - 1 : year}`;
}

/** The sheet's fiscal year start month (1 to 12). */
export function useFiscalStartMonth(sheetId: string): number {
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  return Number(settingOn(settings.data, 'fiscal-start-month', '9999-12-31') || SETTING_DEFAULTS['fiscal-start-month']);
}

/** A dated setting's value in force on a date (yyyy-MM-dd). */
export type DatedOn = (date: string) => string;

/** A setting's value in force on a date (yyyy-MM-dd): the one with the latest from-date on or before
 *  it; else its starting value (SETTING_DEFAULTS), and none (undefined) for a key with none. */
export function settingOn(settings: Setting[] | undefined, key: string, date: string): string | undefined {
  let found: Setting | undefined;
  for (const s of settings ?? []) {
    if (s.key !== key || (s.from !== null && s.from > date)) continue;
    if (found === undefined || (found.from ?? '') < (s.from ?? '')) found = s;
  }
  return found === undefined ? SETTING_DEFAULTS[key] : found.value;
}

const base = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

export const settingsKey = (sheetId: string) => [...sheetKey(sheetId), 'settings'] as const;
export const rolesKey = (sheetId: string) => [...sheetKey(sheetId), 'roles'] as const;

export function listSettings(sheetId: string, live = false): Promise<Setting[]> {
  return getJson<Setting[]>(`${base(sheetId)}/settings`, live);
}

/** Change a setting; refused (rejects) when the value is not valid for it. */
export function changeSetting(sheetId: string, by: string, key: string, value: string, from: string): Promise<Setting> {
  return postJson<Setting>(`${base(sheetId)}/settings?by=${encodeURIComponent(by)}`, { key, value, from });
}

export function listRoles(sheetId: string): Promise<Role[]> {
  return getJson<Role[]>(`${base(sheetId)}/roles`);
}

/** The role the user acts in on the sheet on its today: their own, or steward while a delegation to
 *  them covers it (as the sheet stood then while it is shown as of a date). */
export function roleOf(sheetId: string, user: string): Promise<Role> {
  return getJson<Role>(`${base(sheetId)}/role?user=${encodeURIComponent(user)}`);
}

export const roleOfKey = (sheetId: string, user: string) => [...rolesKey(sheetId), 'of', user] as const;

/** Assign a user name a role; refused (rejects) when the role is not clerk or steward. */
export function setRole(sheetId: string, by: string, user: string, role: string): Promise<Role> {
  return postJson<Role>(`${base(sheetId)}/roles?by=${encodeURIComponent(by)}`, { user, role });
}

/** The sheet's floor by date: the least a claim's live due can be, the one in force on its date. */
export function useFloor(sheetId: string, live = false): DatedOn {
  const settings = useQuery({ queryKey: liveKey(settingsKey(sheetId), live), queryFn: () => listSettings(sheetId, live) });
  return (date) => settingOn(settings.data, 'floor', date) || SETTING_DEFAULTS.floor;
}

/** The proof threshold of a grade by date: the grade's own in force on that date when set, else the
 *  sheet's (null: none in force on that date). */
export function useProofThreshold(sheetId: string): (grade: string, date: string) => string | null {
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  return (grade, date) => (grade ? settingOn(settings.data, `proof-threshold-${grade}`, date) : '')
    || settingOn(settings.data, 'proof-threshold', date) || null;
}

/** The sheet's proof kinds: those a claim above the proof threshold needs a proof of each of (none: not set). */
export function useProofKinds(sheetId: string): string[] {
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  const value = settingOn(settings.data, 'proof-kinds', '9999-12-31') ?? '';
  return value ? value.split(',') : [];
}

/** The sheet's note threshold by date (null: none in force on that date). */
export function useNoteThreshold(sheetId: string): (date: string) => string | null {
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  return (date) => settingOn(settings.data, 'note-threshold', date) || null;
}

/** The sheet's review SLA in whole days (null: none). */
export function useReviewSla(sheetId: string): number | null {
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  const value = settingOn(settings.data, 'review-sla', '9999-12-31');
  return value ? Number(value) : null;
}

/** The day a claim last went to REVIEW (yyyy-MM-dd), from its status log (null: never). */
export function reviewDay(c: Pick<Claim, 'log'>): string | null {
  for (let i = (c.log ?? []).length - 1; i >= 0; i--) {
    const m = /^REVIEW by .* on (\d{4}-\d{2}-\d{2}) due=/.exec(c.log[i]);
    if (m) return m[1];
  }
  return null;
}

/** Whether a claim is review-late: REVIEW, and its review day plus the review SLA (null: none) is
 *  before the sheet's today. */
export function isReviewLate(c: Pick<Claim, 'status' | 'log'>, today: string, sla: number | null): boolean {
  if (c.status !== 'REVIEW' || sla === null) return false;
  const day = reviewDay(c);
  if (day === null) return false;
  const until = new Date(Date.parse(`${day}T00:00:00Z`) + sla * 86_400_000).toISOString().slice(0, 10);
  return until < today;
}

/** The sheet's cap in force on its today: the most a person's due-total can be; null when there is none. */
export function useCap(sheetId: string, today: string, live = false): string | null {
  const settings = useQuery({ queryKey: liveKey(settingsKey(sheetId), live), queryFn: () => listSettings(sheetId, live) });
  return settingOn(settings.data, 'cap', today) || null;
}

/** What a claim's late charge and rounding go by: the sheet's late percent by date (a late charge goes
 *  by the one in force on each 30-day boundary), today and policy date (its today when blank), and the
 *  date a claim's lateness counts from: its due-by plus the grace days (grace-<grade>) of its person's
 *  grade in force on its date, as in force on that date (blank: none). */
export function useLate(sheetId: string, today: string, live = false): Late {
  const settings = useQuery({ queryKey: liveKey(settingsKey(sheetId), live), queryFn: () => listSettings(sheetId, live) });
  const people = useQuery({ queryKey: liveKey(peopleKey(sheetId), live), queryFn: () => listPeople(sheetId, live) });
  const pct = (date: string) => settingOn(settings.data, 'late-pct', date) || SETTING_DEFAULTS['late-pct'];
  const policy = settingOn(settings.data, 'policy-date', today) || today;
  const lateFrom = (c: Claim) => {
    const person = people.data?.find((p) => p.id === c.personId);
    const grace = person === undefined ? '' : settingOn(settings.data, `grace-${gradeOn(person, c.date)}`, c.date) ?? '';
    if (grace === '') return dueBy(c);
    const from = new Date(Date.parse(`${dueBy(c)}T00:00:00Z`) + Number(grace) * 86_400_000);
    return from.toISOString().slice(0, 10);
  };
  const rounding = (date: string) => settingOn(settings.data, 'rounding', date) ?? '';
  return { pct, today, policy, lateFrom, rounding };
}

/** A steward's delegation of the steward role to a user, from a date to a date (yyyy-MM-dd, inclusive):
 *  while the sheet's today is inside it, the user acts as a steward. */
export type Delegation = { id: number; sheetId: string; user: string; from: string; to: string; by: string };

export const delegationsKey = (sheetId: string) => [...sheetKey(sheetId), 'delegations'] as const;

/** The sheet's delegations, newest first. */
export function listDelegations(sheetId: string): Promise<Delegation[]> {
  return getJson<Delegation[]>(`${base(sheetId)}/delegations`);
}

/** Delegate the steward role (a blank date: the sheet's today); refused (rejects) for anyone but a
 *  steward, or a to-date before the from-date. */
export function delegate(sheetId: string, by: string, user: string, from: string, to: string): Promise<Delegation> {
  return postJson<Delegation>(`${base(sheetId)}/delegations?by=${encodeURIComponent(by)}`, { user, from, to });
}

/** A holiday on the sheet (yyyy-MM-dd): a claim's due-by, fixed when it is created, that falls on one
 *  moves to the next day that is not one. */
export type Holiday = { sheetId: string; date: string; by: string };

export const holidaysKey = (sheetId: string) => [...sheetKey(sheetId), 'holidays'] as const;

/** The sheet's holidays, by date. */
export function listHolidays(sheetId: string): Promise<Holiday[]> {
  return getJson<Holiday[]>(`${base(sheetId)}/holidays`);
}

/** Add a holiday; refused (rejects) for a date that is not one, or one already on the list. */
export function addHoliday(sheetId: string, by: string, date: string): Promise<Holiday> {
  return postJson<Holiday>(`${base(sheetId)}/holidays?by=${encodeURIComponent(by)}`, { date });
}
