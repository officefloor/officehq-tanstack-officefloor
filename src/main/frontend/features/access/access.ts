import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Dated, Late } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';

/**
 * A sheet's settings: its clerk limit, with two decimals ('' for no limit), and its floor, the
 * least a claim's live due may be, with two decimals, and its terms, the whole number of days after a
 * claim's date that it is due by, and its late percent, what an overdue claim's due gains per full 30
 * days overdue, with two decimals, and its policy date (yyyy-MM-dd; '' for the sheet's today): a claim
 * dated before it rounds half up, one on or after it half to even, and its period numbering from
 * (yyyy-MM-dd; '' for off): while the sheet's today is on or after it, a claim opened is numbered
 * within its period.
 */
export type Settings = {
  clerkLimit: string;
  floor: string;
  terms: string;
  latePct: string;
  policyDate: string;
  periodNumberingFrom: string;
  /** Each grade's own terms in days ('' for the sheet's terms). */
  gradeTerms: Record<string, string>;
  /** The most a person's due-total may be, with two decimals ('' for none). */
  cap: string;
  /** The due above which a claim, on its date, needs a proof to be settled or approved ('' for none). */
  proofThreshold: string;
  /** Each action's entry in the permission matrix, the roles that may take it, comma separated ('' for the standing rule). */
  perms: Record<string, string>;
  /** Grace days, a whole number ('' for off): when today is changed, every open period whose to-date plus them is before it closes. */
  graceDays: string;
  /** The due above which a claim, on its date, needs a note to be opened ('' for none). */
  noteThreshold: string;
  /** The entry date (yyyy-MM-dd; '' for off): while set, a new claim left undated takes it instead of the sheet's today. */
  entryDate: string;
  /** Each grade's grace days ('' for none): a claim of the grade is overdue only past its due-by plus them. */
  gradeGrace?: Record<string, string>;
  /** Each grade's own proof threshold ('' for none: a claim of the grade takes the sheet's). */
  gradeProofThreshold?: Record<string, string>;
  /** Each grade's own clerk limit ('' for none: a claim of the grade takes the sheet's). */
  gradeClerkLimit?: Record<string, string>;
  /** The pot, with two decimals ('' for none), not dated: shared out as cuts among a balanced pool's members. */
  pot: string;
  /** The parent sheet's id ('' for none), not dated: a grade with no rate in force on a date takes the parent's, up to three levels up. */
  parent: string;
  /** The fiscal year start month, 1 to 12 (1 unless set), not dated: a fiscal year FY<yyyy> starts on its first day in year yyyy. */
  fiscalStartMonth: string;
  /** The alert threshold, with two decimals ('' for off), not dated: a person whose due-total crosses above it is alerted. */
  alertThreshold: string;
  /** The rounding mode, half-up or half-even ('' for none: by the policy date), dated: a claim rounds by the one in force on its date. */
  rounding: string;
  /** The due above which a claim, on its date, goes to REVIEW instead of OPEN when it is opened ('' for none), dated. */
  reviewThreshold: string;
  /** The escalation gap in days ('' for off), not dated: a reminder at least that many days after the claim's previous one escalates it, once. */
  escalateAfter: string;
  /** The review SLA in days ('' for none), not dated: a REVIEW claim whose review day plus it is before the sheet's today is late. */
  reviewSla: string;
  /** The proof kinds, a comma separated list of kinds, receipt or photo ('' for none), not dated: while set, a claim above the proof threshold needs a proof of each. */
  proofKinds: string;
  /** The default grade, one of the grades ('' for A), not dated: the grade a quick-added person and an imported line with a blank grade take. */
  defaultGrade: string;
  /** The mid score threshold, a whole number ('' for none), not dated: while both thresholds are set, a score at or above it is MID. */
  scoreMid: string;
  /** The high score threshold, a whole number ('' for none), not dated: while both thresholds are set, a score at or above it is HIGH. */
  scoreHigh: string;
  /** The score log switch, on ('' for off), not dated: while on, changing the sheet's today writes a SCORE line per person. */
  scoreLog: string;
  /** The maximum of open claims per person, a whole number ('' for none), not dated: opening a claim is refused while its person already has that many OPEN or PENDING. */
  maxOpen: string;
  /** The pending SLA in days ('' for none), not dated: a PENDING claim whose pending day plus it is before the sheet's today is late. */
  pendingSla: string;
  /**
   * Every value recorded for each figure setting, by key (clerk-limit, floor, terms, late-pct,
   * terms-A, terms-B, terms-C, cap, proof-threshold, note-threshold, grace-A, grace-B, grace-C, proof-threshold-A, proof-threshold-B,
   * proof-threshold-C, rounding, review-threshold, clerk-limit-A, clerk-limit-B, clerk-limit-C), in from-date order ('' for none). The values above are those
   * in force on the sheet's today.
   */
  dated?: Record<string, { from: string; value: string }[]>;
};

/** The figure settings, which are saved from a from-date, by the key the screen and the store use. */
export const DATED_KEYS = {
  clerkLimit: 'clerk-limit',
  floor: 'floor',
  terms: 'terms',
  latePct: 'late-pct',
  cap: 'cap',
  proofThreshold: 'proof-threshold',
  noteThreshold: 'note-threshold',
  rounding: 'rounding',
  reviewThreshold: 'review-threshold',
} as const;

/** The grades that may have their own terms. */
export const GRADES = ['A', 'B', 'C'] as const;

/** The key of a grade's grace days, dated like the floor. */
export const gradeGraceKey = (grade: string) => `grace-${grade}`;

/** The key of a grade's own proof threshold, dated like the floor. */
export const gradeProofThresholdKey = (grade: string) => `proof-threshold-${grade}`;

