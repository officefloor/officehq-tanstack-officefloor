import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Late } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';

/** The sheet's settings: the clerk limit with two decimals ('' when there is none) and the floor with two
 *  decimals (a claim's live due is at least it), and the terms in days (a claim's due-by is its date plus them), and
 *  the late percent with two decimals (an overdue claim gains it, of its figure, every full 30 days). */
export type Settings = { clerkLimit: string; floor: string; terms: string; latePct: string };

export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'] as const;

/** The floor a sheet starts with. */
export const DEFAULT_FLOOR = '1.00';

/** The terms a sheet starts with, in days. */
export const DEFAULT_TERMS = '14';

/** The late percent a sheet starts with. */
export const DEFAULT_LATE_PCT = '0.00';

/** The sheet's settings as they are now. */
export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}

/** The sheet's floor in force (the starting floor while the settings load). */
export function useFloor(sheetId: string): string {
  return useSettings(sheetId).data?.floor ?? DEFAULT_FLOOR;
}

/** The sheet's terms in force, in days (the starting terms while the settings load). */
export function useTerms(sheetId: string): number {
  return Number(useSettings(sheetId).data?.terms ?? DEFAULT_TERMS);
}

/** What the late charge follows now: the terms, the late percent and the sheet's today. */
export function useLate(sheetId: string): Late {
  return { terms: useTerms(sheetId), pct: useSettings(sheetId).data?.latePct ?? DEFAULT_LATE_PCT, today: useSheetToday(sheetId) };
}
