import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';

/** The sheet's settings: `clerkLimit` (two decimals) is the most due a clerk may settle a claim at,
 *  null for no limit; `floor` (two decimals) is the least a claim's live due may be; `terms` is the
 *  business days after its date a claim is due by; `latePct` (two decimals) is the percent of an overdue
 *  claim's figure plus its earlier charges (compounding) charged at each full 30 days it is overdue (the one in force on that boundary); `policyDate` (yyyy-MM-dd) is the date
 *  rounding turns on, null when blank (then the sheet's today); `periodNumberingFrom` (yyyy-MM-dd) is the
 *  date from which claims opened are numbered per period, null when blank (off); `gradeTerms` is the terms
 *  in force for each grade (its own, else the sheet's), by grade; `cap` (two decimals) is the most a person's
 *  due-total may be, null for no cap; `proofThreshold` (two decimals) is the due above which a claim needs a proof
 *  before it is settled or approved, null for none; `noteThreshold` (two decimals) is the due above which a
 *  claim needs a note before it is opened, null for none. Each of these that carries a figure is the one in force on the sheet's today.
 *  `floorFrom` and `latePctFrom` are the floor and late percent recorded, oldest from-date (yyyy-MM-dd) first:
 *  a claim's floor and late percent are those in force on its date. `perms` is the permission matrix: the roles
 *  that may take each action (open, settle, void, reassign, close, reopen), comma separated, by action; null when
 *  blank (the standing rule). `graceDays` is the whole days after its to-date an open period is closed when the
 *  sheet's today moves, null when blank (off). `entryDate` (yyyy-MM-dd) is the default date of a new claim
 *  in place of the sheet's today, null when blank (off). `gradeGrace` is each grade's grace days in force on the sheet's
 *  today, by grade, null when it has none; `gradeGraceFrom` is each grade's grace days recorded, by grade, oldest
 *  from-date first: a claim's grace is the one in force on its date for its person's grade then. `gradeProofThreshold`
 *  is each grade's proof threshold in force on the sheet's today, by grade, null when it has none (then the sheet's).
 *  `pot` (two decimals) is the sum a balanced pool shares out among its members, null for none; it is not dated.
 *  `parent` is the id of the sheet whose rates a grade with no rate in force on a date takes (then its parent's, up
 *  to three levels up), null for none; it is not dated. `fiscalStartMonth` is the month (1 to 12) a fiscal year starts in,
 *  1 unless set; it is not dated. `alertThreshold` (two decimals) is the due-total above which a person
 *  crossing it raises an ALERT, null for off; it is not dated. `rounding` is the rounding mode in force on
 *  the sheet's today (half-up or half-even), null for none; `roundingFrom` is the modes recorded, oldest from-date
 *  first (a claim rounds by the one in force on its date, else by the policy date). `reviewThreshold` (two decimals)
 *  is the due above which a claim goes to REVIEW when opened, in force on the sheet's today, null for none.
 *  `escalateAfter` is the whole days at least between a claim's previous reminder and the sheet's today for a reminder
 *  to escalate it (once per claim), null for off; it is not dated. `reviewSla` is the whole days after its review day
 *  a claim in REVIEW may stay there before it is late, null for none; it is not dated. `proofKinds` is the kinds of proof
 *  (receipt, photo), comma separated, a claim above its proof threshold needs one of each of before it is settled or
 *  approved, null for none; it is not dated. `defaultGrade` is the grade the quick-add bar and an import line with a
 *  blank grade give a new person, null when blank (then A); it is not dated. `gradeClerkLimit` is each grade's clerk limit
 *  in force on the sheet's today, by grade, null when it has none (then the sheet's). `scoreMid` and `scoreHigh` are the
 *  whole-number scores at or above which a person's band is MID and HIGH, null when blank (no band while either is);
 *  they are not dated. `scoreLog` is on while moving the sheet's today records each person's score in the audit
 *  file, null when blank; it is not dated. `maxOpen` is the most OPEN or PENDING claims a person may have for
 *  another to be opened, null for none; it is not dated. `pendingSla` is the whole days after its pending day a claim
 *  in PENDING may stay there before it is late, null for none; it is not dated. `policyVersion` is the sheet's current
 *  policy version: the number of changes made to its policy settings (the rounding, the floor, the cap, the terms, the
 *  late percent and the clerk limit). `policyFrom` is each policy version's from-date (yyyy-MM-dd), in the order made,
 *  null for one with no from-date (it counts always). `capFrom` is the caps recorded, oldest from-date first: a person's
 *  cap is the one in force on the date of their latest claim that counts. */
