import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The role a user name holds on the sheet: clerk or steward. A name not listed is a clerk. */
export type Role = { user: string; role: string };

/** The sheet's settings: its clerk limit (two decimals; null means no limit), its floor (two decimals:
 *  the least a live due can be), its terms (the days after a claim's date that it is due by) and the
 *  roles set, and its late percent (two decimals: what an overdue claim's due gains, of its figure, for
 *  every full 30 days it is overdue), and its policy date (yyyy-mm-dd; null when blank, meaning the
 *  sheet's today: a claim dated before it rounds half up, one on or after it half to even), and its period numbering from-date (yyyy-mm-dd; null
 *  when blank, meaning off: while set and the sheet's today is on or after it, a claim opened is
 *  numbered within its period), and each grade's own terms, by grade (a grade not listed uses the sheet's
 *  terms; a claim's due-by is fixed from them when it is created), and its cap (two decimals; null means
 *  none: a person's due-total is at most it). Those with a figure are as in force on the sheet's today when
 *  loaded; `dated` is every value recorded for each of them, by key (clerk-limit, floor, terms, terms-<grade>,
 *  late-pct, cap, proof-threshold: a claim whose due is above the one in force on its date cannot be settled or
 *  approved without a proof), oldest from-date first. `permissions` is the permission matrix entries set, by action
 *  (open, settle, void, reassign, close, reopen): the roles that may take it, comma separated; an action not listed
 *  keeps the standing rule (open, settle, void and close by clerk and steward; reassign and reopen by steward).
 *  `graceDays` is the days after a period's to-date it is left open (null when blank: off; not dated): changing the
 *  sheet's today closes each open period lapsed past them. */
export type Settings = { clerkLimit: string | null; floor: string; terms: number; latePct: string; roles: Role[]; policyDate: string | null; periodNumberingFrom: string | null;
  gradeTerms: Record<string, number>; cap: string | null; dated: Record<string, DatedValue[]>;
  permissions: Record<string, string>; graceDays: number | null };

/** A value a dated setting has from a date (yyyy-mm-dd); null when blank. */
export type DatedValue = { from: string; value: string | null };

/** The value each dated setting starts with, before its first from-date; a key not listed has none. */
const STARTING: Record<string, string> = { floor: '1.00', 'late-pct': '0.00', terms: '14' };

/** The value of a dated setting in force on a date: the one recorded with the latest from-date on or
 *  before it, else the setting's starting value; null when blank or there is none. */
export function settingOn(settings: Settings | undefined, key: string, date: string | undefined): string | null {
  let value: string | null = STARTING[key] ?? null;
  if (date === undefined) return value;
  for (const d of settings?.dated?.[key] ?? []) {
    if (d.from <= date) value = d.value;
  }
  return value;
}

export function settingsKey(sheetId: string) {
  return ['sheet', sheetId, 'settings'];
}

export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}
