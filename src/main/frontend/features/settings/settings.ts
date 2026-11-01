import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { hundredths } from '../rates/rates';

/** The sheet's settings: the clerk limit with two decimals, the most due a clerk may settle ('' for no limit);
 *  the floor with two decimals, the least a claim's live due is; and the terms, the whole days after its date by
 *  which a claim is due; and the late percent with two decimals, charged on an overdue claim's figure for every
 *  full 30 days it is overdue; and the policy date (yyyy-MM-dd; '' for the sheet's today): a claim dated before it
 *  rounds half up, on or after it half to even; and the period numbering from-date (yyyy-MM-dd; '' for off): while
 *  the sheet's today is on or after it, a claim opened is numbered within its period; and each grade's own terms
 *  (whole days; '' when it takes the sheet's terms), from which a claim's due-by is fixed when it is added; and the
 *  cap with two decimals, the most a person's due-total is ('' for no cap). Each is the value in force on the sheet's
 *  today. And each action's permitted roles in the permission matrix (comma separated; '' for its standing rule). And the
 *  grace days (whole days; '' for off): a today change closes each open period whose to-date plus them is before it.
 *  And the entry date (yyyy-MM-dd; '' for off): while set, a new claim's date defaults to it instead of the sheet's today.
 *  And each grade's grace (whole days; '' for none), in force on the sheet's today: a claim whose person's grade in
 *  force on its date has a grace in force on that date is overdue only once its due-by plus the grace is before the
 *  sheet's today, and its late charge's 30-day boundaries are counted from there.
 *  And each grade's own proof threshold (two decimals; '' for none), in force on the sheet's today: a claim whose
 *  person's grade in force on its date has one in force on that date takes it in place of the sheet's.
 *  And the pot (two decimals; '' for none, not dated): what each balanced pool shares out among its members.
 *  And, for each dated key (every one that carries a figure), its values as recorded, by from-date. */
export type Settings = { clerkLimit: string; floor: string; terms: string; latePct: string; policyDate: string; periodNumberingFrom: string;
  termsA: string; termsB: string; termsC: string; cap: string;
  proofThreshold: string; noteThreshold: string; graceDays: string; entryDate: string; graceA: string; graceB: string; graceC: string;
  proofThresholdA: string; proofThresholdB: string; proofThresholdC: string; pot: string; perms: Partial<Record<PermAction, string>>; dated: Partial<Record<SettingKey, DatedValue[]>> };

/** A dated setting's value as recorded: in force from its from-date (yyyy-MM-dd). */
export type DatedValue = { from: string; value: string };

/** A figure in force by date (yyyy-MM-dd), in hundredths. */
export type DatedCents = (date: string) => bigint;

/** Whether a setting is dated: every one that carries a figure is but the pot; the policy, period numbering and entry dates and the grace days are not. */
export function isDated(key: SettingKey): boolean {
  return key !== 'policy-date' && key !== 'period-numbering-from' && key !== 'grace-days' && key !== 'entry-date' && key !== 'pot' && !key.startsWith('perm-');
}

/** The figure in force on a date: the value of the latest from-date on or before it, before every from-date (or
 *  where a blank is in force) the starting value. */
function datedCents(values: DatedValue[] | undefined, start: string): DatedCents {
  const sorted = [...(values ?? [])].sort((a, b) => (a.from < b.from ? -1 : a.from > b.from ? 1 : 0));
  return (date) => {
    let value = start;
    for (const v of sorted) {
      if (v.from > date) break;
      value = v.value === '' ? start : v.value;
    }
    return hundredths(value);
  };
}

/** A setting's key as the server names it, with the field of {@link Settings} that shows it. */
export type SettingKey = 'clerk-limit' | 'floor' | 'terms' | 'late-pct' | 'policy-date' | 'period-numbering-from'
  | 'terms-A' | 'terms-B' | 'terms-C' | 'grace-A' | 'grace-B' | 'grace-C' | 'proof-threshold-A' | 'proof-threshold-B' | 'proof-threshold-C' | 'cap' | 'proof-threshold' | 'note-threshold' | 'grace-days' | 'entry-date' | 'pot' | `perm-${PermAction}`;

