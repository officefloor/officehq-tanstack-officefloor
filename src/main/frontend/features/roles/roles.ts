import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A user name's role on the sheet: clerk or steward. A name with none is a clerk. */
export type Role = { user: string; role: string };

/** The sheet's settings: its clerk limit with two decimals (null when there is none) and its floor with two
 *  decimals (a claim's live due is at least it), and its terms in days (a claim is due by its date plus them), and its late percent with two decimals (an overdue claim's due gains
 *  it of its figure for every full 30 days overdue), and its policy date (YYYY-MM-DD; "" when blank: the sheet's today — a claim
 *  dated before it rounds half up, one on or after it half to even). */
export type Settings = { clerkLimit: string | null; floor: string; terms: string;
  latePct: string; policyDate: string };

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
