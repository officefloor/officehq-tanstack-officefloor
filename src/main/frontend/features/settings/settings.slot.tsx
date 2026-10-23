import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { poolsKey } from '../pools/pools';
import { ratesKey } from '../rates/rates';
import { DEFAULT_FLOOR, DEFAULT_LATE_PCT, DEFAULT_TERMS, GRADES, PERM_ACTIONS, settingsKey, useSettings, type Settings } from './settings';

const LIMIT = /^\d+(\.\d{1,2})?$/;
const DAYS = /^\d{1,4}$/;
const WHOLE = /^-?\d{1,6}$/;
const PCT = /^\d{1,3}(\.\d{1,2})?$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;
const ROLES = /^\s*(clerk|steward)\s*(,\s*(clerk|steward)\s*)*$/;

/** The settings that carry a figure, and so are saved from a from-date. */
const DATED = ['clerk-limit', ...GRADES.map((grade) => `clerk-limit-${grade}`), 'floor', 'terms', ...GRADES.map((grade) => `terms-${grade}`),
  ...GRADES.map((grade) => `grace-${grade}`), 'late-pct', 'cap', 'proof-threshold',
  ...GRADES.map((grade) => `proof-threshold-${grade}`), 'note-threshold', 'rounding', 'review-threshold'];

/** The settings a save changes: only those typed into since the last save are sent (and recorded). */
type Changes = { clerkLimit?: string; floor?: string; terms?: string; latePct?: string; policyDate?: string;
  periodNumberingFrom?: string; gradeTerms?: Record<string, string>; cap?: string; proofThreshold?: string; noteThreshold?: string; from?: Record<string, string>;
  perms?: Record<string, string>; graceDays?: string; entryDate?: string; gradeGrace?: Record<string, string>;
  gradeProofThreshold?: Record<string, string>; pot?: string; parent?: string; fiscalStartMonth?: string; alertThreshold?: string;
  rounding?: string; reviewThreshold?: string; escalateAfter?: string; reviewSla?: string; proofKinds?: string;
  defaultGrade?: string; gradeClerkLimit?: Record<string, string>; scoreMid?: string; scoreHigh?: string; scoreLog?: string };

/** The sheet's settings — the clerk limit and the floor in force, and the form that saves them (a blank clerk
 *  limit: no limit). A clerk may settle only claims whose due is at or below the limit; the server enforces it.
 *  A grade may have a clerk limit of its own (blank: the sheet's); a claim whose person's grade on the sheet's today
 *  has one in force then takes it instead of the sheet's.
 *  A claim's live due is at least the floor; its due-by is the terms-th business day after its date (skipping Saturdays, Sundays and holidays); once overdue it gains, for every full 30
 *  days, the late percent in force on that boundary date of its figure. A claim dated before the policy date (blank: the sheet's today)
 *  rounds its live figures half up, one on or after it half to even. While the period numbering from is a date
 *  on or before the sheet's today, a claim settled is numbered within its period (<period>-<n>, X-<n> in none).
 *  A grade may have terms of its own (blank: the sheet's terms); a claim's terms are fixed when it is created, from
 *  the terms of its person's grade on its date. A grade may have grace days (blank: none); a claim whose person's grade
 *  on its date has a grace in force on that date is overdue only past its due-by plus them, and its late charge's
 *  30-day boundaries count from there; its due-by is unchanged. A person's due-total is at most the cap (blank: none).
 *  A claim whose due is above the proof threshold (blank: none) in force on its date cannot be settled or approved
 *  while it has no proofs; the server enforces it. A grade may have a proof threshold of its own (blank: the sheet's);
 *  a claim whose person's grade on its date has one in force on that date takes it instead of the sheet's. A claim whose due is above the note threshold (blank: none) in
 *  force on its date cannot be opened while its note is empty; the server enforces it. A claim whose due is above
 *  the review threshold (blank: none) in force on its date goes to REVIEW when opened; a steward reviews it to OPEN.
 *  The permission matrix gives, for each of open, settle, void, reassign, close and reopen, the roles that may take
 *  it (comma separated; blank: the standing rule); it is not dated, and the server enforces it.
 *  The grace days (blank: off; not dated): when the sheet's today is changed, each open period whose to-date plus
 *  them is before the new today is closed by the server.
 *  The entry date (blank: off; not dated): while it is set, a new claim given no date (the claim form, the quick-add
 *  bar, a pasted line, a correction) is dated it instead of the sheet's today.
 *  The pot (blank: none; not dated): each member of a balanced pool is shown their cut of it.
 *  The alert threshold (blank: off; not dated): the server alerts, in the audit file, each person whose due-total
 *  crosses above it over a change.
 *  The escalation gap (days; blank: off; not dated): reminding a claim whose previous reminder was at least that many
 *  days before the sheet's today escalates it, once per claim ever; the server records it in the audit file.
 *  The parent sheet (a sheet id; blank: none; not dated): a grade with no rate in force on a date on this sheet takes
 *  the parent's, then its parent's, up to three levels up. The server refuses the sheet itself, a sheet never
 *  loaded, or one that has this sheet as an ancestor.
 *  The fiscal year start month (1 to 12; 1 unless set; not dated): a fiscal year starts on the first day of it.
 *  The settings that carry a figure are dated: each is saved from a from-date (blank: the sheet's today), keeping
 *  the values already recorded; a claim takes the floor, late percent and terms in force on its date. What is shown
 *  is the value in force on the sheet's today. Typing a from-date alone saves the value shown from it. */
