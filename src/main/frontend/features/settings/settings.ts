import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useClaims, type Claim } from '../claims/claims';
import { gradeOn, usePeople } from '../people/people';
import type { Late } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';

/** The sheet's settings: the clerk limit with two decimals ('' when there is none) and the floor with two
 *  decimals (a claim's live due is at least it), and the terms in business days (a claim's due-by is the terms-th business day after its date: not a Saturday, Sunday or holiday), and
 *  the late percent with two decimals (an overdue claim gains, of its figure, the one in force on each full 30-day boundary past its due-by), and the
 *  policy date (yyyy-mm-dd; '' when blank: the sheet's today) a claim's live figures round by, and the period numbering from (yyyy-mm-dd; '' when off: on or after it, by
 *  the sheet's today, a claim opened is numbered within its period), and each grade's own terms in days ('' when the
 *  grade has none: it uses the sheet's terms), and the cap with two decimals ('' when there is none: a person's
 *  due-total is at most it), and the proof threshold with two decimals ('' when there is none: a claim whose due is
 *  above the one in force on its date cannot be settled or approved without a proof), and the note threshold with two decimals ('' when there is none: a
 *  claim whose due is above the one in force on its date cannot be opened with an empty note). A claim's terms are fixed when it is created. Each is the value in force on the sheet's
 *  today; `dated` holds every value recorded for the settings that carry a figure, by key (clerk-limit, floor, terms,
 *  terms-A/B/C, late-pct, cap, proof-threshold, proof-threshold-A/B/C, note-threshold), oldest from-date first. `perms` is the permission matrix (not dated): for
 *  each action the roles that may take it, comma separated ('' when blank: the standing rule). `graceDays` is the grace days (not dated; '' when off): when the sheet's
 *  today is changed, each open period whose to-date plus them is before the new today is closed. `entryDate` is the entry date (not dated; '' when off): while it is set, a new claim
 *  given no date is dated it instead of the sheet's today. `gradeGrace` is each grade's grace in days ('' when the grade
 *  has none), dated (keys grace-A/B/C): a claim whose person's grade in force on its date has a grace in force on that
 *  date is overdue, and counts its late charge, from its due-by plus it. `gradeProofThreshold` is each grade's own proof
 *  threshold with two decimals ('' when the grade has none: the sheet's applies), dated (keys proof-threshold-A/B/C). */
export type Settings = { clerkLimit: string; floor: string; terms: string; latePct: string; policyDate: string;
  periodNumberingFrom: string; gradeTerms: Record<string, string>; cap: string; proofThreshold?: string; noteThreshold?: string; dated?: Record<string, Dated[]>;
  perms?: Record<string, string>; graceDays?: string; entryDate?: string; gradeGrace?: Record<string, string>;
  gradeProofThreshold?: Record<string, string> };

/** A dated setting's value ('' when blank) in force from its from-date (yyyy-mm-dd). */
export type Dated = { from: string; value: string };

/** A dated figure as it applies to a date (yyyy-mm-dd): the value in force on it. */
export type DatedValue = (date: string) => string;

/** The value of a dated setting in force on a date: the one with the latest from-date on or before it, else the
 *  sheet's starting value. */
export function valueOn(values: Dated[] | undefined, start: string, date: string): string {
  let found: Dated | undefined;
  for (const v of values ?? []) {
    if (v.from <= date && (!found || found.from < v.from)) found = v;
  }
  return found ? found.value : start;
}

/** The actions the permission matrix covers, in order. */
export const PERM_ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen'] as const;

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

/** The sheet's floor by date: a claim's live due is at least the one in force on its date (the starting floor
 *  while the settings load). */
export function useFloor(sheetId: string): DatedValue {
  const dated = useSettings(sheetId).data?.dated?.floor;
  return useMemo(() => (date: string) => valueOn(dated, DEFAULT_FLOOR, date), [dated]);
}

/** The sheet's cap in force ('' when there is none, or while the settings load). */
export function useCap(sheetId: string): string {
  return useSettings(sheetId).data?.cap ?? '';
}

/** The sheet's terms in force, in days (the starting terms while the settings load). */
export function useTerms(sheetId: string): number {
  return Number(useSettings(sheetId).data?.terms ?? DEFAULT_TERMS);
}

/** What the late charge follows now: the late percent by date (a claim gains the one in force on its date) and the sheet's today, and each
 *  claim's grace (of its person's grade on its date, in force on that date; 0 while loading); and how claims round:
 *  the policy date in force (blank: the sheet's today) and the dates of the sheet's numbered claims. */
export function useLate(sheetId: string): Late {
  const settings = useSettings(sheetId).data;
  const today = useSheetToday(sheetId);
  const claims = useClaims(sheetId).data;
  const numbered = useMemo(
    () => new Map((claims ?? []).filter((c) => c.number !== null).map((c) => [c.number as string, c.date])),
    [claims],
  );
  const dated = settings?.dated?.['late-pct'];
  const pct = useMemo(() => (date: string) => valueOn(dated, DEFAULT_LATE_PCT, date), [dated]);
  const people = usePeople(sheetId).data;
  const history = settings?.dated;
  const grace = useMemo(() => {
    const byId = new Map((people ?? []).map((p) => [p.id, p]));
    return (claim: Claim) => {
      const grade = gradeOn(byId.get(claim.personId), claim.date);
      if (grade === '') return 0;
      const days = valueOn(history?.[`grace-${grade}`], '', claim.date);
      return days === '' ? 0 : Number(days);
    };
  }, [people, history]);
  return { pct, today, grace,
    policy: settings?.policyDate || today, numbered };
}
