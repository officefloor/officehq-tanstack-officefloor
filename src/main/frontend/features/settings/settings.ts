import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { hundredths } from '../rates/rates';

// A sheet's settings: the clerk limit (two decimals; null: no limit). And the role each user name has
// on the sheet: clerk (any name not given one) or steward. A clerk settles only a claim whose due is
// at or below the clerk limit; a steward settles any. The floor (two decimals, 1.00 until set): a
// claim's live due is at least it. The terms (whole days, 14 until set): a claim is due by its date plus them.
// The late percent (two decimals, 0.00 until set): an overdue claim's live due gains it of its floored due for
// every full 30 days it is overdue.
export type Settings = { clerkLimit: string | null; floor: string; terms: string; latePct: string };
export type Role = 'clerk' | 'steward';
export type UserRole = { user: string; role: Role };

/** Empty (unchanged), or a whole number of days, 0 or more. */
export const TERMS = /^(\d{1,4})?$/;

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

export const saveSettings = (sheet: string, user: string, settings: { clerkLimit: string; floor: string; terms: string; latePct: string }) =>
  postJson<Settings>(`${base(sheet)}/settings`, settings, user);

export const setRole = (sheet: string, user: string, input: { user: string; role: string }) =>
  postJson<UserRole>(`${base(sheet)}/roles`, input, user);

/** The floor in force before the sheet's settings load or any is set. */
export const DEFAULT_FLOOR = '1.00';

/** The active sheet's floor in cents: a claim's live due is at least it. */
export function useFloor(): bigint {
  return hundredths(useSettings().data?.floor ?? DEFAULT_FLOOR);
}

/** The terms in force before the sheet's settings load or any is set. */
export const DEFAULT_TERMS = '14';

/** The active sheet's terms in days: a claim is due by its date plus them. */
export function useTerms(): number {
  return Number(useSettings().data?.terms ?? DEFAULT_TERMS);
}

/** Whether the user may settle a claim with this due (cents; undefined for n/a, which is within any limit). */
export function useWithinClerkLimit() {
  const limit = useSettings().data?.clerkLimit ?? null;
  const roles = useRoles().data ?? [];
  return (user: string, due: bigint | undefined) => {
    const role = roles.find((r) => r.user === user.trim())?.role ?? 'clerk';
    return role === 'steward' || limit === null || due === undefined || due <= hundredths(limit);
  };
}

/** The late percent in force before the sheet's settings load or any is set. */
export const DEFAULT_LATE_PCT = '0.00';

/** The active sheet's late percent in hundredths of a percent. */
export function useLatePct(): bigint {
  return hundredths(useSettings().data?.latePct ?? DEFAULT_LATE_PCT);
}
