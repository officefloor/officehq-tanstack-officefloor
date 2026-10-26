import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { toHundredths, type Late } from '../rates/useRates';
import { useSheetToday } from '../today/today.slot';
import { useClaims } from '../claims/useClaims';

/** A role assigned to a user name: clerk (any name without one), steward or viewer. */
export type Role = { user: string; role: string };

/** A sheet setting: its value in force on the sheet's today (null: blank) and, for a dated one, every value recorded
 *  by from-date (yyyy-mm-dd), each in force until the next one's. */
export type Setting = { key: string; value: string | null; dated: { from: string; value: string | null }[] };

/** A dated setting's value in force on a date (yyyy-mm-dd): the one with the latest from-date on or before it; before
 *  every from-date (or when blank), `starting`. */
export function settingOn(setting: Setting | undefined, date: string, starting: string): string {
  let found: { from: string; value: string | null } | undefined;
  for (const d of setting?.dated ?? []) {
    // ISO dates compare as text.
    if (d.from <= date && (found === undefined || d.from > found.from)) found = d;
  }
  return found?.value ?? starting;
}

/** A steward's (`by`) delegation of the steward role to a user, from a date to a date (yyyy-mm-dd), both inclusive. */
export type Delegation = { user: string; from: string; to: string; by: string };

export const delegationsKey = (sheetId: string) => ['sheet', sheetId, 'delegations'] as const;

/** The sheet's delegations of the steward role, in the order they were made. */
export function useDelegations(sheetId: string) {
  return useQuery({
    queryKey: delegationsKey(sheetId),
    queryFn: () => getJson<Delegation[]>(`/api/sheets/${encodeURIComponent(sheetId)}/delegations`),
  });
}

export const rolesKey = (sheetId: string) => ['sheet', sheetId, 'roles'] as const;
export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'] as const;

/** The sheet's assigned roles, by user name. */
export function useRoles(sheetId: string) {
  return useQuery({
    queryKey: rolesKey(sheetId),
    queryFn: () => getJson<Role[]>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`),
  });
}

/** The floor a sheet starts with, until it is set. */
export const FLOOR_DEFAULT = '1.00';

/** The sheet's settings, by key. */
export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Setting[]>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}

/** The sheet's floor in cents in force on a date: the least a claim's live due may be (1.00 before any is in force). */
export type FloorOn = (date: string) => bigint;

/** The sheet's floor in cents, by the date it is in force on. */
export function useFloorCents(sheetId: string): FloorOn {
  const floor = useSettings(sheetId).data?.find((s) => s.key === 'floor');
  return (date) => toHundredths(settingOn(floor, date, FLOOR_DEFAULT));
}

/** The sheet's cap in cents in force on its today: the most a person's due-total may be; null while blank (no cap). */
export function useCapCents(sheetId: string): bigint | null {
  const value = useSettings(sheetId).data?.find((s) => s.key === 'cap')?.value;
  return value == null || value === '' ? null : toHundredths(value);
}

/** The terms a sheet starts with, until they are set: the days after its date a claim is due by. */
export const TERMS_DEFAULT = '14';

/** The late percent a sheet starts with, until it is set. */
export const LATE_PCT_DEFAULT = '0.00';

/** The sheet's late charge as of its today: its late percent in force on each date (0.00 before any is), and each
 *  grade's grace in force on a date (0 while blank); with its
 *  rounding mode in force on each date (none while blank), the
 *  policy date (its today while blank) and the numbered claims' dates, which decide how each claim rounds. Undefined
 *  until the sheet's today is known. */
export function useLate(sheetId: string): Late | undefined {
  const settings = useSettings(sheetId).data;
  const pct = settings?.find((s) => s.key === 'late-pct');
  const policy = settings?.find((s) => s.key === 'policy-date')?.value ?? '';
  const rounding = settings?.find((s) => s.key === 'rounding');
  const today = useSheetToday(sheetId).data?.today;
  const claims = useClaims(sheetId).data;
  const dateOfNumber = new Map<string, string>();
  for (const c of claims ?? []) if (c.number != null) dateOfNumber.set(c.number, c.date);
  const graceOn = (grade: string | undefined, date: string) => {
    const value = grade === undefined ? '' : settingOn(settings?.find((s) => s.key === `grace-${grade}`), date, '');
    return value === '' ? 0 : Number(value);
  };
  return today === undefined ? undefined
    : { today, pctOn: (date) => toHundredths(settingOn(pct, date, LATE_PCT_DEFAULT)), graceOn,
      policyDate: policy === '' ? today : policy, roundingOn: (date) => settingOn(rounding, date, ''), dateOfNumber };
}
