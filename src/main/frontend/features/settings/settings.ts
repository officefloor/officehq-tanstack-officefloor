import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { hundredths } from '../rates/rates';

// A sheet's settings: the clerk limit (two decimals; null: no limit). And the role each user name has
// on the sheet: clerk (any name not given one) or steward. A clerk settles only a claim whose due is
// at or below the clerk limit; a steward settles any.
export type Settings = { clerkLimit: string | null };
export type Role = 'clerk' | 'steward';
export type UserRole = { user: string; role: Role };

export const ROLES: Role[] = ['clerk', 'steward'];

/** Empty (no limit), or 0.00 or more with up to two decimals. */
export const LIMIT = /^(\d{1,12}(\.\d{1,2})?)?$/;

const base = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}`;
export const settingsKey = (sheet: string) => ['sheet', sheet, 'settings'] as const;
export const rolesKey = (sheet: string) => ['sheet', sheet, 'roles'] as const;

/** The active sheet's settings. */
export function useSettings() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: settingsKey(sheet ?? ''),
    queryFn: () => getJson<Settings>(`${base(sheet!)}/settings`),
    enabled: !!sheet,
  });
}

/** The roles assigned on the active sheet, by user name. */
export function useRoles() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: rolesKey(sheet ?? ''),
    queryFn: () => getJson<{ roles: UserRole[] }>(`${base(sheet!)}/roles`).then((r) => r.roles),
    enabled: !!sheet,
  });
}

export const saveSettings = (sheet: string, user: string, settings: { clerkLimit: string }) =>
  postJson<Settings>(`${base(sheet)}/settings`, settings, user);

export const setRole = (sheet: string, user: string, input: { user: string; role: string }) =>
  postJson<UserRole>(`${base(sheet)}/roles`, input, user);

/** Whether the user may settle a claim with this due (cents; undefined for n/a, which is within any limit). */
export function useWithinClerkLimit() {
  const limit = useSettings().data?.clerkLimit ?? null;
  const roles = useRoles().data ?? [];
  return (user: string, due: bigint | undefined) => {
    const role = roles.find((r) => r.user === user.trim())?.role ?? 'clerk';
    return role === 'steward' || limit === null || due === undefined || due <= hundredths(limit);
  };
}
