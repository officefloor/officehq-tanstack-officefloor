import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';

/** The sheet's settings: `clerkLimit` (two decimals) is the most due a clerk may settle a claim at,
 *  null for no limit; `floor` (two decimals) is the least a claim's live due may be; `terms` is the
 *  whole days after its date a claim is due by; `latePct` (two decimals) is the percent of an overdue
 *  claim's figure charged for every full 30 days it is overdue; `policyDate` (yyyy-MM-dd) is the date
 *  rounding turns on, null when blank (then the sheet's today); `periodNumberingFrom` (yyyy-MM-dd) is the
 *  date from which claims opened are numbered per period, null when blank (off); `gradeTerms` is the terms
 *  in force for each grade (its own, else the sheet's), by grade; `cap` (two decimals) is the most a person's
 *  due-total may be, null for no cap; `proofThreshold` (two decimals) is the due above which a claim needs a proof
 *  before it is settled or approved, null for none. Each of these that carries a figure is the one in force on the sheet's today.
 *  `floorFrom` and `latePctFrom` are the floor and late percent recorded, oldest from-date (yyyy-MM-dd) first:
 *  a claim's floor and late percent are those in force on its date. */
export type Settings = { clerkLimit: string | null; floor: string; terms: string; latePct: string; policyDate: string | null;
  periodNumberingFrom: string | null; gradeTerms: Record<string, string>; cap: string | null; proofThreshold: string | null;
  floorFrom: Dated[]; latePctFrom: Dated[] };

/** A setting's value recorded from a date (yyyy-MM-dd); a null value is blank. */
export type Dated = { from: string; value: string | null };

/** The floor a sheet has before any is recorded. */
const STARTING_FLOOR = '1.00';

/** The late percent a sheet has before any is recorded. */
const STARTING_LATE_PCT = '0.00';

/** The value in force on the date: the one with the latest from-date on or before it, else `starting`. */
export function valueOn(history: Dated[], date: string, starting: string): string {
  let found: string | null = null;
  for (const d of history) if (d.from <= date) found = d.value;
  return found ?? starting;
}

/** The floor in force on each date; undefined until the settings are read. */
export function floorOf(settings: Settings | undefined): ((date: string) => string) | undefined {
  return settings && ((date) => valueOn(settings.floorFrom, date, STARTING_FLOOR));
}

/** The late percent in force on each date; undefined until the settings are read. */
export function latePctOf(settings: Settings | undefined): ((date: string) => string) | undefined {
  return settings && ((date) => valueOn(settings.latePctFrom, date, STARTING_LATE_PCT));
}

/** Under the sheet's key, so loading the sheet again re-reads its settings. */
export const settingsKey = (sheet: string) => [...sheetKey(sheet), 'settings'] as const;

export function useSettings(sheet: string) {
  return useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`),
    enabled: sheet !== '',
  });
}
