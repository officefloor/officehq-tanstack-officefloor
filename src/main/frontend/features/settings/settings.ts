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
 *  cap with two decimals, the most a person's due-total is ('' for no cap). */
export type Settings = { clerkLimit: string; floor: string; terms: string; latePct: string; policyDate: string; periodNumberingFrom: string;
  termsA: string; termsB: string; termsC: string; cap: string };

/** A setting's key as the server names it, with the field of {@link Settings} that shows it. */
export type SettingKey = 'clerk-limit' | 'floor' | 'terms' | 'late-pct' | 'policy-date' | 'period-numbering-from'
  | 'terms-A' | 'terms-B' | 'terms-C' | 'cap';

/** The grades that may have terms of their own, with the setting key and field of {@link Settings} of each. */
export const GRADE_TERMS = [
  { grade: 'A', key: 'terms-A', field: 'termsA' },
  { grade: 'B', key: 'terms-B', field: 'termsB' },
  { grade: 'C', key: 'terms-C', field: 'termsC' },
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

/** The sheet's floor in hundredths: the least a claim's live due is. */
export function useFloorCents(sheet: string): bigint {
  const { data } = useSettings(sheet);
  return hundredths(data?.floor ?? DEFAULT_FLOOR);
}

/** The sheet's cap in hundredths: the most a person's due-total is; undefined when there is none. */
export function useCapCents(sheet: string): bigint | undefined {
  const { data } = useSettings(sheet);
  return data?.cap ? hundredths(data.cap) : undefined;
}

/** The sheet's late percent in hundredths: charged on an overdue claim's figure for every full 30 days it is
 *  overdue. */
export function useLatePctCents(sheet: string): bigint {
  const { data } = useSettings(sheet);
  return hundredths(data?.latePct ?? DEFAULT_LATE_PCT);
}

/** The sheet's policy date (yyyy-MM-dd), or '' when it is the sheet's today. */
export function usePolicyDate(sheet: string): string {
  const { data } = useSettings(sheet);
  return data?.policyDate ?? '';
}
