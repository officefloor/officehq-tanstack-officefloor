import { useQuery } from '@tanstack/react-query';
import { useReader } from '../../api/readNow';

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
 *  loaded; `dated` is every value recorded for each of them, by key (clerk-limit, clerk-limit-<grade>: the grade's own clerk limit, used for a claim of a person of
 *  that grade instead of the sheet's while one is in force on the sheet's today, floor, terms, terms-<grade>,
 *  late-pct, cap, proof-threshold: a claim whose due is above the one in force on its date cannot be settled or
 *  approved without a proof; proof-threshold-<grade>: the grade's own proof threshold, used for a claim of a
 *  person of that grade instead of the sheet's while one is in force on its date; note-threshold: one above it cannot be opened with an empty note; review-threshold: one above it goes to REVIEW when opened; rounding: the rounding mode, half-up or half-even, a claim of a date with one in force rounds its live figures by it; grace-<grade>: the grade's grace days, see `overdueFrom`), oldest from-date first. `permissions` is the permission matrix entries set, by action
 *  (open, settle, void, reassign, close, reopen): the roles that may take it, comma separated; an action not listed
 *  keeps the standing rule (open, settle, void and close by clerk and steward; reassign and reopen by steward).
 *  `graceDays` is the days after a period's to-date it is left open (null when blank: off; not dated): changing the
 *  sheet's today closes each open period lapsed past them. `delegations` is each delegation of the steward role
 *  made on the sheet, oldest first. `entryDate` is the entry date (yyyy-mm-dd; null when blank: off; not dated): while
 *  set, it is the default date of a new claim instead of the sheet's today. `holidays` is the sheet's holidays
 *  (yyyy-mm-dd), earliest first: a claim's due-by, fixed when it is created, that falls on one is moved to the next
 *  day that is not one. `voidReasons` is the sheet's void reasons, in the order they were added: while there is one,
 *  voiding a claim needs one of them. `pot` is the sheet's pot (two decimals; null when blank: none; not dated): each
 *  member of a balanced pool gets a cut of it. `parent` is the sheet's parent sheet id (null when blank: none; not
 *  dated): a grade with no rate in force on a date takes the parent's, then its parent's, up to three levels up.
 *  `fiscalStartMonth` is the month (1 to 12; 1 unless set; not dated) the sheet's fiscal year starts in.
 *  `alertThreshold` is the sheet's alert threshold (two decimals; null when blank: off; not dated): a person whose
 *  due-total goes from at or below it to above it is written an ALERT line. `escalateAfter` is the sheet's escalation
 *  gap in days (null when blank: off; not dated): reminding a claim whose previous reminder was at least that many days
 *  before the sheet's today escalates it, once per claim ever. `reviewSla` is the sheet's review SLA in days (null when
 *  blank: none; not dated): a REVIEW claim whose review day plus it is before the sheet's today is review-late.
 *  `proofKinds` is the sheet's proof kinds (a comma separated list of receipt and photo; null when blank: none; not
 *  dated): a claim above the proof threshold needs a proof of each listed kind to be settled or approved.
 *  `defaultGrade` is the sheet's default grade (A, B or C; null when blank: A; not dated): the grade the quick-add
 *  bar gives a new person, and the grade of an imported line that leaves it blank. */
export type Settings = { clerkLimit: string | null; floor: string; terms: number; latePct: string; roles: Role[]; policyDate: string | null; periodNumberingFrom: string | null;
  gradeTerms: Record<string, number>; cap: string | null; dated: Record<string, DatedValue[]>;
  permissions: Record<string, string>; graceDays: number | null; delegations: Delegation[];
  entryDate: string | null; holidays: string[]; voidReasons: string[]; pot: string | null;
  parent: string | null; fiscalStartMonth: number; alertThreshold: string | null; escalateAfter: number | null; reviewSla: number | null; proofKinds: string | null; defaultGrade: string | null };

/** A steward's (`by`) delegation of the steward role to a user, from a date to a date (yyyy-mm-dd, inclusive):
 *  while the sheet's today is inside it the user acts as a steward. */
export type Delegation = { user: string; by: string; from: string; to: string };

/** The role a user acts in on the sheet: steward while a delegation to them covers the sheet's today, else
 *  their own role (undefined when none is set: a clerk). */
export function roleOf(settings: Settings | undefined, user: string | undefined, today: string | undefined): string | undefined {
  if (today !== undefined && settings?.delegations?.some((d) => d.user === user && d.from <= today && today <= d.to)) return 'steward';
  return settings?.roles.find((r) => r.user === user)?.role;
}

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
  const read = useReader();
  return useQuery({
    queryKey: read.key(settingsKey(sheetId)),
    queryFn: () => read.get<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}
