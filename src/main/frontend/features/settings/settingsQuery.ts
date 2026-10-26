import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import type { Late } from '../rates/ratesQuery';
import { claimsQuery } from '../claims/claimsQuery';

/** `clerkLimit` has two decimals ("1.50"), or is "" when there is no limit; `floor` has two decimals;
 *  `terms` is a whole number of days ("14"); `latePct` is a percent with two decimals ("0.00");
 *  `policyDate` is yyyy-mm-dd, or "" when there is none (the sheet's today is the policy date); `periodNumberingFrom` is yyyy-mm-dd, or "" when numbering by period is off. */
export type Settings = { clerkLimit: string; floor: string; terms: string; latePct: string; policyDate: string; periodNumberingFrom: string };

/** What the request carries: a setting left out (or blank, for the floor, terms and late percent) is left as it is; a blank policy date is none. */
export type SettingsChange = { clerkLimit?: string; floor?: string; terms?: string; latePct?: string; policyDate?: string; periodNumberingFrom?: string };

/** The floor a sheet starts with. */
export const DEFAULT_FLOOR = '1.00';

/** The terms a sheet starts with, in days. */
export const DEFAULT_TERMS = '14';

/** The late percent a sheet starts with. */
export const DEFAULT_LATE_PCT = '0.00';

export type RoleName = 'clerk' | 'steward';

/** The role a sheet gives a user name; a name with no entry is a clerk. */
export type Role = { user: string; role: RoleName };

export const ROLES: RoleName[] = ['clerk', 'steward'];

/** Under the sheet's key, so re-loading the sheet re-reads its settings and roles. */
export const settingsKey = (sheetId: string) => [...sheetKey(sheetId), 'settings'] as const;
export const rolesKey = (sheetId: string) => [...sheetKey(sheetId), 'roles'] as const;

export const settingsQuery = (sheetId: string) => ({
  queryKey: settingsKey(sheetId),
  queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
});

/** The sheet's floor: what a claim's live due is at least. */
export function useFloor(sheetId: string): string {
  return useQuery(settingsQuery(sheetId)).data?.floor ?? DEFAULT_FLOOR;
}

/** The sheet's terms in days: how long after its date a claim is due by. */
export function useTerms(sheetId: string): number {
  return Number(useQuery(settingsQuery(sheetId)).data?.terms ?? DEFAULT_TERMS);
}

/** The sheet's late percent: what an overdue claim gains of its figure for every full 30 days it is overdue. */
export function useLatePct(sheetId: string): string {
  return useQuery(settingsQuery(sheetId)).data?.latePct ?? DEFAULT_LATE_PCT;
}

/** The sheet's policy date (yyyy-mm-dd): the one set, or the sheet's today when there is none. */
export function usePolicyDate(sheetId: string): string {
  const today = useSheetToday(sheetId);
  return useQuery(settingsQuery(sheetId)).data?.policyDate || today;
}

/** What a claim's late charge and its rounding depend on, as the sheet has it now. */
export function useLate(sheetId: string): Late {
  return {
    today: useSheetToday(sheetId),
    terms: useTerms(sheetId),
    pct: useLatePct(sheetId),
    policy: usePolicyDate(sheetId),
    claims: useQuery(claimsQuery(sheetId)).data ?? [],
  };
}

export const rolesQuery = (sheetId: string) => ({
  queryKey: rolesKey(sheetId),
  queryFn: () => getJson<Role[]>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`),
});
