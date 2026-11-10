import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Late } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';

/**
 * A sheet's settings: its clerk limit, with two decimals ('' for no limit), and its floor, the
 * least a claim's live due may be, with two decimals, and its terms, the whole number of days after a
 * claim's date that it is due by, and its late percent, what an overdue claim's due gains per full 30
 * days overdue, with two decimals, and its policy date (yyyy-MM-dd; '' for the sheet's today): a claim
 * dated before it rounds half up, one on or after it half to even, and its period numbering from
 * (yyyy-MM-dd; '' for off): while the sheet's today is on or after it, a claim opened is numbered
 * within its period.
 */
export type Settings = {
  clerkLimit: string;
  floor: string;
  terms: string;
  latePct: string;
  policyDate: string;
  periodNumberingFrom: string;
  /** Each grade's own terms in days ('' for the sheet's terms). */
  gradeTerms: Record<string, string>;
};

/** The grades that may have their own terms. */
export const GRADES = ['A', 'B', 'C'] as const;

/** A save of the settings: only those being changed are sent. */
export type SettingsChange = Partial<Settings>;

/** A user's role on a sheet. A name with no role assigned is a clerk. */
export type Role = { user: string; role: string };

export const ROLES = ['clerk', 'steward'] as const;

export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'] as const;
export const rolesKey = (sheetId: string) => ['sheet', sheetId, 'roles'] as const;

export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}

/** The sheet's floor ('0.00', no floor, until the settings are read). */
export function useFloor(sheetId: string): string {
  return useSettings(sheetId).data?.floor ?? '0.00';
}

/**
 * What the sheet's late charge and rounding need: its late percent ('0.00' until read),
 * today and policy date ('' until read).
 */
export function useLate(sheetId: string): Late {
  const settings = useSettings(sheetId).data;
  const pct = settings?.latePct ?? '0.00';
  return { pct, today: useSheetToday(sheetId), policyDate: settings?.policyDate ?? '' };
}

/** The roles assigned on the sheet, by user name. */
export function useRoles(sheetId: string) {
  return useQuery({
    queryKey: rolesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ roles: Role[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`)).roles,
  });
}

/** A clerk limit as the screen takes it: blank, or 0.00 or more with up to two decimals. */
export const LIMIT = /^\d+(\.\d{1,2})?$/;

/** A floor as the screen takes it: 0.00 or more with up to two decimals. */
export const FLOOR = /^\d+(\.\d{1,2})?$/;

/** A late percent as the screen takes it: 0.00 or more with up to two decimals. */
export const LATE_PCT = /^\d+(\.\d{1,2})?$/;

/** A policy date as the screen takes it: blank, or a date yyyy-MM-dd. */
export const POLICY_DATE = /^\d{4}-\d{2}-\d{2}$/;

/** Terms as the screen takes them: a whole number of days, 0 or more. */
export const TERMS = /^\d{1,5}$/;
