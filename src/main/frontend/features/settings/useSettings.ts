import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useSheetToday } from '../sheet/useSheetToday';

/** A role a user name holds on the sheet: clerk (any name not listed) or steward. */
export type Role = { user: string; role: string };

/** The sheet's settings, each dated one as in force on the sheet's today: the clerk limit with two decimals ("" for no limit), the floor with two
 *  decimals (a claim's live due is at least it), the terms in days (a new claim's due-by is its date
 *  plus them, unless its person's grade has its own terms), the late percent with two decimals (an overdue claim gains it of its figure for every
 *  full 30 days overdue), the policy date ("" for blank: the sheet's today), the period numbering from ("" for off: from it,
 *  a claim opened is numbered within its period), the roles set and each grade's own terms in days, by grade
 *  (a grade not listed uses the sheet's terms), and the cap with two decimals ("" for none: a person's due-total is at most it). */
export type Settings = { clerkLimit: string; floor: string; terms: string; latePct: string; policyDate: string; periodNumberingFrom: string; roles: Role[]; gradeTerms: Record<string, string>; cap: string; dated: Record<string, Dated[]> };

/** A value recorded for a dated setting (clerk-limit, floor, terms, terms-<grade>, late-pct, cap): the date
 *  it is in force from and the value ("" for none). The figures shown in Settings are those in force on the
 *  sheet's today; these are every value recorded, oldest first. */
export type Dated = { from: string; value: string };

/** The value of a dated setting in force on a date: the one with the latest from-date on or before it,
 *  else the sheet's starting value. */
export function inForce(settings: Settings | undefined, key: string, date: string, start: string): string {
  const before = (settings?.dated?.[key] ?? []).filter((d) => d.from <= date);
  const found = before[before.length - 1];
  return found === undefined || found.value === '' ? start : found.value;
}

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

/** The sheet's floor in force on a date (a claim's: its date). */
export function useFloor(sheet: string): (date: string) => string {
  const settings = useSettings(sheet);
  return (date) => inForce(settings, 'floor', date, DEFAULT_FLOOR);
}

/** The late percent a sheet starts with, used until its settings have loaded. */
export const DEFAULT_LATE_PCT = '0.00';

/** The sheet's late percent in force on a date (a claim's: its date). */
export function useLatePct(sheet: string): (date: string) => string {
  const settings = useSettings(sheet);
  return (date) => inForce(settings, 'late-pct', date, DEFAULT_LATE_PCT);
}

/** The sheet's policy date: the one set, or the sheet's today when it is blank. A claim dated before
 *  it rounds half up, one on or after it half to even. Undefined while loading. */
export function usePolicyDate(sheet: string): string | undefined {
  const set = useSettings(sheet)?.policyDate;
  const today = useSheetToday(sheet);
  return set ? set : today;
}

/** The sheet's cap in force on its today ("" for none): a person's due-total is at most it. */
export function useCap(sheet: string): string {
  return useSettings(sheet)?.cap ?? '';
}
