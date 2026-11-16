import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { nowKey, useReadNow } from '../../query/readNow';
import type { Late } from '../overdue/late';
import { gradeOn, usePeople } from '../people/people';
import { useSheetToday } from '../session/session';

/** A sheet setting: `value` is text (clerk-limit, cap, floor: an amount with two decimals; terms, terms-A, terms-B, terms-C: a whole number of days; late-pct: a percent with two decimals; grace-A, grace-B, grace-C: a whole number of days; policy-date, period-numbering-from, entry-date: yyyy-MM-dd; fiscal-start-month: a month 1 to 12), null when blank.
 *  `from` is the date the value is in force from (yyyy-MM-dd); null when undated (policy-date, period-numbering-from, entry-date, or a
 *  value from before settings were dated: in force on every date). */
export type Setting = { sheetId: string; key: string; value: string | null; from: string | null };

export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'];

/** The sheet's settings: every value recorded, by key then from-date. */
export function useSettings(sheetId: string) {
  const now = useReadNow();
  return useQuery({
    queryKey: nowKey(settingsKey(sheetId), now),
    queryFn: () => getJson<Setting[]>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, now),
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

/** The fiscal year start month a sheet has until one is set. */
export const DEFAULT_FISCAL_START_MONTH = '1';

/** The sheet's fiscal year start month (1 to 12; undated), 1 unless set. */
export function useFiscalStartMonth(sheetId: string): number {
  const settings = useSettings(sheetId);
  return Number(settings.data?.find((s) => s.key === 'fiscal-start-month')?.value ?? DEFAULT_FISCAL_START_MONTH);
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

/** What the sheet's late charge depends on: its today, its late percent (the one in force on each 30-day boundary of an overdue claim), its
 *  policy date (its today while blank), which decides how claims round, and each claim's grace: grace-A, grace-B or grace-C (whole days,
 *  blank for none) of its person's grade in force on its date, the one in force on that date. */
export function useLate(sheetId: string): Late {
  const settings = useSettings(sheetId);
  const people = usePeople(sheetId);
  const today = useSheetToday(sheetId);
  const policy = settings.data?.find((s) => s.key === 'policy-date')?.value ?? today;
  const grace = (claim: { personId: number; date: string }) => {
    const person = people.data?.find((p) => p.id === claim.personId);
    const value = person === undefined ? null : inForce(settings.data, `grace-${gradeOn(person, claim.date)}`, claim.date)?.value;
    return value === null || value === undefined ? 0 : Number(value);
  };
  return { today, pct: (date) => inForce(settings.data, 'late-pct', date)?.value ?? DEFAULT_LATE_PCT, policy, grace };
}
