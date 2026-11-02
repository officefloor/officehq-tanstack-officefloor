import { useQuery } from '@tanstack/react-query';
import { useReadScope } from '../../query/live';
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
 *  And the proof kinds (comma separated receipt and photo; '' for none, not dated): a claim above the proof threshold
 *  needs a proof of each before it is settled or approved.
 *  And the pot (two decimals; '' for none, not dated): what each balanced pool shares out among its members.
 *  And the parent sheet's id ('' for none, not dated): where the sheet has no rate in force for a grade on a date, the
 *  parent's is used, then its parent's, up to three levels up.
 *  And the fiscal start month (1 to 12, not dated): the month a fiscal year starts in.
 *  And the alert threshold (two decimals; '' for off, not dated): a person's due-total crossing above it is audited.
 *  And the rounding mode (half-up or half-even; '' for none), in force on the sheet's today: a claim whose date has
 *  one in force rounds its live figures by it in place of the policy date.
 *  And the review threshold (two decimals; '' for none), in force on the sheet's today.
 *  And the review SLA (a whole number of days; '' for none, not dated): a REVIEW claim whose review day plus it is
 *  before the sheet's today is late for review.
 *  And the default grade (A, B or C; '' for A, not dated): the grade a person added by the quick-add bar, or by an
 *  import line with a blank grade, takes.
 *  And, for each dated key (every one that carries a figure), its values as recorded, by from-date. */
export type Settings = { clerkLimit: string; floor: string; terms: string; latePct: string; policyDate: string; periodNumberingFrom: string;
  termsA: string; termsB: string; termsC: string; cap: string;
  proofThreshold: string; noteThreshold: string; reviewThreshold: string; graceDays: string; escalateAfter: string; entryDate: string; graceA: string; graceB: string; graceC: string;
  proofThresholdA: string; proofThresholdB: string; proofThresholdC: string; pot: string; parent: string; fiscalStartMonth: string; alertThreshold: string; rounding: string; reviewSla: string; proofKinds: string; defaultGrade: string;
  clerkLimitA: string; clerkLimitB: string; clerkLimitC: string; perms: Partial<Record<PermAction, string>>; dated: Partial<Record<SettingKey, DatedValue[]>> };

/** A dated setting's value as recorded: in force from its from-date (yyyy-MM-dd). */
export type DatedValue = { from: string; value: string };

/** A figure in force by date (yyyy-MM-dd), in hundredths. */
export type DatedCents = (date: string) => bigint;

/** Whether a setting is dated: every one that carries a figure is but the pot and the alert threshold; the policy, period numbering and entry dates and the grace days are not. */
export function isDated(key: SettingKey): boolean {
  return key !== 'policy-date' && key !== 'period-numbering-from' && key !== 'grace-days' && key !== 'escalate-after' && key !== 'review-sla' && key !== 'entry-date' && key !== 'proof-kinds' && key !== 'pot' && key !== 'alert-threshold' && key !== 'parent' && key !== 'fiscal-start-month' && key !== 'default-grade' && !key.startsWith('perm-');
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
  | 'terms-A' | 'terms-B' | 'terms-C' | 'grace-A' | 'grace-B' | 'grace-C' | 'clerk-limit-A' | 'clerk-limit-B' | 'clerk-limit-C' | 'proof-threshold-A' | 'proof-threshold-B' | 'proof-threshold-C' | 'cap' | 'proof-threshold' | 'proof-kinds' | 'note-threshold' | 'review-threshold' | 'grace-days' | 'escalate-after' | 'review-sla' | 'entry-date' | 'pot' | 'parent' | 'fiscal-start-month' | 'alert-threshold' | 'rounding' | 'default-grade' | `perm-${PermAction}`;

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

/** The grades that may have a clerk limit of their own, with the setting key and field of {@link Settings} of each. */
export const GRADE_CLERK_LIMIT = [
  { grade: 'A', key: 'clerk-limit-A', field: 'clerkLimitA' },
  { grade: 'B', key: 'clerk-limit-B', field: 'clerkLimitB' },
  { grade: 'C', key: 'clerk-limit-C', field: 'clerkLimitC' },
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
  const read = useReadScope(settingsKey(sheet));
  return useQuery({
    queryKey: read.key,
    queryFn: () => read.get<Settings>(settingsPath(sheet)),
  });
}

/** The sheet's floor in hundredths by date: the least a claim's live due is, the one in force on its date. */
export function useFloorCents(sheet: string): DatedCents {
  const { data } = useSettings(sheet);
  return datedCents(data?.dated?.floor, DEFAULT_FLOOR);
}

/** The month (1 to 12) the sheet's fiscal year starts in; 1 unless set. */
export function useFiscalStartMonth(sheet: string): number {
  const { data } = useSettings(sheet);
  return Number(data?.fiscalStartMonth || '1');
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

/** The sheet's review SLA in whole days, or null when there is none. */
export function useReviewSla(sheet: string): number | null {
  const { data } = useSettings(sheet);
  const days = data?.reviewSla ?? '';
  return days === '' ? null : Number.parseInt(days, 10);
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

/** The sheet's rounding mode in force on a date (yyyy-MM-dd): 'half-up', 'half-even', or '' for none. */
export type RoundingModeOn = (date: string) => string;

/** The sheet's rounding mode by date: the value of the latest from-date on or before it (a blank, or none: none). */
export function useRoundingMode(sheet: string): RoundingModeOn {
  const { data } = useSettings(sheet);
  const sorted = [...(data?.dated?.rounding ?? [])].sort((a, b) => (a.from < b.from ? -1 : a.from > b.from ? 1 : 0));
  return (date) => {
    let value = '';
    for (const v of sorted) {
      if (v.from > date) break;
      value = v.value;
    }
    return value;
  };
}

/** The sheet's policy date (yyyy-MM-dd), or '' when it is the sheet's today. */
export function usePolicyDate(sheet: string): string {
  const { data } = useSettings(sheet);
  return data?.policyDate ?? '';
}
