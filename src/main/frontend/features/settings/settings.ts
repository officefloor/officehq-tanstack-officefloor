import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Late } from '../overdue/late';
import { useSheetToday } from '../session/session';

/** A sheet setting: `value` is text (clerk-limit, cap, floor: an amount with two decimals; terms, terms-A, terms-B, terms-C: a whole number of days; late-pct: a percent with two decimals; policy-date, period-numbering-from: yyyy-MM-dd), null when blank.
 *  `from` is the date the value is in force from (yyyy-MM-dd); null when undated (policy-date, period-numbering-from, or a
 *  value from before settings were dated: in force on every date). */
export type Setting = { sheetId: string; key: string; value: string | null; from: string | null };

export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'];

/** The sheet's settings: every value recorded, by key then from-date. */
export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Setting[]>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}

/** The setting recorded for the key in force on the date (the latest from-date on or before it; an undated one is in
 *  force on every date); undefined when none is. */
export function inForce(settings: Setting[] | undefined, key: string, date: string): Setting | undefined {
  let found: Setting | undefined;
  for (const s of settings ?? []) {
    if (s.key !== key || (s.from !== null && s.from > date)) continue;
    if (found === undefined || (s.from !== null && (found.from === null || s.from > found.from))) found = s;
  }
  return found;
}

/** A dated figure: its value in force on a date (yyyy-MM-dd). */
export type Dated = (date: string) => string;

/** The floor a sheet has until one is set. */
export const DEFAULT_FLOOR = '1.00';

/** The sheet's floor: the least a claim's live due may be (two decimals), the one in force on the claim's date. */
export function useFloor(sheetId: string): Dated {
  const settings = useSettings(sheetId);
  return (date) => inForce(settings.data, 'floor', date)?.value ?? DEFAULT_FLOOR;
}

/** The sheet's cap in force on its today: the most a person's due-total may be (two decimals); null when blank (none). */
export function useCap(sheetId: string): string | null {
  const settings = useSettings(sheetId);
  const today = useSheetToday(sheetId);
  return inForce(settings.data, 'cap', today)?.value ?? null;
}

/** The terms a sheet has until they are set. */
export const DEFAULT_TERMS = '14';


/** The late percent a sheet has until one is set. */
export const DEFAULT_LATE_PCT = '0.00';

/** What the sheet's late charge depends on: its today, its late percent (the one in force on each 30-day boundary of an overdue claim) and its
 *  policy date (its today while blank), which decides how claims round. */
export function useLate(sheetId: string): Late {
  const settings = useSettings(sheetId);
  const today = useSheetToday(sheetId);
  const policy = settings.data?.find((s) => s.key === 'policy-date')?.value ?? today;
  return { today, pct: (date) => inForce(settings.data, 'late-pct', date)?.value ?? DEFAULT_LATE_PCT, policy };
}