/** The key of a grade's own clerk limit, dated like the floor. */
export const gradeClerkLimitKey = (grade: string) => `clerk-limit-${grade}`;

/** The actions of the permission matrix, in the order they are shown. */
export const ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen', 'today'] as const;

/** Who may take each action while its entry is blank. */
export const STANDING_RULE: Record<string, string> = {
  open: 'clerk,steward',
  settle: 'clerk,steward',
  void: 'clerk,steward',
  reassign: 'steward',
  close: 'clerk,steward',
  reopen: 'steward',
  today: 'clerk,steward',
};

/** A save of the settings: only those being changed are sent. */
export type SettingsChange = Partial<Omit<Settings, 'dated'>> & { from?: Record<string, string> };

/**
 * A user's role on a sheet. A name with no role assigned is a clerk; a viewer sees everything and changes nothing;
 * a restricted user acts as a clerk but sees on the people list only the people of the crews they lead or
 * belong to (as the person with their name) and that person.
 */
export type Role = { user: string; role: string };

export const ROLES = ['clerk', 'steward', 'viewer', 'restricted'] as const;

/** The roles the permission matrix may name: a viewer may take no action. */
export const PERM_ROLES = ['clerk', 'steward'] as const;

export const settingsKey = (sheetId: string) => ['sheet', sheetId, 'settings'] as const;
export const rolesKey = (sheetId: string) => ['sheet', sheetId, 'roles'] as const;

export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}

/**
 * The sheet's floor, dated: 1.00 before every value recorded, each claim taking the one in force on
 * its date ('0.00', no floor, until the settings are read).
 */
export function useFloor(sheetId: string): Dated {
  return floorOf(useSettings(sheetId).data);
}

/** The floor of settings as read (undefined until they are): as `useFloor`. */
export function floorOf(settings: Settings | undefined): Dated {
  if (!settings) return { start: '0.00', values: [] };
  return { start: '1.00', values: settings.dated?.floor ?? [] };
}

/**
 * What the sheet's late charge and rounding need: its late percent ('0.00' until read),
 * today and policy date ('' until read).
 */
export function useLate(sheetId: string): Late {
  return lateOf(useSettings(sheetId).data, useSheetToday(sheetId));
}

/** The late charge of settings as read (undefined until they are) on the sheet's today: as `useLate`. */
export function lateOf(settings: Settings | undefined, today: string): Late {
  const pct: Dated = { start: '0.00', values: settings?.dated?.['late-pct'] ?? [] };
  const grace = Object.fromEntries(
    GRADES.map((g) => [g, { start: '', values: settings?.dated?.[gradeGraceKey(g)] ?? [] }]),
  );
  const rounding: Dated = { start: '', values: settings?.dated?.rounding ?? [] };
  return { pct, today, policyDate: settings?.policyDate ?? '', grace, rounding };
}

/** The sheet's cap on a person's due-total ('' for none, until the settings are read). */
export function useCap(sheetId: string): string {
  return useSettings(sheetId).data?.cap ?? '';
}

/** The sheet's review SLA in days ('' for none, until the settings are read). */
export function useReviewSla(sheetId: string): string {
  return useSettings(sheetId).data?.reviewSla ?? '';
}

/** The sheet's pending SLA in days ('' for none, until the settings are read). */
export function usePendingSla(sheetId: string): string {
  return useSettings(sheetId).data?.pendingSla ?? '';
}

/** The sheet's score thresholds, mid and high ('' for none, until the settings are read). */
export function useScoreThresholds(sheetId: string): { mid: string; high: string } {
  const data = useSettings(sheetId).data;
  return { mid: data?.scoreMid ?? '', high: data?.scoreHigh ?? '' };
}

/** The roles assigned on the sheet, by user name. */
export function useRoles(sheetId: string) {
  return useQuery({
    queryKey: rolesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ roles: Role[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`)).roles,
  });
}

/** A clerk limit as the screen takes it: blank, or 0.00 or more with up to two decimals. */
export const LIMIT = /^\d+(\.\d{1,2})?$/;

/** A cap as the screen takes it: blank, or 0.00 or more with up to two decimals. */
export const CAP = /^\d+(\.\d{1,2})?$/;
export const POT = /^\d+(\.\d{1,2})?$/;

/** An alert threshold as the screen takes it: blank, or 0.00 or more with up to two decimals. */
export const ALERT_THRESHOLD = /^\d+(\.\d{1,2})?$/;

/** A proof threshold as the screen takes it: blank, or 0.00 or more with up to two decimals. */
export const PROOF_THRESHOLD = /^\d+(\.\d{1,2})?$/;

/** A note threshold as the screen takes it: blank, or 0.00 or more with up to two decimals. */
export const NOTE_THRESHOLD = /^\d+(\.\d{1,2})?$/;

/** The kinds a proof may have, in the order they are shown: a proof with none chosen is a receipt. */
export const PROOF_KINDS: readonly string[] = ['receipt', 'photo'];

/** A review threshold as the screen takes it: blank, or 0.00 or more with up to two decimals. */
export const REVIEW_THRESHOLD = /^\d+(\.\d{1,2})?$/;

/** A rounding mode as the screen takes it: blank, half-up or half-even. */
export const ROUNDING = /^(half-up|half-even)$/;

/** A floor as the screen takes it: 0.00 or more with up to two decimals. */
export const FLOOR = /^\d+(\.\d{1,2})?$/;

/** A late percent as the screen takes it: 0.00 or more with up to two decimals. */
export const LATE_PCT = /^\d+(\.\d{1,2})?$/;

/** A policy date as the screen takes it: blank, or a date yyyy-MM-dd. */
export const POLICY_DATE = /^\d{4}-\d{2}-\d{2}$/;

/** Terms as the screen takes them: a whole number of days, 0 or more. */
export const TERMS = /^\d{1,5}$/;