/** The actions whose roles the permission matrix sets, each with its standing rule (used while its entry is blank). */
export const PERMISSIONS = [
  { action: 'open', standing: 'clerk,steward' },
  { action: 'settle', standing: 'clerk,steward' },
  { action: 'void', standing: 'clerk,steward' },
  { action: 'reassign', standing: 'steward' },
  { action: 'close', standing: 'clerk,steward' },
  { action: 'reopen', standing: 'steward' },
] as const;

export type PermAction = (typeof PERMISSIONS)[number]['action'];

/** The grades that may have terms of their own, with the setting key and field of {@link Settings} of each. */
export const GRADE_TERMS = [
  { grade: 'A', key: 'terms-A', field: 'termsA' },
  { grade: 'B', key: 'terms-B', field: 'termsB' },
  { grade: 'C', key: 'terms-C', field: 'termsC' },
] as const;

/** The grades that may have a grace, with the setting key and field of {@link Settings} of each. */
export const GRADE_GRACE = [
  { grade: 'A', key: 'grace-A', field: 'graceA' },
  { grade: 'B', key: 'grace-B', field: 'graceB' },
  { grade: 'C', key: 'grace-C', field: 'graceC' },
] as const;

/** The grades that may have a proof threshold of their own, with the setting key and field of {@link Settings} of each. */
export const GRADE_PROOF_THRESHOLD = [
  { grade: 'A', key: 'proof-threshold-A', field: 'proofThresholdA' },
  { grade: 'B', key: 'proof-threshold-B', field: 'proofThresholdB' },
  { grade: 'C', key: 'proof-threshold-C', field: 'proofThresholdC' },
] as const;

/** The floor a sheet starts with. */
export const DEFAULT_FLOOR = '1.00';

/** The late percent a sheet starts with. */
export const DEFAULT_LATE_PCT = '0.00';

/** The terms a sheet starts with. */
export const DEFAULT_TERMS = '14';

export const settingsKey = (sheet: string) => ['settings', sheet] as const;

export const settingsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/settings`;

export function useSettings(sheet: string) {
  return useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(settingsPath(sheet)),
  });
}

/** The sheet's floor in hundredths by date: the least a claim's live due is, the one in force on its date. */
export function useFloorCents(sheet: string): DatedCents {
  const { data } = useSettings(sheet);
  return datedCents(data?.dated?.floor, DEFAULT_FLOOR);
}

/** The sheet's cap in hundredths: the most a person's due-total is; undefined when there is none. */
export function useCapCents(sheet: string): bigint | undefined {
  const { data } = useSettings(sheet);
  return data?.cap ? hundredths(data.cap) : undefined;
}

/** The sheet's late percent in hundredths by date: charged on an overdue claim's figure for every full 30 days it is
 *  overdue, the one in force on each 30-day boundary after its due-by. */
export function useLatePctCents(sheet: string): DatedCents {
  const { data } = useSettings(sheet);
  return datedCents(data?.dated?.['late-pct'], DEFAULT_LATE_PCT);
}

/** The grace of a grade in force on a date (yyyy-MM-dd), in whole days; 0 when it has none then. */
export type GraceDays = (grade: string | undefined, date: string) => number;

/** Each grade's grace by date: the value of the latest from-date on or before it (a blank, or none: no grace). */
export function useGraceDays(sheet: string): GraceDays {
  const { data } = useSettings(sheet);
  return (grade, date) => {
    const entry = GRADE_GRACE.find((g) => g.grade === grade);
    if (entry === undefined) return 0;
    const sorted = [...(data?.dated?.[entry.key] ?? [])].sort((a, b) => (a.from < b.from ? -1 : a.from > b.from ? 1 : 0));
    let value = '';
    for (const v of sorted) {
      if (v.from > date) break;
      value = v.value;
    }
    return value === '' ? 0 : Number.parseInt(value, 10);
  };
}

/** The sheet's policy date (yyyy-MM-dd), or '' when it is the sheet's today. */
export function usePolicyDate(sheet: string): string {
  const { data } = useSettings(sheet);
  return data?.policyDate ?? '';
}
