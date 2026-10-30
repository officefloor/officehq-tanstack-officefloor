import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useSheetToday } from '../sheet/useSheetToday';

/** A role a user name holds on the sheet: clerk (any name not listed) or steward. */
export type Role = { user: string; role: string };

/** The sheet's settings: the clerk limit with two decimals ("" for no limit), the floor with two
 *  decimals (a claim's live due is at least it), the terms in days (a new claim's due-by is its date
 *  plus them, unless its person's grade has its own terms), the late percent with two decimals (an overdue claim gains it of its figure for every
 *  full 30 days overdue), the policy date ("" for blank: the sheet's today), the period numbering from ("" for off: from it,
 *  a claim opened is numbered within its period), the roles set and each grade's own terms in days, by grade
 *  (a grade not listed uses the sheet's terms), and the cap with two decimals ("" for none: a person's due-total is at most it). */
export type Settings = { clerkLimit: string; floor: string; terms: string; latePct: string; policyDate: string; periodNumberingFrom: string; roles: Role[]; gradeTerms: Record<string, string>; cap: string };

/** The floor a sheet starts with, used until its settings have loaded. */
export const DEFAULT_FLOOR = '1.00';

export const settingsKey = (sheet: string) => ['sheet', sheet, 'settings'] as const;

export function useSettings(sheet: string): Settings | undefined {
  const { data } = useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`),
    enabled: sheet !== '',
  });
  return data;
}

/** The sheet's floor. */
export function useFloor(sheet: string): string {
  return useSettings(sheet)?.floor ?? DEFAULT_FLOOR;
}

/** The late percent a sheet starts with, used until its settings have loaded. */
export const DEFAULT_LATE_PCT = '0.00';

/** The sheet's late percent. */
export function useLatePct(sheet: string): string {
  return useSettings(sheet)?.latePct ?? DEFAULT_LATE_PCT;
}

/** The sheet's policy date: the one set, or the sheet's today when it is blank. A claim dated before
 *  it rounds half up, one on or after it half to even. Undefined while loading. */
export function usePolicyDate(sheet: string): string | undefined {
  const set = useSettings(sheet)?.policyDate;
  const today = useSheetToday(sheet);
  return set ? set : today;
}

/** The sheet's cap ("" for none): a person's due-total is at most it. */
export function useCap(sheet: string): string {
  return useSettings(sheet)?.cap ?? '';
}