function SettingsSection({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const settings = useSettings(sheetId);
  const currentLimit = settings.data?.clerkLimit ?? '';
  const currentFloor = settings.data?.floor ?? DEFAULT_FLOOR;
  const currentTerms = settings.data?.terms ?? DEFAULT_TERMS;
  const currentLatePct = settings.data?.latePct ?? DEFAULT_LATE_PCT;
  const currentPolicyDate = settings.data?.policyDate ?? '';
  const currentNumberingFrom = settings.data?.periodNumberingFrom ?? '';
  const currentCap = settings.data?.cap ?? '';
  const currentProofThreshold = settings.data?.proofThreshold ?? '';
  const currentNoteThreshold = settings.data?.noteThreshold ?? '';
  const currentReviewThreshold = settings.data?.reviewThreshold ?? '';
  const currentGraceDays = settings.data?.graceDays ?? '';
  const currentEntryDate = settings.data?.entryDate ?? '';
  const currentPot = settings.data?.pot ?? '';
  const currentAlertThreshold = settings.data?.alertThreshold ?? '';
  const currentEscalateAfter = settings.data?.escalateAfter ?? '';
  const currentReviewSla = settings.data?.reviewSla ?? '';
  const currentScoreMid = settings.data?.scoreMid ?? '';
  const currentScoreHigh = settings.data?.scoreHigh ?? '';
  const currentScoreLog = settings.data?.scoreLog ?? '';
  const currentProofKinds = settings.data?.proofKinds ?? '';
  const currentDefaultGrade = settings.data?.defaultGrade ?? '';
  const currentRounding = settings.data?.rounding ?? '';
  const currentParent = settings.data?.parent ?? '';
  const currentFiscalStartMonth = settings.data?.fiscalStartMonth ?? '1';
  // null: not typed in since the last save, so the field shows the setting in force.
  const [typedLimit, setTypedLimit] = useState<string | null>(null);
  const [typedFloor, setTypedFloor] = useState<string | null>(null);
  const [typedRounding, setTypedRounding] = useState<string | null>(null);
  const [typedTerms, setTypedTerms] = useState<string | null>(null);
  const [typedLatePct, setTypedLatePct] = useState<string | null>(null);
  const [typedPolicyDate, setTypedPolicyDate] = useState<string | null>(null);
  const [typedNumberingFrom, setTypedNumberingFrom] = useState<string | null>(null);
  const [typedCap, setTypedCap] = useState<string | null>(null);
  const [typedProofThreshold, setTypedProofThreshold] = useState<string | null>(null);
  const [typedNoteThreshold, setTypedNoteThreshold] = useState<string | null>(null);
  const [typedReviewThreshold, setTypedReviewThreshold] = useState<string | null>(null);
  const [typedGraceDays, setTypedGraceDays] = useState<string | null>(null);
  const [typedEntryDate, setTypedEntryDate] = useState<string | null>(null);
  const [typedPot, setTypedPot] = useState<string | null>(null);
  const [typedAlertThreshold, setTypedAlertThreshold] = useState<string | null>(null);
  const [typedEscalateAfter, setTypedEscalateAfter] = useState<string | null>(null);
  const [typedReviewSla, setTypedReviewSla] = useState<string | null>(null);
  const [typedScoreMid, setTypedScoreMid] = useState<string | null>(null);
  const [typedScoreHigh, setTypedScoreHigh] = useState<string | null>(null);
  const [typedScoreLog, setTypedScoreLog] = useState<string | null>(null);
  const [typedProofKinds, setTypedProofKinds] = useState<string | null>(null);
  const [typedDefaultGrade, setTypedDefaultGrade] = useState<string | null>(null);
  const [typedParent, setTypedParent] = useState<string | null>(null);
  const [typedFiscalStartMonth, setTypedFiscalStartMonth] = useState<string | null>(null);
  const currentGradeTerms = (grade: string) => settings.data?.gradeTerms?.[grade] ?? '';
  const [typedGradeTerms, setTypedGradeTerms] = useState<Record<string, string>>({});
  const currentGradeGrace = (grade: string) => settings.data?.gradeGrace?.[grade] ?? '';
  const [typedGradeGrace, setTypedGradeGrace] = useState<Record<string, string>>({});
  const currentGradeClerkLimit = (grade: string) => settings.data?.gradeClerkLimit?.[grade] ?? '';
  const [typedGradeClerkLimit, setTypedGradeClerkLimit] = useState<Record<string, string>>({});
  const currentGradeProofThreshold = (grade: string) => settings.data?.gradeProofThreshold?.[grade] ?? '';
  const [typedGradeProofThreshold, setTypedGradeProofThreshold] = useState<Record<string, string>>({});
  const currentPerm = (action: string) => settings.data?.perms?.[action] ?? '';
  const [typedPerms, setTypedPerms] = useState<Record<string, string>>({});
  // From-dates typed in since the last save, by dated setting key.
  const [typedFrom, setTypedFrom] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (changes: Changes) =>
      putJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, { user, ...changes }),
    onSuccess: (saved) => {
      queryClient.setQueryData(settingsKey(sheetId), saved);
      void queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
      setTypedLimit(null);
      setTypedFloor(null);
      setTypedTerms(null);
      setTypedLatePct(null);
      setTypedPolicyDate(null);
      setTypedNumberingFrom(null);
      setTypedCap(null);
      setTypedProofThreshold(null);
      setTypedNoteThreshold(null);
      setTypedReviewThreshold(null);
      setTypedGraceDays(null);
      setTypedEntryDate(null);
      setTypedPot(null);
      setTypedAlertThreshold(null);
      setTypedEscalateAfter(null);
      setTypedReviewSla(null);
      setTypedScoreMid(null);
      setTypedScoreHigh(null);
      setTypedScoreLog(null);
      setTypedProofKinds(null);
      setTypedDefaultGrade(null);
      setTypedRounding(null);
      setTypedParent(null);
      setTypedFiscalStartMonth(null);
      setTypedGradeTerms({});
      setTypedGradeGrace({});
      setTypedGradeProofThreshold({});
      setTypedGradeClerkLimit({});
      setTypedPerms({});
      setTypedFrom({});
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body
        ? e.body
        : 'Could not save the settings: the clerk limit and floor must be amounts with up to two decimals, the terms whole days, the late percent a percent, the policy date and period numbering from dates or blank.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const changes: Changes = {};
    if (typedLimit !== null) {
      const limit = typedLimit.trim();
      if (limit !== '' && !LIMIT.test(limit)) {
        setError('The clerk limit must be an amount, 0.00 or more, with up to two decimals, or blank for no limit.');
        return;
      }
      changes.clerkLimit = limit;
    }
    if (typedFloor !== null) {
      const floor = typedFloor.trim();
      if (!LIMIT.test(floor)) {
        setError('The floor must be an amount, 0.00 or more, with up to two decimals.');
        return;
      }
      changes.floor = floor;
    }
    if (typedTerms !== null) {
      const terms = typedTerms.trim();
      if (!DAYS.test(terms)) {
        setError('The terms must be a whole number of business days, 0 or more.');
        return;
      }
      changes.terms = terms;
    }
    if (typedLatePct !== null) {
      const latePct = typedLatePct.trim();
      if (!PCT.test(latePct)) {
        setError('The late percent must be a percent, 0.00 or more, with up to two decimals.');
        return;
      }
      changes.latePct = latePct;
    }
    if (typedPolicyDate !== null) {
      const policyDate = typedPolicyDate.trim();
      if (policyDate !== '' && !DATE.test(policyDate)) {
        setError("The policy date must be a date, yyyy-mm-dd, or blank for the sheet's today.");
        return;
      }
      changes.policyDate = policyDate;
    }
    if (typedNumberingFrom !== null) {
      const numberingFrom = typedNumberingFrom.trim();
      if (numberingFrom !== '' && !DATE.test(numberingFrom)) {
        setError('The period numbering from must be a date, yyyy-mm-dd, or blank for off.');
        return;
      }
      changes.periodNumberingFrom = numberingFrom;
    }
    if (typedCap !== null) {
      const cap = typedCap.trim();
      if (cap !== '' && !LIMIT.test(cap)) {
        setError('The cap must be an amount, 0.00 or more, with up to two decimals, or blank for none.');
        return;
      }
      changes.cap = cap;
    }
    if (typedProofThreshold !== null) {
      const threshold = typedProofThreshold.trim();
      if (threshold !== '' && !LIMIT.test(threshold)) {
        setError('The proof threshold must be an amount, 0.00 or more, with up to two decimals, or blank for none.');
        return;
      }
      changes.proofThreshold = threshold;
    }
    if (typedNoteThreshold !== null) {
      const threshold = typedNoteThreshold.trim();
      if (threshold !== '' && !LIMIT.test(threshold)) {
        setError('The note threshold must be an amount, 0.00 or more, with up to two decimals, or blank for none.');
        return;
      }
      changes.noteThreshold = threshold;
    }
    if (typedReviewThreshold !== null) {
      const threshold = typedReviewThreshold.trim();
      if (threshold !== '' && !LIMIT.test(threshold)) {
        setError('The review threshold must be an amount, 0.00 or more, with up to two decimals, or blank for none.');
        return;
      }
      changes.reviewThreshold = threshold;
    }
    if (typedGraceDays !== null) {
      const graceDays = typedGraceDays.trim();
      if (graceDays !== '' && !DAYS.test(graceDays)) {
        setError('The grace days must be a whole number of days, 0 or more, or blank for off.');
        return;
      }
      changes.graceDays = graceDays;
    }
    if (typedEntryDate !== null) {
      const entryDate = typedEntryDate.trim();
      if (entryDate !== '' && !DATE.test(entryDate)) {
        setError('The entry date must be a date, yyyy-mm-dd, or blank for off.');
        return;
      }
      changes.entryDate = entryDate;
    }
    if (typedPot !== null) {
      const pot = typedPot.trim();
      if (pot !== '' && !LIMIT.test(pot)) {
        setError('The pot must be an amount, 0.00 or more, with up to two decimals, or blank for none.');
        return;
      }
      changes.pot = pot;
    }
    if (typedAlertThreshold !== null) {
      const threshold = typedAlertThreshold.trim();
      if (threshold !== '' && !LIMIT.test(threshold)) {
        setError('The alert threshold must be an amount, 0.00 or more, with up to two decimals, or blank for off.');
        return;
      }
      changes.alertThreshold = threshold;
    }
    if (typedEscalateAfter !== null) {
      const escalateAfter = typedEscalateAfter.trim();
      if (escalateAfter !== '' && !DAYS.test(escalateAfter)) {
        setError('The escalation gap must be a whole number of days, 0 or more, or blank for off.');
        return;
      }
      changes.escalateAfter = escalateAfter;
    }
    if (typedReviewSla !== null) {
      const reviewSla = typedReviewSla.trim();
      if (reviewSla !== '' && !DAYS.test(reviewSla)) {
        setError('The review SLA must be a whole number of days, 0 or more, or blank for none.');
        return;
      }
      changes.reviewSla = reviewSla;
    }
    if (typedScoreMid !== null) {
      const scoreMid = typedScoreMid.trim();
      if (scoreMid !== '' && !WHOLE.test(scoreMid)) {
        setError('The mid score threshold must be a whole number, or blank for none.');
        return;
      }
      changes.scoreMid = scoreMid;
    }
    if (typedScoreHigh !== null) {
      const scoreHigh = typedScoreHigh.trim();
      if (scoreHigh !== '' && !WHOLE.test(scoreHigh)) {
        setError('The high score threshold must be a whole number, or blank for none.');
        return;
      }
      changes.scoreHigh = scoreHigh;
    }
    if (typedScoreLog !== null) {
      const scoreLog = typedScoreLog.trim();
      if (scoreLog !== '' && scoreLog !== 'on') {
        setError('The score log must be on, or blank for off.');
        return;
      }
      changes.scoreLog = scoreLog;
    }
    if (typedProofKinds !== null) {
      const proofKinds = typedProofKinds.trim();
      if (proofKinds !== '' && !proofKinds.split(',').every((k) => k.trim() === 'receipt' || k.trim() === 'photo')) {
        setError('The proof kinds must be a comma separated list of receipt and photo, or blank for none.');
        return;
      }
      changes.proofKinds = proofKinds;
    }
    if (typedDefaultGrade !== null) {
      const defaultGrade = typedDefaultGrade.trim();
      if (defaultGrade !== '' && !(GRADES as readonly string[]).includes(defaultGrade)) {
        setError('The default grade must be A, B or C, or blank for A.');
        return;
      }
      changes.defaultGrade = defaultGrade;
    }
    if (typedRounding !== null) {
      const rounding = typedRounding.trim();
      if (rounding !== '' && rounding !== 'half-up' && rounding !== 'half-even') {
        setError('The rounding mode must be half-up or half-even, or blank for none.');
        return;
      }
      changes.rounding = rounding;
    }
    if (typedParent !== null) {
      changes.parent = typedParent.trim();
    }
    if (typedFiscalStartMonth !== null) {
      const month = typedFiscalStartMonth.trim();
      if (!/^([1-9]|1[0-2])$/.test(month)) {
        setError('The fiscal year start month must be a month, 1 to 12.');
        return;
      }
      changes.fiscalStartMonth = month;
    }
    for (const grade of GRADES) {
      const typedTerms = typedGradeTerms[grade];
      if (typedTerms === undefined) continue;
      const terms = typedTerms.trim();
      if (terms !== '' && !DAYS.test(terms)) {
        setError(`The terms of grade ${grade} must be a whole number of business days, 0 or more, or blank for the sheet's terms.`);
        return;
      }
      changes.gradeTerms = { ...changes.gradeTerms, [grade]: terms };
    }
    for (const grade of GRADES) {
      const typedGrace = typedGradeGrace[grade];
      if (typedGrace === undefined) continue;
      const grace = typedGrace.trim();
      if (grace !== '' && !DAYS.test(grace)) {
        setError(`The grace of grade ${grade} must be a whole number of days, 0 or more, or blank for none.`);
        return;
      }
      changes.gradeGrace = { ...changes.gradeGrace, [grade]: grace };
    }
    for (const grade of GRADES) {
      const typedThreshold = typedGradeProofThreshold[grade];
      if (typedThreshold === undefined) continue;
      const threshold = typedThreshold.trim();
      if (threshold !== '' && !LIMIT.test(threshold)) {
        setError(`The proof threshold of grade ${grade} must be an amount, 0.00 or more, with up to two decimals, or blank for the sheet's.`);
        return;
      }
      changes.gradeProofThreshold = { ...changes.gradeProofThreshold, [grade]: threshold };
    }
    for (const grade of GRADES) {
      const typedGradeLimit = typedGradeClerkLimit[grade];
      if (typedGradeLimit === undefined) continue;
      const limit = typedGradeLimit.trim();
      if (limit !== '' && !LIMIT.test(limit)) {
        setError(`The clerk limit of grade ${grade} must be an amount, 0.00 or more, with up to two decimals, or blank for the sheet's.`);
        return;
      }
      changes.gradeClerkLimit = { ...changes.gradeClerkLimit, [grade]: limit };
    }
    for (const action of PERM_ACTIONS) {
      const typedRoles = typedPerms[action];
      if (typedRoles === undefined) continue;
      const roles = typedRoles.trim();
      if (roles !== '' && !ROLES.test(roles)) {
        setError(`The roles allowed to ${action} must be a comma separated list of clerk and steward, or blank for the standing rule.`);
        return;
      }
      changes.perms = { ...changes.perms, [action]: roles };
    }
    // A dated setting with a from-date typed: saved from it, with the value typed, else the value in force.
    const shownValue: Record<string, () => void> = {
      'clerk-limit': () => { changes.clerkLimit ??= currentLimit; },
      floor: () => { changes.floor ??= currentFloor; },
      terms: () => { changes.terms ??= currentTerms; },
      'late-pct': () => { changes.latePct ??= currentLatePct; },
      cap: () => { changes.cap ??= currentCap; },
      'proof-threshold': () => { changes.proofThreshold ??= currentProofThreshold; },
      'note-threshold': () => { changes.noteThreshold ??= currentNoteThreshold; },
      'review-threshold': () => { changes.reviewThreshold ??= currentReviewThreshold; },
      rounding: () => { changes.rounding ??= currentRounding; },
      ...Object.fromEntries(GRADES.map((grade) => [`terms-${grade}`, () => {
        if (changes.gradeTerms?.[grade] === undefined) {
          changes.gradeTerms = { ...changes.gradeTerms, [grade]: currentGradeTerms(grade) };
        }
      }])),
      ...Object.fromEntries(GRADES.map((grade) => [`grace-${grade}`, () => {
        if (changes.gradeGrace?.[grade] === undefined) {
          changes.gradeGrace = { ...changes.gradeGrace, [grade]: currentGradeGrace(grade) };
        }
      }])),
      ...Object.fromEntries(GRADES.map((grade) => [`clerk-limit-${grade}`, () => {
        if (changes.gradeClerkLimit?.[grade] === undefined) {
          changes.gradeClerkLimit = { ...changes.gradeClerkLimit, [grade]: currentGradeClerkLimit(grade) };
        }
      }])),
      ...Object.fromEntries(GRADES.map((grade) => [`proof-threshold-${grade}`, () => {
        if (changes.gradeProofThreshold?.[grade] === undefined) {
          changes.gradeProofThreshold = { ...changes.gradeProofThreshold, [grade]: currentGradeProofThreshold(grade) };
        }
      }])),
    };
    for (const key of DATED) {
      const from = (typedFrom[key] ?? '').trim();
      if (from === '') continue;
      if (!DATE.test(from)) {
        setError("A from-date must be a date, yyyy-mm-dd, or blank for the sheet's today.");
        return;
      }
      shownValue[key]();
      changes.from = { ...changes.from, [key]: from };
    }
    if (!once.start()) return;
    save.mutate(changes);
  };

  // onInput as well as onChange, and focusing the field: retyping (or clearing) the value in force still counts
  // as setting it.
  const typed = (set: (v: string) => void) => (e: FormEvent<HTMLInputElement>) => {
    set(e.currentTarget.value);
    setError('');
  };

  const fromInput = (key: string) => (
    <label>
      {' '}from{' '}
      <input
        data-testid={`setting-${key}-from`}
        placeholder="today"
        size={10}
        value={typedFrom[key] ?? ''}
        onInput={typed((v) => setTypedFrom((t) => ({ ...t, [key]: v })))}
        onChange={typed((v) => setTypedFrom((t) => ({ ...t, [key]: v })))}
      />
    </label>
  );

  return (
    <section data-testid="settings">
      <h2>Settings</h2>
      <p>
        Clerk limit <strong data-testid="setting-clerk-limit-current">{currentLimit}</strong>{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            Grade {grade} clerk limit <strong data-testid={`setting-clerk-limit-${grade}-current`}>{currentGradeClerkLimit(grade)}</strong>{' '}
          </span>
        ))}
        Floor <strong data-testid="setting-floor-current">{currentFloor}</strong>{' '}
        Terms <strong data-testid="setting-terms-current">{currentTerms}</strong> business days{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            Grade {grade} terms <strong data-testid={`setting-terms-${grade}-current`}>{currentGradeTerms(grade)}</strong>{' '}
          </span>
        ))}
        {GRADES.map((grade) => (
          <span key={grade}>
            Grade {grade} grace <strong data-testid={`setting-grace-${grade}-current`}>{currentGradeGrace(grade)}</strong>{' '}
          </span>
        ))}
        Late <strong data-testid="setting-late-pct-current">{currentLatePct}</strong>%{' '}
        Policy date <strong data-testid="setting-policy-date-current">{currentPolicyDate}</strong>{' '}
        Period numbering from <strong data-testid="setting-period-numbering-from-current">{currentNumberingFrom}</strong>{' '}
        Cap <strong data-testid="setting-cap-current">{currentCap}</strong>{' '}
        Proof threshold <strong data-testid="setting-proof-threshold-current">{currentProofThreshold}</strong>{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            Grade {grade} proof threshold <strong data-testid={`setting-proof-threshold-${grade}-current`}>{currentGradeProofThreshold(grade)}</strong>{' '}
          </span>
        ))}
        Note threshold <strong data-testid="setting-note-threshold-current">{currentNoteThreshold}</strong>{' '}
        Review threshold <strong data-testid="setting-review-threshold-current">{currentReviewThreshold}</strong>{' '}
        Grace days <strong data-testid="setting-grace-days-current">{currentGraceDays}</strong>{' '}
        Entry date <strong data-testid="setting-entry-date-current">{currentEntryDate}</strong>{' '}
        Pot <strong data-testid="setting-pot-current">{currentPot}</strong>{' '}
        Alert threshold <strong data-testid="setting-alert-threshold-current">{currentAlertThreshold}</strong>{' '}
        Escalate after <strong data-testid="setting-escalate-after-current">{currentEscalateAfter}</strong>{' '}
        Review SLA <strong data-testid="setting-review-sla-current">{currentReviewSla}</strong>{' '}
        Score mid <strong data-testid="setting-score-mid-current">{currentScoreMid}</strong>{' '}
        Score high <strong data-testid="setting-score-high-current">{currentScoreHigh}</strong>{' '}
        Score log <strong data-testid="setting-score-log-current">{currentScoreLog}</strong>{' '}
        Proof kinds <strong data-testid="setting-proof-kinds-current">{currentProofKinds}</strong>{' '}
        Default grade <strong data-testid="setting-default-grade-current">{currentDefaultGrade}</strong>{' '}
        Rounding <strong data-testid="setting-rounding-current">{currentRounding}</strong>{' '}
        Parent <strong data-testid="setting-parent-current">{currentParent}</strong>{' '}
        Fiscal year start month <strong data-testid="setting-fiscal-start-month-current">{currentFiscalStartMonth}</strong>
      </p>
      <p>
        {PERM_ACTIONS.map((action) => (
          <span key={action}>
            May {action} <strong data-testid={`setting-perm-${action}-current`}>{currentPerm(action)}</strong>{' '}
          </span>
        ))}
      </p>
      <form data-testid="settings-form" onSubmit={submit}>
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            placeholder="no limit"
            value={typedLimit ?? currentLimit}
            onInput={typed(setTypedLimit)}
            onChange={typed(setTypedLimit)}
            onFocus={() => setTypedLimit((v) => v ?? currentLimit)}
          />
        </label>
        {fromInput('clerk-limit')}{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            <label>
              Grade {grade} clerk limit{' '}
              <input
                data-testid={`setting-clerk-limit-${grade}`}
                placeholder="sheet's"
                value={typedGradeClerkLimit[grade] ?? currentGradeClerkLimit(grade)}
                onInput={typed((v) => setTypedGradeClerkLimit((t) => ({ ...t, [grade]: v })))}
                onChange={typed((v) => setTypedGradeClerkLimit((t) => ({ ...t, [grade]: v })))}
                onFocus={() => setTypedGradeClerkLimit((t) => (grade in t ? t : { ...t, [grade]: currentGradeClerkLimit(grade) }))}
              />
            </label>
            {fromInput(`clerk-limit-${grade}`)}{' '}
          </span>
        ))}
        <label>
          Floor{' '}
          <input
            data-testid="setting-floor"
            value={typedFloor ?? currentFloor}
            onInput={typed(setTypedFloor)}
            onChange={typed(setTypedFloor)}
            onFocus={() => setTypedFloor((v) => v ?? currentFloor)}
          />
        </label>
        {fromInput('floor')}{' '}
        <label>
          Terms (business days){' '}
          <input
            data-testid="setting-terms"
            value={typedTerms ?? currentTerms}
            onInput={typed(setTypedTerms)}
            onChange={typed(setTypedTerms)}
            onFocus={() => setTypedTerms((v) => v ?? currentTerms)}
          />
        </label>
        {fromInput('terms')}{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            <label>
              Grade {grade} terms{' '}
              <input
                data-testid={`setting-terms-${grade}`}
                placeholder="sheet's terms"
                value={typedGradeTerms[grade] ?? currentGradeTerms(grade)}
                onInput={typed((v) => setTypedGradeTerms((t) => ({ ...t, [grade]: v })))}
                onChange={typed((v) => setTypedGradeTerms((t) => ({ ...t, [grade]: v })))}
                onFocus={() => setTypedGradeTerms((t) => (grade in t ? t : { ...t, [grade]: currentGradeTerms(grade) }))}
              />
            </label>
            {fromInput(`terms-${grade}`)}{' '}
          </span>
        ))}
        {GRADES.map((grade) => (
          <span key={grade}>
            <label>
              Grade {grade} grace days{' '}
              <input
                data-testid={`setting-grace-${grade}`}
                placeholder="none"
                value={typedGradeGrace[grade] ?? currentGradeGrace(grade)}
                onInput={typed((v) => setTypedGradeGrace((t) => ({ ...t, [grade]: v })))}
                onChange={typed((v) => setTypedGradeGrace((t) => ({ ...t, [grade]: v })))}
                onFocus={() => setTypedGradeGrace((t) => (grade in t ? t : { ...t, [grade]: currentGradeGrace(grade) }))}
              />
            </label>
            {fromInput(`grace-${grade}`)}{' '}
          </span>
        ))}
        <label>
          Late percent{' '}
          <input
            data-testid="setting-late-pct"
            value={typedLatePct ?? currentLatePct}
            onInput={typed(setTypedLatePct)}
            onChange={typed(setTypedLatePct)}
            onFocus={() => setTypedLatePct((v) => v ?? currentLatePct)}
          />
        </label>
        {fromInput('late-pct')}{' '}
        <label>
          Policy date{' '}
          <input
            data-testid="setting-policy-date"
            placeholder="today"
            value={typedPolicyDate ?? currentPolicyDate}
            onInput={typed(setTypedPolicyDate)}
            onChange={typed(setTypedPolicyDate)}
            onFocus={() => setTypedPolicyDate((v) => v ?? currentPolicyDate)}
          />
        </label>{' '}
        <label>
          Period numbering from{' '}
          <input
            data-testid="setting-period-numbering-from"
            placeholder="off"
            value={typedNumberingFrom ?? currentNumberingFrom}
            onInput={typed(setTypedNumberingFrom)}
            onChange={typed(setTypedNumberingFrom)}
            onFocus={() => setTypedNumberingFrom((v) => v ?? currentNumberingFrom)}
          />
        </label>{' '}
        <label>
          Cap{' '}
          <input
            data-testid="setting-cap"
            placeholder="none"
            value={typedCap ?? currentCap}
            onInput={typed(setTypedCap)}
            onChange={typed(setTypedCap)}
            onFocus={() => setTypedCap((v) => v ?? currentCap)}
          />
        </label>
        {fromInput('cap')}{' '}
        <label>
          Proof threshold{' '}
          <input
            data-testid="setting-proof-threshold"
            placeholder="none"
            value={typedProofThreshold ?? currentProofThreshold}
            onInput={typed(setTypedProofThreshold)}
            onChange={typed(setTypedProofThreshold)}
            onFocus={() => setTypedProofThreshold((v) => v ?? currentProofThreshold)}
          />
        </label>
        {fromInput('proof-threshold')}{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            <label>
              Grade {grade} proof threshold{' '}
              <input
                data-testid={`setting-proof-threshold-${grade}`}
                placeholder="sheet's"
                value={typedGradeProofThreshold[grade] ?? currentGradeProofThreshold(grade)}
                onInput={typed((v) => setTypedGradeProofThreshold((t) => ({ ...t, [grade]: v })))}
                onChange={typed((v) => setTypedGradeProofThreshold((t) => ({ ...t, [grade]: v })))}
                onFocus={() => setTypedGradeProofThreshold((t) => (grade in t ? t : { ...t, [grade]: currentGradeProofThreshold(grade) }))}
              />
            </label>
            {fromInput(`proof-threshold-${grade}`)}{' '}
          </span>
        ))}
        <label>
          Note threshold{' '}
          <input
            data-testid="setting-note-threshold"
            placeholder="none"
            value={typedNoteThreshold ?? currentNoteThreshold}
            onInput={typed(setTypedNoteThreshold)}
            onChange={typed(setTypedNoteThreshold)}
            onFocus={() => setTypedNoteThreshold((v) => v ?? currentNoteThreshold)}
          />
        </label>
        {fromInput('note-threshold')}{' '}
        <label>
          Review threshold{' '}
          <input
            data-testid="setting-review-threshold"
            placeholder="none"
            value={typedReviewThreshold ?? currentReviewThreshold}
            onInput={typed(setTypedReviewThreshold)}
            onChange={typed(setTypedReviewThreshold)}
            onFocus={() => setTypedReviewThreshold((v) => v ?? currentReviewThreshold)}
          />
        </label>
        {fromInput('review-threshold')}{' '}
        <label>
          Grace days{' '}
          <input
            data-testid="setting-grace-days"
            placeholder="off"
            value={typedGraceDays ?? currentGraceDays}
            onInput={typed(setTypedGraceDays)}
            onChange={typed(setTypedGraceDays)}
            onFocus={() => setTypedGraceDays((v) => v ?? currentGraceDays)}
          />
        </label>{' '}
        <label>
          Entry date{' '}
          <input
            data-testid="setting-entry-date"
            placeholder="off"
            value={typedEntryDate ?? currentEntryDate}
            onInput={typed(setTypedEntryDate)}
            onChange={typed(setTypedEntryDate)}
            onFocus={() => setTypedEntryDate((v) => v ?? currentEntryDate)}
          />
        </label>{' '}
        <label>
          Pot{' '}
          <input
            data-testid="setting-pot"
            placeholder="none"
            value={typedPot ?? currentPot}
            onInput={typed(setTypedPot)}
            onChange={typed(setTypedPot)}
            onFocus={() => setTypedPot((v) => v ?? currentPot)}
          />
        </label>{' '}
        <label>
          Alert threshold{' '}
          <input
            data-testid="setting-alert-threshold"
            placeholder="off"
            value={typedAlertThreshold ?? currentAlertThreshold}
            onInput={typed(setTypedAlertThreshold)}
            onChange={typed(setTypedAlertThreshold)}
            onFocus={() => setTypedAlertThreshold((v) => v ?? currentAlertThreshold)}
          />
        </label>{' '}
        <label>
          Escalate after{' '}
          <input
            data-testid="setting-escalate-after"
            placeholder="off"
            value={typedEscalateAfter ?? currentEscalateAfter}
            onInput={typed(setTypedEscalateAfter)}
            onChange={typed(setTypedEscalateAfter)}
            onFocus={() => setTypedEscalateAfter((v) => v ?? currentEscalateAfter)}
          />{' '}
          days
        </label>{' '}
        <label>
          Review SLA{' '}
          <input
            data-testid="setting-review-sla"
            placeholder="none"
            value={typedReviewSla ?? currentReviewSla}
            onInput={typed(setTypedReviewSla)}
            onChange={typed(setTypedReviewSla)}
            onFocus={() => setTypedReviewSla((v) => v ?? currentReviewSla)}
          />{' '}
          days
        </label>{' '}
        <label>
          Score mid{' '}
          <input
            data-testid="setting-score-mid"
            placeholder="none"
            value={typedScoreMid ?? currentScoreMid}
            onInput={typed(setTypedScoreMid)}
            onChange={typed(setTypedScoreMid)}
            onFocus={() => setTypedScoreMid((v) => v ?? currentScoreMid)}
          />
        </label>{' '}
        <label>
          Score high{' '}
          <input
            data-testid="setting-score-high"
            placeholder="none"
            value={typedScoreHigh ?? currentScoreHigh}
            onInput={typed(setTypedScoreHigh)}
            onChange={typed(setTypedScoreHigh)}
            onFocus={() => setTypedScoreHigh((v) => v ?? currentScoreHigh)}
          />
        </label>{' '}
        <label>
          Score log{' '}
          <input
            data-testid="setting-score-log"
            placeholder="off (on)"
            value={typedScoreLog ?? currentScoreLog}
            onInput={typed(setTypedScoreLog)}
            onChange={typed(setTypedScoreLog)}
            onFocus={() => setTypedScoreLog((v) => v ?? currentScoreLog)}
          />
        </label>{' '}
        <label>
          Proof kinds{' '}
          <input
            data-testid="setting-proof-kinds"
            placeholder="none (receipt,photo)"
            value={typedProofKinds ?? currentProofKinds}
            onInput={typed(setTypedProofKinds)}
            onChange={typed(setTypedProofKinds)}
            onFocus={() => setTypedProofKinds((v) => v ?? currentProofKinds)}
          />
        </label>{' '}
        <label>
          Default grade{' '}
          <input
            data-testid="setting-default-grade"
            placeholder="A"
            value={typedDefaultGrade ?? currentDefaultGrade}
            onInput={typed(setTypedDefaultGrade)}
            onChange={typed(setTypedDefaultGrade)}
            onFocus={() => setTypedDefaultGrade((v) => v ?? currentDefaultGrade)}
          />
        </label>{' '}
        <label>
          Rounding{' '}
          <input
            data-testid="setting-rounding"
            placeholder="none"
            value={typedRounding ?? currentRounding}
            onInput={typed(setTypedRounding)}
            onChange={typed(setTypedRounding)}
            onFocus={() => setTypedRounding((v) => v ?? currentRounding)}
          />
        </label>
        {fromInput('rounding')}{' '}
        <label>
          Parent sheet{' '}
          <input
            data-testid="setting-parent"
            placeholder="none"
            value={typedParent ?? currentParent}
            onInput={typed(setTypedParent)}
            onChange={typed(setTypedParent)}
            onFocus={() => setTypedParent((v) => v ?? currentParent)}
          />
        </label>{' '}
        <label>
          Fiscal year start month{' '}
          <input
            data-testid="setting-fiscal-start-month"
            placeholder="1 to 12"
            value={typedFiscalStartMonth ?? currentFiscalStartMonth}
            onInput={typed(setTypedFiscalStartMonth)}
            onChange={typed(setTypedFiscalStartMonth)}
            onFocus={() => setTypedFiscalStartMonth((v) => v ?? currentFiscalStartMonth)}
          />
        </label>{' '}
        {PERM_ACTIONS.map((action) => (
          <span key={action}>
            <label>
              May {action}{' '}
              <input
                data-testid={`setting-perm-${action}`}
                placeholder="standing rule"
                value={typedPerms[action] ?? currentPerm(action)}
                onInput={typed((v) => setTypedPerms((t) => ({ ...t, [action]: v })))}
                onChange={typed((v) => setTypedPerms((t) => ({ ...t, [action]: v })))}
                onFocus={() => setTypedPerms((t) => (action in t ? t : { ...t, [action]: currentPerm(action) }))}
              />
            </label>{' '}
          </span>
        ))}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {error && (
          <p data-testid="settings-error" role="alert">
            {error}
          </p>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7, Component: SettingsSection });