export type Settings = { clerkLimit: string | null; floor: string; terms: string; latePct: string; policyDate: string | null;
  periodNumberingFrom: string | null; gradeTerms: Record<string, string>; cap: string | null; proofThreshold: string | null;
  floorFrom: Dated[]; latePctFrom: Dated[]; perms: Record<string, string | null>;
  graceDays: string | null; noteThreshold: string | null; entryDate: string | null;
  gradeGrace: Record<string, string | null>; gradeGraceFrom: Record<string, Dated[]>;
  gradeProofThreshold: Record<string, string | null>; pot: string | null; parent: string | null; fiscalStartMonth: string;
  alertThreshold: string | null; rounding: string | null; roundingFrom: Dated[]; reviewThreshold: string | null;
  escalateAfter: string | null; reviewSla: string | null; proofKinds: string | null; defaultGrade: string | null;
  gradeClerkLimit: Record<string, string | null>; scoreMid: string | null; scoreHigh: string | null;
  scoreLog: string | null; maxOpen: string | null; pendingSla: string | null;
  policyVersion: string; policyFrom: (string | null)[]; capFrom: Dated[] };

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

/** The cap in force on each date (two decimals), null for none; undefined until the settings are read. */
export function capOf(settings: Settings | undefined): ((date: string) => string | null) | undefined {
  return settings && ((date) => valueOn(settings.capFrom ?? [], date, '') || null);
}

/** The policy version in force on each date: the number of versions whose from-date is on or before it (one with
 *  no from-date counts always); undefined until the settings are read. */
export function policyOf(settings: Settings | undefined): ((date: string) => number) | undefined {
  return settings && ((date) => (settings.policyFrom ?? []).filter((from) => from === null || from <= date).length);
}

/** The rounding mode in force on each date ('half-up', 'half-even', or '' for none); undefined until the settings are read. */
export function roundingOf(settings: Settings | undefined): ((date: string) => string) | undefined {
  return settings && ((date) => valueOn(settings.roundingFrom ?? [], date, ''));
}

/** The grace days of a grade in force on each date (null for none); undefined until the settings are read. */
export function graceOf(settings: Settings | undefined): ((grade: string, date: string) => number | null) | undefined {
  return settings && ((grade, date) => {
    const value = valueOn(settings.gradeGraceFrom?.[grade] ?? [], date, '');
    return value === '' ? null : Number(value);
  });
}

/** The fiscal year (FY<yyyy>, by the calendar year its first day falls in) the date (yyyy-MM-dd) falls in,
 *  for a fiscal year starting on the first of the start month (1 to 12). */
export function fiscalYearOf(date: string, startMonth: number): string {
  const year = Number(date.slice(0, 4));
  const month = Number(date.slice(5, 7));
  return `FY${month >= startMonth ? year : year - 1}`;
}

/** Under the sheet's key, so loading the sheet again re-reads its settings. */
export const settingsKey = (sheet: string) => [...sheetKey(sheet), 'settings'] as const;

export function useSettings(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(settingsKey(sheet), asOf),
    queryFn: () => getJson<Settings>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/settings`, asOf)),
    enabled: sheet !== '',
  });
}
