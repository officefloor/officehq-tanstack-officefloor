import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Late } from '../overdue/late';
import { useSheetToday } from '../session/session';

/** A sheet setting: `value` is text (clerk-limit, floor: an amount with two decimals; terms: a whole number of days; late-pct: a percent with two decimals), null when blank. */
export type Setting = { sheetId: string; key: string; value: string | null };

export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'];

/** The sheet's settings, by key. */
export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Setting[]>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}

/** The floor a sheet has until one is set. */
export const DEFAULT_FLOOR = '1.00';

/** The sheet's floor: the least a claim's live due may be (two decimals). */
export function useFloor(sheetId: string): string {
  const settings = useSettings(sheetId);
  return settings.data?.find((s) => s.key === 'floor')?.value ?? DEFAULT_FLOOR;
}

/** The terms a sheet has until they are set. */
export const DEFAULT_TERMS = '14';

/** The sheet's terms: the days from a claim's date to its due-by. */
export function useTerms(sheetId: string): number {
  const settings = useSettings(sheetId);
  return Number(settings.data?.find((s) => s.key === 'terms')?.value ?? DEFAULT_TERMS);
}

/** The late percent a sheet has until one is set. */
export const DEFAULT_LATE_PCT = '0.00';

/** What the sheet's late charge depends on: its terms, its today and its late percent. */
export function useLate(sheetId: string): Late {
  const settings = useSettings(sheetId);
  const today = useSheetToday(sheetId);
  const terms = useTerms(sheetId);
  return { terms, today, pct: settings.data?.find((s) => s.key === 'late-pct')?.value ?? DEFAULT_LATE_PCT };
}
