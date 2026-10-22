import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useClaims } from '../claims/claims';
import type { Late } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';

/** The sheet's settings: the clerk limit with two decimals ('' when there is none) and the floor with two
 *  decimals (a claim's live due is at least it), and the terms in days (a claim's due-by is its date plus them), and
 *  the late percent with two decimals (an overdue claim gains it, of its figure, every full 30 days), and the
 *  policy date (yyyy-mm-dd; '' when blank: the sheet's today) a claim's live figures round by, and the period numbering from (yyyy-mm-dd; '' when off: on or after it, by
 *  the sheet's today, a claim opened is numbered within its period), and each grade's own terms in days ('' when the
 *  grade has none: it uses the sheet's terms). A claim's due-by is fixed when it is created. */
export type Settings = { clerkLimit: string; floor: string; terms: string; latePct: string; policyDate: string;
  periodNumberingFrom: string; gradeTerms: Record<string, string> };

/** The grades that may have terms of their own. */
export const GRADES = ['A', 'B', 'C'] as const;

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

/** What the late charge follows now: the late percent and the sheet's today; and how claims round:
 *  the policy date in force (blank: the sheet's today) and the dates of the sheet's numbered claims. */
export function useLate(sheetId: string): Late {
  const settings = useSettings(sheetId).data;
  const today = useSheetToday(sheetId);
  const claims = useClaims(sheetId).data;
  const numbered = useMemo(
    () => new Map((claims ?? []).filter((c) => c.number !== null).map((c) => [c.number as string, c.date])),
    [claims],
  );
  return { pct: settings?.latePct ?? DEFAULT_LATE_PCT, today,
    policy: settings?.policyDate || today, numbered };
}
