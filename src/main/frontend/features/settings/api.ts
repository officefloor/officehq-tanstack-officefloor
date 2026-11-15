import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';
import type { Late } from '../rates/api';

/** A value recorded for a sheet setting: its value as text ("" when blank), in force from its
 *  from-date (yyyy-MM-dd; null: from the beginning of time, and always for an undated key). A key
 *  may have several, one per from-date; a setting never set is not listed. */
export type Setting = { sheetId: string; key: string; value: string; from: string | null };
/** The role a user name has on the sheet; a name not listed is a clerk. */
export type Role = { sheetId: string; user: string; role: string };

/** The settings the screen offers: the clerk limit (two decimals; blank means no limit), the cap
 *  (two decimals; blank means none: the most a person's due-total can be), the
 *  floor (two decimals; the least a claim's live due can be) and the terms (whole days after its
 *  date by which a claim is due) and the late percent (two decimals; the share of an overdue claim's
 *  figure it gains for every full 30 days it is overdue) and the policy date (yyyy-MM-dd; blank means
 *  the sheet's today: a claim dated before it rounds half up, on or after it half to even) and the
 *  period numbering date (yyyy-MM-dd; blank means off: once the sheet's today is on or after it, a
 *  claim opened is numbered within its period, <period>-<n>, or X-<n> in no period). A grade's own terms
 *  (terms-<grade>; blank means the sheet's terms) fix the due-by of a claim added for a person of that
 *  grade on its date: its date plus those terms, kept when the terms or its date change later. The proof
 *  threshold (two decimals; blank means none): a claim whose due is above the one in force on its date
 *  cannot be settled or approved while it has no proofs. The permission matrix (perm-<action>, a comma
 *  separated list of roles; blank means the standing rule, PERMISSION_STANDING): who may take each action.
 *  The grace days (whole days; blank means off): when the sheet's today changes, every open period whose
 *  to-date plus them is before the new today is closed. */
export const SETTING_KEYS = ['clerk-limit', 'cap', 'floor', 'terms', 'terms-A', 'terms-B', 'terms-C', 'late-pct', 'policy-date',
  'period-numbering-from', 'grace-days', 'proof-threshold', 'perm-open', 'perm-settle', 'perm-void', 'perm-reassign', 'perm-close', 'perm-reopen'] as const;

/** The roles that may take each action of the permission matrix while its entry is blank. */
export const PERMISSION_STANDING: Record<string, string> = {
  open: 'clerk,steward', settle: 'clerk,steward', void: 'clerk,steward', close: 'clerk,steward',
  reassign: 'steward', reopen: 'steward',
};

/** A setting's value until it is first set. */
export const SETTING_DEFAULTS: Record<string, string> = { floor: '1.00', terms: '14', 'late-pct': '0.00' };

/** The settings that are not dated: every other one is saved from a from-date (blank: the sheet's today). */
export const UNDATED_KEYS: readonly string[] = ['policy-date', 'period-numbering-from', 'grace-days', 'perm-open', 'perm-settle', 'perm-void',
  'perm-reassign', 'perm-close', 'perm-reopen'];

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

export function listSettings(sheetId: string): Promise<Setting[]> {
  return getJson<Setting[]>(`${base(sheetId)}/settings`);
}

/** Change a setting; refused (rejects) when the value is not valid for it. */
export function changeSetting(sheetId: string, by: string, key: string, value: string, from: string): Promise<Setting> {
  return postJson<Setting>(`${base(sheetId)}/settings?by=${encodeURIComponent(by)}`, { key, value, from });
}

export function listRoles(sheetId: string): Promise<Role[]> {
  return getJson<Role[]>(`${base(sheetId)}/roles`);
}

/** Assign a user name a role; refused (rejects) when the role is not clerk or steward. */
export function setRole(sheetId: string, by: string, user: string, role: string): Promise<Role> {
  return postJson<Role>(`${base(sheetId)}/roles?by=${encodeURIComponent(by)}`, { user, role });
}

/** The sheet's floor by date: the least a claim's live due can be, the one in force on its date. */
export function useFloor(sheetId: string): DatedOn {
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  return (date) => settingOn(settings.data, 'floor', date) || SETTING_DEFAULTS.floor;
}

/** The sheet's proof threshold by date (null: none in force on that date). */
export function useProofThreshold(sheetId: string): (date: string) => string | null {
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  return (date) => settingOn(settings.data, 'proof-threshold', date) || null;
}

/** The sheet's cap in force on its today: the most a person's due-total can be; null when there is none. */
export function useCap(sheetId: string, today: string): string | null {
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  return settingOn(settings.data, 'cap', today) || null;
}

/** What a claim's late charge and rounding go by: the sheet's late percent by date (a late charge goes
 *  by the one in force on each 30-day boundary), today and policy date (its today when blank). */
export function useLate(sheetId: string, today: string): Late {
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  const pct = (date: string) => settingOn(settings.data, 'late-pct', date) || SETTING_DEFAULTS['late-pct'];
  const policy = settingOn(settings.data, 'policy-date', today) || today;
  return { pct, today, policy };
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
