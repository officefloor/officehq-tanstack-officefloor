import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useGradeCodes } from '../grades/useGrades';
import { poolsKey } from '../pools/usePools';
import { inForce, PERM_ACTIONS, settingsKey, useSettings, type Settings } from './useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { PolicyViewer } from './PolicyViewer';
import { PolicyExport } from './PolicyExport';

// The sheet's settings: the clerk limit (a clerk settles only claims whose due is at or below it;
// blank: no limit), the floor (a claim's live due is at least it; saved when its field is filled —
// a blank floor field leaves it as it is and saves the clerk limit), the terms (whole business days; a claim's
// due-by is that many business days after its date, skipping weekends and the sheet's holidays, fixed when the claim is created; saved when its field is filled, together with any other field filled),
// each grade's own terms (whole days; a new claim whose person's grade on its date has them uses them instead; saved like the terms; none: the sheet's terms), the late percent
// (two decimals; an overdue claim gains it of its figure for every full 30 days overdue; saved like the terms), the policy date (a claim dated
// before it rounds its live figures half up, one on or after it half to even; blank: the sheet's today; saved when its field is
// filled or was the last one entered, together with any other field filled), the period numbering from (from that date,
// a claim opened is numbered within its period; blank: off; saved like the policy date), the cap (two decimals; a person's
// due-total is at most the one in force on the date of their latest claim that counts; blank: none; saved like the policy date) and the role each user name holds (clerk unless set to steward, viewer or restricted — a viewer sees everything and changes nothing; a restricted user sees on the people list only the people of their crews and themselves).
// The clerk limit, the floor, the terms, each grade's terms, the late percent and the cap are dated: each has a from-date
// field (blank: the sheet's today) and is saved as its value from that date, keeping the values from other dates; each
// shows the value in force on the sheet's today. The rounding mode, the floor, the cap, the terms, the late percent and the
// clerk limit are versioned together as the policy: every change to one is a new version, and the current number shows. The proof threshold (two decimals; blank: none; saved like the cap) is dated
// too: a claim whose due is above the one in force on its date needs a proof to be settled or approved. So is the note
// threshold (two decimals; blank: none; saved like the cap): a claim whose due is above the one in force on its date cannot be opened with an empty note. A claim takes the floor, late percent and terms in force on its date.
// The permission matrix (not dated): for each of open, settle, void, reassign, close and reopen, the roles that may take it,
// comma separated (blank: the standing rule — open, settle, void and close by clerk and steward, reassign and reopen by steward);
// saved like the cap. The grace days (not dated; whole days; blank: off; saved like the cap): when the sheet's today is changed,
// every open period whose to-date plus them is before the new today is closed. The entry date (not dated; a date; blank: off;
// saved like the cap): while set, it is the default date of a new claim (the claim form, the quick-add bar, a pasted line
// with a blank date, a correction) in place of the sheet's today. The pot (not dated; two decimals; blank: none; saved like
// the cap): each member of a balanced pool shows their cut of it. The alert threshold (not dated; two decimals; blank: off;
// saved like the cap): a person whose due-total crosses above it is audited as an ALERT. The parent (not dated; a sheet id; blank: none; saved like
// the cap): a grade with no rate in force on a date on this sheet takes the parent's, then the parent's parent's, up to three
// levels up. The sheet itself, a sheet that has this sheet as an ancestor, or a sheet never loaded is refused.
// The proof kinds (not dated; a comma separated list of receipt and photo; blank: none; saved like the cap): while set, a
// claim above the proof threshold needs at least one proof of each listed kind to be settled or approved.
// A steward delegates the steward role to a user for a from-date and a to-date, inclusive (blank: the sheet's today): while the
// sheet's today is inside them the user acts as a steward in every rule. Anyone but a steward (by the role set), or a to-date
// before the from-date, is refused.
// Each grade's grace days (dated; whole days; blank: none; saved like the cap): a claim whose person's grade in force on its
// date has grace days in force on that date is overdue only once its due-by plus them is before the sheet's today, and its
// late charge's 30-day boundaries count from its due-by plus them. Its due-by is shown as it is.
// Each grade's proof threshold (dated; two decimals; blank: none; saved like the cap): a claim whose person's grade in force
// on its date has one in force on that date takes it in place of the sheet's proof threshold.
// The rounding mode (dated; half-up or half-even, anything else refused; blank: none; saved like the cap): a claim whose
// date has one in force rounds every live figure by it; one with none in force on its date rounds by the policy date.
// The fiscal year start month (not dated; 1 to 12; 1 unless set; saved when its field is filled, together with any other
// field filled): a fiscal year is named FY<yyyy> by the calendar year its first day falls in.
export function SettingsPanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const grades = useGradeCodes(sheet);
  const [user] = useSearchParam('user', asString);
  const settings = useSettings(sheet);
  const queryClient = useQueryClient();
  const submitSetting = useSubmitOnce();
  const submitRole = useSubmitOnce();
  const submitDelegate = useSubmitOnce();
  const [limit, setLimit] = useState('');
  const [floor, setFloor] = useState('');
  const [terms, setTerms] = useState('');
  // Each grade's own terms, by grade, as typed.
  const [gradeTerms, setGradeTerms] = useState<Record<string, string>>({});
  const [latePct, setLatePct] = useState('');
  const [fiscalStart, setFiscalStart] = useState('');
  // Each grade's grace days, by grade, as typed; each may be saved blank (none), so entering its field marks it as one to save.
  const [gradeGrace, setGradeGrace] = useState<Record<string, string>>({});
  const graceGradeChosen = useRef(new Set<string>());
  // Each grade's proof threshold, by grade, as typed; likewise each may be saved blank (none).
  const [gradeProof, setGradeProof] = useState<Record<string, string>>({});
  const proofGradeChosen = useRef(new Set<string>());
  // Each grade's clerk limit, by grade, as typed; likewise each may be saved blank (none: the sheet's applies).
  const [gradeLimit, setGradeLimit] = useState<Record<string, string>>({});
  const limitGradeChosen = useRef(new Set<string>());
  const today = useSheetToday(sheet);
  const [policyDate, setPolicyDate] = useState('');
  // The policy date may be saved blank (back to the sheet's today), so entering its field marks it as the one to save.
  const policyChosen = useRef(false);
  const [numberingFrom, setNumberingFrom] = useState('');
  // Likewise the period numbering from may be saved blank (off).
  const numberingChosen = useRef(false);
  const [cap, setCap] = useState('');
  // Likewise the cap may be saved blank (none).
  const capChosen = useRef(false);
  const [proofThreshold, setProofThreshold] = useState('');
  // Likewise the proof threshold may be saved blank (none).
  const proofChosen = useRef(false);
  const [noteThreshold, setNoteThreshold] = useState('');
  // Likewise the note threshold may be saved blank (none).
  const noteChosen = useRef(false);
  // The grace days (not dated); likewise may be saved blank (off).
  const [graceDays, setGraceDays] = useState('');
  const graceChosen = useRef(false);
  // The escalation gap in days (not dated); likewise may be saved blank (off).
  const [escalateAfter, setEscalateAfter] = useState('');
  const escalateChosen = useRef(false);
  // The review SLA in days (not dated); likewise may be saved blank (none).
  const [reviewSla, setReviewSla] = useState('');
  const reviewSlaChosen = useRef(false);
  // The entry date (not dated); likewise may be saved blank (off).
  const [entryDate, setEntryDate] = useState('');
  const entryChosen = useRef(false);
  // The pot (not dated); likewise may be saved blank (none).
  const [pot, setPot] = useState('');
  const potChosen = useRef(false);
  // The alert threshold (not dated); likewise may be saved blank (off).
  const [alertThreshold, setAlertThreshold] = useState('');
  const alertChosen = useRef(false);
  // The rounding mode (dated); likewise may be saved blank (none).
  const [rounding, setRounding] = useState('');
  const roundingChosen = useRef(false);
  // The review threshold (dated); likewise may be saved blank (none).
  const [reviewThreshold, setReviewThreshold] = useState('');
  const reviewChosen = useRef(false);
  // The parent sheet (not dated); likewise may be saved blank (none).
  const [parent, setParent] = useState('');
  const parentChosen = useRef(false);
  // The proof kinds (not dated; comma separated); likewise may be saved blank (none).
  const [proofKinds, setProofKinds] = useState('');
  const proofKindsChosen = useRef(false);
  // The default grade (not dated); likewise may be saved blank (A).
  const [defaultGrade, setDefaultGrade] = useState('');
  const defaultGradeChosen = useRef(false);
  // The score thresholds (not dated); likewise each may be saved blank (none).
  const [scoreMid, setScoreMid] = useState('');
  const scoreMidChosen = useRef(false);
  const [scoreHigh, setScoreHigh] = useState('');
  const scoreHighChosen = useRef(false);
  // The score log switch (not dated): on, or saved blank (off).
  const [scoreLog, setScoreLog] = useState('');
  const scoreLogChosen = useRef(false);
  // Entering a score threshold marks it as the one to save, as it is.
  const focusScoreMid = () => {
    unchoosePolicy();
    scoreMidChosen.current = true;
  };
  const focusScoreHigh = () => {
    unchoosePolicy();
    scoreHighChosen.current = true;
  };
  const focusScoreLog = () => {
    unchoosePolicy();
    scoreLogChosen.current = true;
  };
  // The maximum of open claims per person (not dated): a whole number, or saved blank (none).
  const [maxOpen, setMaxOpen] = useState('');
  const maxOpenChosen = useRef(false);
  const focusMaxOpen = () => {
    unchoosePolicy();
    maxOpenChosen.current = true;
  };
  // The pending SLA in days (not dated): a whole number, or saved blank (none).
  const [pendingSla, setPendingSla] = useState('');
  const pendingSlaChosen = useRef(false);
  const focusPendingSla = () => {
    unchoosePolicy();
    pendingSlaChosen.current = true;
  };
  // Entering the default grade marks it as the one to save, as it is.
  const focusDefaultGrade = () => {
    unchoosePolicy();
    defaultGradeChosen.current = true;
  };
  // Entering the proof kinds marks them as the one to save, as they are.
  const focusProofKinds = () => {
    unchoosePolicy();
    proofKindsChosen.current = true;
  };
  // Entering the parent marks it as the one to save, as it is.
  const focusParent = () => {
    unchoosePolicy();
    parentChosen.current = true;
  };
  // Entering the rounding mode (or its from-date) marks it as the one to save, as it is.
  const focusRounding = () => {
    unchoosePolicy();
    roundingChosen.current = true;
  };
  // Entering the alert threshold marks it as the one to save, as it is.
  const focusAlert = () => {
    unchoosePolicy();
    alertChosen.current = true;
  };
  // Entering the pot marks it as the one to save, as it is.
  const focusPot = () => {
    unchoosePolicy();
    potChosen.current = true;
  };
  // Entering the grace days marks them as the one to save, as they are.
  const focusGrace = () => {
    if (entryDate.trim() === '') entryChosen.current = false;
    unchoosePerms();
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (cap.trim() === '') capChosen.current = false;
    if (proofThreshold.trim() === '') proofChosen.current = false;
    if (noteThreshold.trim() === '') noteChosen.current = false;
    if (escalateAfter.trim() === '') escalateChosen.current = false;
    if (reviewSla.trim() === '') reviewSlaChosen.current = false;
    graceChosen.current = true;
  };
  // Entering the escalation gap marks it as the one to save, as it is.
  const focusEscalate = () => {
    if (entryDate.trim() === '') entryChosen.current = false;
    unchoosePerms();
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (cap.trim() === '') capChosen.current = false;
    if (proofThreshold.trim() === '') proofChosen.current = false;
    if (noteThreshold.trim() === '') noteChosen.current = false;
    if (graceDays.trim() === '') graceChosen.current = false;
    if (reviewSla.trim() === '') reviewSlaChosen.current = false;
    escalateChosen.current = true;
  };
  // Entering the review SLA marks it as the one to save, as it is.
  const focusReviewSla = () => {
    if (entryDate.trim() === '') entryChosen.current = false;
    unchoosePerms();
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (cap.trim() === '') capChosen.current = false;
    if (proofThreshold.trim() === '') proofChosen.current = false;
    if (noteThreshold.trim() === '') noteChosen.current = false;
    if (graceDays.trim() === '') graceChosen.current = false;
    if (escalateAfter.trim() === '') escalateChosen.current = false;
    reviewSlaChosen.current = true;
  };
  // Entering the entry date marks it as the one to save, as it is.
  const focusEntry = () => {
    unchoosePerms();
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (cap.trim() === '') capChosen.current = false;
    if (proofThreshold.trim() === '') proofChosen.current = false;
    if (noteThreshold.trim() === '') noteChosen.current = false;
    if (graceDays.trim() === '') graceChosen.current = false;
    if (escalateAfter.trim() === '') escalateChosen.current = false;
    if (reviewSla.trim() === '') reviewSlaChosen.current = false;
    entryChosen.current = true;
  };
  // Entering the review threshold (or its from-date) marks it as the one to save, as it is.
  const focusReview = () => {
    unchoosePolicy();
    reviewChosen.current = true;
  };
  // Each action's permission, by action, as typed; each may be saved blank (the standing rule).
  const [perms, setPerms] = useState<Record<string, string>>({});
  const permsChosen = useRef(new Set<string>());
  // Entering another field leaves a blank permission as it is.
  const unchoosePerms = (except?: string) => {
    for (const a of PERM_ACTIONS) {
      if (a !== except && (perms[a] ?? '').trim() === '') permsChosen.current.delete(a);
    }
    if (pot.trim() === '') potChosen.current = false;
    if (alertThreshold.trim() === '') alertChosen.current = false;
    if (parent.trim() === '') parentChosen.current = false;
    if (proofKinds.trim() === '') proofKindsChosen.current = false;
    if (defaultGrade.trim() === '') defaultGradeChosen.current = false;
    if (scoreMid.trim() === '') scoreMidChosen.current = false;
    if (scoreHigh.trim() === '') scoreHighChosen.current = false;
    if (scoreLog.trim() === '') scoreLogChosen.current = false;
    if (rounding.trim() === '') roundingChosen.current = false;
    if (reviewThreshold.trim() === '') reviewChosen.current = false;
    unchooseGradeGrace();
  };
  // Entering another field leaves a blank grade's grace days as they are.
  const unchooseGradeGrace = () => {
    for (const g of grades) {
      if ((gradeGrace[g] ?? '').trim() === '') graceGradeChosen.current.delete(g);
      if ((gradeProof[g] ?? '').trim() === '') proofGradeChosen.current.delete(g);
      if ((gradeLimit[g] ?? '').trim() === '') limitGradeChosen.current.delete(g);
    }
  };
  // Entering a grade's grace days (or their from-date) marks them as one to save, as they are.
  const focusGradeGrace = (grade: string) => {
    unchoosePolicy();
    graceGradeChosen.current.add(grade);
  };
  // Entering a grade's proof threshold (or its from-date) marks it as one to save, as it is.
  const focusGradeProof = (grade: string) => {
    unchoosePolicy();
    proofGradeChosen.current.add(grade);
  };
  // Entering a grade's clerk limit (or its from-date) marks it as one to save, as it is.
  const focusGradeLimit = (grade: string) => {
    unchoosePolicy();
    limitGradeChosen.current.add(grade);
  };
  // Entering a permission marks it as one to save, as it is.
  const focusPerm = (action: string) => {
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (cap.trim() === '') capChosen.current = false;
    if (proofThreshold.trim() === '') proofChosen.current = false;
    if (noteThreshold.trim() === '') noteChosen.current = false;
    if (graceDays.trim() === '') graceChosen.current = false;
    if (escalateAfter.trim() === '') escalateChosen.current = false;
    if (reviewSla.trim() === '') reviewSlaChosen.current = false;
    if (entryDate.trim() === '') entryChosen.current = false;
    unchoosePerms(action);
    permsChosen.current.add(action);
  };
  // Entering another field leaves a blank policy date (and a blank period numbering from, and a blank cap) as it is.
  const unchoosePolicy = () => {
    unchoosePerms();
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (cap.trim() === '') capChosen.current = false;
    if (proofThreshold.trim() === '') proofChosen.current = false;
    if (noteThreshold.trim() === '') noteChosen.current = false;
    if (graceDays.trim() === '') graceChosen.current = false;
    if (escalateAfter.trim() === '') escalateChosen.current = false;
    if (reviewSla.trim() === '') reviewSlaChosen.current = false;
    if (entryDate.trim() === '') entryChosen.current = false;
  };
  // Each dated setting's from-date, by key, as typed.
  const [froms, setFroms] = useState<Record<string, string>>({});
  const fromField = (key: string, onFocus: () => void = unchoosePolicy) => (
    <input
      data-testid={`setting-${key}-from`}
      onFocus={onFocus}
      value={froms[key] ?? ''}
      placeholder="From YYYY-MM-DD (blank: the sheet's today)"
      onChange={(e) => {
        const value = e.target.value;
        setFroms((f) => ({ ...f, [key]: value }));
        saveSetting.reset();
      }}
    />
  );
  // Entering the cap (or its from-date) marks it as the one to save, as it is.
  const focusCap = () => {
    unchoosePerms();
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (proofThreshold.trim() === '') proofChosen.current = false;
    if (noteThreshold.trim() === '') noteChosen.current = false;
    if (graceDays.trim() === '') graceChosen.current = false;
    if (escalateAfter.trim() === '') escalateChosen.current = false;
    if (reviewSla.trim() === '') reviewSlaChosen.current = false;
    if (entryDate.trim() === '') entryChosen.current = false;
    capChosen.current = true;
  };
  // Entering the proof threshold (or its from-date) marks it as the one to save, as it is.
  const focusProof = () => {
    unchoosePerms();
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (cap.trim() === '') capChosen.current = false;
    if (graceDays.trim() === '') graceChosen.current = false;
    if (escalateAfter.trim() === '') escalateChosen.current = false;
    if (reviewSla.trim() === '') reviewSlaChosen.current = false;
    if (entryDate.trim() === '') entryChosen.current = false;
    if (noteThreshold.trim() === '') noteChosen.current = false;
    proofChosen.current = true;
  };
  // Entering the note threshold (or its from-date) marks it as the one to save, as it is.
  const focusNote = () => {
    unchoosePerms();
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (cap.trim() === '') capChosen.current = false;
    if (proofThreshold.trim() === '') proofChosen.current = false;
    if (graceDays.trim() === '') graceChosen.current = false;
    if (escalateAfter.trim() === '') escalateChosen.current = false;
    if (reviewSla.trim() === '') reviewSlaChosen.current = false;
    if (entryDate.trim() === '') entryChosen.current = false;
    noteChosen.current = true;
  };
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [delegateUser, setDelegateUser] = useState('');
  const [delegateFrom, setDelegateFrom] = useState('');
  const [delegateTo, setDelegateTo] = useState('');
  const saveSetting = useMutation({
    mutationFn: (body: { key: string; value: string; from?: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`, { ...body, user }),
    onSuccess: (saved, body) => {
      setFroms((f) => ({ ...f, [body.key]: '' }));
      if (body.key === 'floor') setFloor('');
      else if (body.key === 'terms') setTerms('');
      else if (body.key.startsWith('terms-')) setGradeTerms((t) => ({ ...t, [body.key.slice('terms-'.length)]: '' }));
      else if (body.key === 'late-pct') setLatePct('');
      else if (body.key === 'fiscal-start-month') setFiscalStart('');
      else if (body.key === 'policy-date') {
        setPolicyDate('');
        policyChosen.current = false;
      }
      else if (body.key === 'period-numbering-from') {
        setNumberingFrom('');
        numberingChosen.current = false;
      }
      else if (body.key === 'cap') {
        setCap('');
        capChosen.current = false;
      }
      else if (body.key.startsWith('perm-')) {
        const action = body.key.slice('perm-'.length);
        setPerms((p) => ({ ...p, [action]: '' }));
        permsChosen.current.delete(action);
      }
      else if (body.key.startsWith('grace-') && body.key !== 'grace-days') {
        const grade = body.key.slice('grace-'.length);
        setGradeGrace((t) => ({ ...t, [grade]: '' }));
        graceGradeChosen.current.delete(grade);
      }
      else if (body.key === 'grace-days') {
        setGraceDays('');
        graceChosen.current = false;
      }
      else if (body.key === 'escalate-after') {
        setEscalateAfter('');
        escalateChosen.current = false;
      }
      else if (body.key === 'review-sla') {
        setReviewSla('');
        reviewSlaChosen.current = false;
      }
      else if (body.key === 'pot') {
        setPot('');
        potChosen.current = false;
        queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
      }
      else if (body.key === 'alert-threshold') {
        setAlertThreshold('');
        alertChosen.current = false;
      }
      else if (body.key === 'proof-kinds') {
        setProofKinds('');
        proofKindsChosen.current = false;
      }
      else if (body.key === 'default-grade') {
        setDefaultGrade('');
        defaultGradeChosen.current = false;
      }
      else if (body.key === 'score-mid') {
        setScoreMid('');
        scoreMidChosen.current = false;
      }
      else if (body.key === 'score-high') {
        setScoreHigh('');
        scoreHighChosen.current = false;
      }
      else if (body.key === 'score-log') {
        setScoreLog('');
        scoreLogChosen.current = false;
      }
      else if (body.key === 'max-open') {
        setMaxOpen('');
        maxOpenChosen.current = false;
      }
      else if (body.key === 'pending-sla') {
        setPendingSla('');
        pendingSlaChosen.current = false;
      }
      else if (body.key === 'parent') {
        setParent('');
        parentChosen.current = false;
        queryClient.invalidateQueries({ queryKey: ['sheet', sheet, 'inherited-rates'] });
      }
      else if (body.key === 'entry-date') {
        setEntryDate('');
        entryChosen.current = false;
      }
      else if (body.key.startsWith('clerk-limit-')) {
        const grade = body.key.slice('clerk-limit-'.length);
        setGradeLimit((t) => ({ ...t, [grade]: '' }));
        limitGradeChosen.current.delete(grade);
      }
      else if (body.key.startsWith('proof-threshold-')) {
        const grade = body.key.slice('proof-threshold-'.length);
        setGradeProof((t) => ({ ...t, [grade]: '' }));
        proofGradeChosen.current.delete(grade);
      }
      else if (body.key === 'proof-threshold') {
        setProofThreshold('');
        proofChosen.current = false;
      }
      else if (body.key === 'note-threshold') {
        setNoteThreshold('');
        noteChosen.current = false;
      }
      else if (body.key === 'review-threshold') {
        setReviewThreshold('');
        reviewChosen.current = false;
      }
      else if (body.key === 'rounding') {
        setRounding('');
        roundingChosen.current = false;
      }
      else setLimit('');
      queryClient.setQueryData(settingsKey(sheet), saved);
    },
  });
  const saveRole = useMutation({
    mutationFn: (body: { name: string; role: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/roles`, { ...body, user }),
    onSuccess: (saved) => {
      setName('');
      setRole('');
      queryClient.setQueryData(settingsKey(sheet), saved);
    },
  });
  const saveDelegate = useMutation({
    mutationFn: (body: { name: string; from: string; to: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/delegations`, { ...body, user }),
    onSuccess: (saved) => {
      setDelegateUser('');
      setDelegateFrom('');
      setDelegateTo('');
      queryClient.setQueryData(settingsKey(sheet), saved);
    },
  });
  return (
    <>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitSetting(() => {
            saveSetting.reset();
            // A dated setting is saved from its from-date (blank: the sheet's today).
            const save = (body: { key: string; value: string }) =>
              saveSetting.mutateAsync({ ...body, from: (froms[body.key] ?? '').trim() });
            const policy = policyDate.trim() !== '' || policyChosen.current;
            const numbering = numberingFrom.trim() !== '' || numberingChosen.current;
            const capped = cap.trim() !== '' || capChosen.current;
            const proofed = proofThreshold.trim() !== '' || proofChosen.current;
            const noted = noteThreshold.trim() !== '' || noteChosen.current;
            const rounded = rounding.trim() !== '' || roundingChosen.current;
            const reviewed = reviewThreshold.trim() !== '' || reviewChosen.current;
            const graced = graceDays.trim() !== '' || graceChosen.current;
            const escalated = escalateAfter.trim() !== '' || escalateChosen.current;
            const slaed = reviewSla.trim() !== '' || reviewSlaChosen.current;
            const entered = entryDate.trim() !== '' || entryChosen.current;
            const potted = pot.trim() !== '' || potChosen.current;
            const alerted = alertThreshold.trim() !== '' || alertChosen.current;
            const parented = parent.trim() !== '' || parentChosen.current;
            const kinded = proofKinds.trim() !== '' || proofKindsChosen.current;
            const defaulted = defaultGrade.trim() !== '' || defaultGradeChosen.current;
            const scoredMid = scoreMid.trim() !== '' || scoreMidChosen.current;
            const scoredHigh = scoreHigh.trim() !== '' || scoreHighChosen.current;
            const scoreLogged = scoreLog.trim() !== '' || scoreLogChosen.current;
            const maxed = maxOpen.trim() !== '' || maxOpenChosen.current;
            const pendingSlaed = pendingSla.trim() !== '' || pendingSlaChosen.current;
            const ownTerms = grades.filter((g) => (gradeTerms[g] ?? '').trim() !== '');
            const ownGrace = grades.filter((g) => (gradeGrace[g] ?? '').trim() !== '' || graceGradeChosen.current.has(g));
            const ownProof = grades.filter((g) => (gradeProof[g] ?? '').trim() !== '' || proofGradeChosen.current.has(g));
            const ownLimit = grades.filter((g) => (gradeLimit[g] ?? '').trim() !== '' || limitGradeChosen.current.has(g));
            const permitted = PERM_ACTIONS.filter((a) => (perms[a] ?? '').trim() !== '' || permsChosen.current.has(a));
            if (terms.trim() !== '' || ownTerms.length > 0 || ownGrace.length > 0 || latePct.trim() !== '' || fiscalStart.trim() !== '' || policy || numbering || capped || proofed || ownProof.length > 0 || ownLimit.length > 0
                || noted || rounded || reviewed || graced || escalated || slaed || entered || potted || alerted || parented || kinded || defaulted || scoredMid || scoredHigh || scoreLogged || maxed || pendingSlaed || permitted.length > 0) {
              const saves = [
                ...(limit.trim() !== '' ? [{ key: 'clerk-limit', value: limit.trim() }] : []),
                ...ownLimit.map((g) => ({ key: `clerk-limit-${g}`, value: (gradeLimit[g] ?? '').trim() })),
                ...(floor.trim() !== '' ? [{ key: 'floor', value: floor.trim() }] : []),
                ...(terms.trim() !== '' ? [{ key: 'terms', value: terms.trim() }] : []),
                ...ownTerms.map((g) => ({ key: `terms-${g}`, value: gradeTerms[g].trim() })),
                ...(latePct.trim() !== '' ? [{ key: 'late-pct', value: latePct.trim() }] : []),
                ...(fiscalStart.trim() !== '' ? [{ key: 'fiscal-start-month', value: fiscalStart.trim() }] : []),
                ...ownGrace.map((g) => ({ key: `grace-${g}`, value: (gradeGrace[g] ?? '').trim() })),
                ...(policy ? [{ key: 'policy-date', value: policyDate.trim() }] : []),
                ...(numbering ? [{ key: 'period-numbering-from', value: numberingFrom.trim() }] : []),
                ...(capped ? [{ key: 'cap', value: cap.trim() }] : []),
                ...(proofed ? [{ key: 'proof-threshold', value: proofThreshold.trim() }] : []),
                ...ownProof.map((g) => ({ key: `proof-threshold-${g}`, value: (gradeProof[g] ?? '').trim() })),
                ...(noted ? [{ key: 'note-threshold', value: noteThreshold.trim() }] : []),
                ...(rounded ? [{ key: 'rounding', value: rounding.trim() }] : []),
                ...(reviewed ? [{ key: 'review-threshold', value: reviewThreshold.trim() }] : []),
                ...(graced ? [{ key: 'grace-days', value: graceDays.trim() }] : []),
                ...(escalated ? [{ key: 'escalate-after', value: escalateAfter.trim() }] : []),
                ...(slaed ? [{ key: 'review-sla', value: reviewSla.trim() }] : []),
                ...(entered ? [{ key: 'entry-date', value: entryDate.trim() }] : []),
                ...(potted ? [{ key: 'pot', value: pot.trim() }] : []),
                ...(alerted ? [{ key: 'alert-threshold', value: alertThreshold.trim() }] : []),
                ...(parented ? [{ key: 'parent', value: parent.trim() }] : []),
                ...(kinded ? [{ key: 'proof-kinds', value: proofKinds.trim() }] : []),
                ...(defaulted ? [{ key: 'default-grade', value: defaultGrade.trim() }] : []),
                ...(scoredMid ? [{ key: 'score-mid', value: scoreMid.trim() }] : []),
                ...(scoredHigh ? [{ key: 'score-high', value: scoreHigh.trim() }] : []),
                ...(scoreLogged ? [{ key: 'score-log', value: scoreLog.trim() }] : []),
                ...(maxed ? [{ key: 'max-open', value: maxOpen.trim() }] : []),
                ...(pendingSlaed ? [{ key: 'pending-sla', value: pendingSla.trim() }] : []),
                ...permitted.map((a) => ({ key: `perm-${a}`, value: (perms[a] ?? '').trim() })),
              ];
              return saves.reduce<Promise<unknown>>((done, body) => done.then(() => save(body)), Promise.resolve());
            }
            if (floor.trim() === '') return save({ key: 'clerk-limit', value: limit.trim() });
            const saveFloor = () => save({ key: 'floor', value: floor.trim() });
            return limit.trim() === ''
              ? saveFloor()
              : save({ key: 'clerk-limit', value: limit.trim() }).then(saveFloor);
          });
        }}
      >
        Clerk limit: <span data-testid="setting-clerk-limit-current">{settings?.clerkLimit ?? ''}</span>{' '}
        <input
          data-testid="setting-clerk-limit"
          onFocus={unchoosePolicy}
          value={limit}
          placeholder="Clerk limit (blank: none)"
          onChange={(e) => {
            setLimit(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        {fromField('clerk-limit')}{' '}
        {grades.map((g) => (
          <span key={g}>
            Clerk limit for grade {g}: <span data-testid={`setting-clerk-limit-${g}-current`}>{today === undefined ? '' : inForce(settings, `clerk-limit-${g}`, today, '')}</span>{' '}
            <input
              data-testid={`setting-clerk-limit-${g}`}
              onFocus={() => focusGradeLimit(g)}
              value={gradeLimit[g] ?? ''}
              placeholder={`Grade ${g} clerk limit (blank: none)`}
              onChange={(e) => {
                const value = e.target.value;
                setGradeLimit((t) => ({ ...t, [g]: value }));
                limitGradeChosen.current.add(g);
                saveSetting.reset();
              }}
            />{' '}
            {fromField(`clerk-limit-${g}`, () => focusGradeLimit(g))}{' '}
          </span>
        ))}
        Floor: <span data-testid="setting-floor-current">{settings?.floor ?? ''}</span>{' '}
        <input
          data-testid="setting-floor"
          onFocus={unchoosePolicy}
          value={floor}
          placeholder="Floor (blank: unchanged)"
          onChange={(e) => {
            setFloor(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        {fromField('floor')}{' '}
        Terms (days): <span data-testid="setting-terms-current">{settings?.terms ?? ''}</span>{' '}
        <input
          data-testid="setting-terms"
          onFocus={unchoosePolicy}
          value={terms}
          placeholder="Terms in days (blank: unchanged)"
          onChange={(e) => {
            setTerms(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        {fromField('terms')}{' '}
        {grades.map((g) => (
          <span key={g}>
            Terms for grade {g} (days): <span data-testid={`setting-terms-${g}-current`}>{settings?.gradeTerms?.[g] ?? ''}</span>{' '}
            <input
              data-testid={`setting-terms-${g}`}
              onFocus={unchoosePolicy}
              value={gradeTerms[g] ?? ''}
              placeholder={`Grade ${g} terms in days (blank: unchanged)`}
              onChange={(e) => {
                const value = e.target.value;
                setGradeTerms((t) => ({ ...t, [g]: value }));
                saveSetting.reset();
              }}
            />{' '}
            {fromField(`terms-${g}`)}{' '}
          </span>
        ))}
        {grades.map((g) => (
          <span key={g}>
            Grace for grade {g} (days): <span data-testid={`setting-grace-${g}-current`}>{today === undefined ? '' : inForce(settings, `grace-${g}`, today, '')}</span>{' '}
            <input
              data-testid={`setting-grace-${g}`}
              onFocus={() => focusGradeGrace(g)}
              value={gradeGrace[g] ?? ''}
              placeholder={`Grade ${g} grace in days (blank: none)`}
              onChange={(e) => {
                const value = e.target.value;
                setGradeGrace((t) => ({ ...t, [g]: value }));
                graceGradeChosen.current.add(g);
                saveSetting.reset();
              }}
            />{' '}
            {fromField(`grace-${g}`, () => focusGradeGrace(g))}{' '}
          </span>
        ))}
        Late percent: <span data-testid="setting-late-pct-current">{settings?.latePct ?? ''}</span>{' '}
        <input
          data-testid="setting-late-pct"
          onFocus={unchoosePolicy}
          value={latePct}
          placeholder="Late percent (blank: unchanged)"
          onChange={(e) => {
            setLatePct(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        {fromField('late-pct')}{' '}
        Fiscal year starts in month: <span data-testid="setting-fiscal-start-month-current">{settings?.fiscalStartMonth ?? ''}</span>{' '}
        <input
          data-testid="setting-fiscal-start-month"
          onFocus={unchoosePolicy}
          value={fiscalStart}
          placeholder="Fiscal start month 1-12 (blank: unchanged)"
          onChange={(e) => {
            setFiscalStart(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        Policy date: <span data-testid="setting-policy-date-current">{settings?.policyDate ?? ''}</span>{' '}
        <input
          data-testid="setting-policy-date"
          value={policyDate}
          placeholder="Policy date YYYY-MM-DD (blank: the sheet's today)"
          onFocus={() => {
            unchoosePerms();
            if (numberingFrom.trim() === '') numberingChosen.current = false;
            if (cap.trim() === '') capChosen.current = false;
            if (proofThreshold.trim() === '') proofChosen.current = false;
            if (noteThreshold.trim() === '') noteChosen.current = false;
            if (graceDays.trim() === '') graceChosen.current = false;
            if (escalateAfter.trim() === '') escalateChosen.current = false;
            if (reviewSla.trim() === '') reviewSlaChosen.current = false;
            if (entryDate.trim() === '') entryChosen.current = false;
            policyChosen.current = true;
          }}
          onChange={(e) => {
            setPolicyDate(e.target.value);
            policyChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Period numbering from: <span data-testid="setting-period-numbering-from-current">{settings?.periodNumberingFrom ?? ''}</span>{' '}
        <input
          data-testid="setting-period-numbering-from"
          value={numberingFrom}
          placeholder="Period numbering from YYYY-MM-DD (blank: off)"
          onFocus={() => {
            unchoosePerms();
            if (policyDate.trim() === '') policyChosen.current = false;
            if (cap.trim() === '') capChosen.current = false;
            if (proofThreshold.trim() === '') proofChosen.current = false;
            if (noteThreshold.trim() === '') noteChosen.current = false;
            if (graceDays.trim() === '') graceChosen.current = false;
            if (escalateAfter.trim() === '') escalateChosen.current = false;
            if (reviewSla.trim() === '') reviewSlaChosen.current = false;
            if (entryDate.trim() === '') entryChosen.current = false;
            numberingChosen.current = true;
          }}
          onChange={(e) => {
            setNumberingFrom(e.target.value);
            numberingChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Policy version: <span data-testid="policy-version">{settings?.policyVersions?.length ?? 0}</span>{' '}
        <PolicyViewer />{' '}
        <PolicyExport />{' '}
        Cap: <span data-testid="setting-cap-current">{settings?.cap ?? ''}</span>{' '}
        <input
          data-testid="setting-cap"
          value={cap}
          placeholder="Cap (blank: none)"
          onFocus={focusCap}
          onChange={(e) => {
            setCap(e.target.value);
            capChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        {fromField('cap', focusCap)}{' '}
        Proof threshold: <span data-testid="setting-proof-threshold-current">{settings?.proofThreshold ?? ''}</span>{' '}
        <input
          data-testid="setting-proof-threshold"
          value={proofThreshold}
          placeholder="Proof threshold (blank: none)"
          onFocus={focusProof}
          onChange={(e) => {
            setProofThreshold(e.target.value);
            proofChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        {fromField('proof-threshold', focusProof)}{' '}
        {grades.map((g) => (
          <span key={g}>
            Proof threshold for grade {g}: <span data-testid={`setting-proof-threshold-${g}-current`}>{today === undefined ? '' : inForce(settings, `proof-threshold-${g}`, today, '')}</span>{' '}
            <input
              data-testid={`setting-proof-threshold-${g}`}
              onFocus={() => focusGradeProof(g)}
              value={gradeProof[g] ?? ''}
              placeholder={`Grade ${g} proof threshold (blank: none)`}
              onChange={(e) => {
                const value = e.target.value;
                setGradeProof((t) => ({ ...t, [g]: value }));
                proofGradeChosen.current.add(g);
                saveSetting.reset();
              }}
            />{' '}
            {fromField(`proof-threshold-${g}`, () => focusGradeProof(g))}{' '}
          </span>
        ))}
        Note threshold: <span data-testid="setting-note-threshold-current">{settings?.noteThreshold ?? ''}</span>{' '}
        <input
          data-testid="setting-note-threshold"
          value={noteThreshold}
          placeholder="Note threshold (blank: none)"
          onFocus={focusNote}
          onChange={(e) => {
            setNoteThreshold(e.target.value);
            noteChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        {fromField('note-threshold', focusNote)}{' '}
        Rounding: <span data-testid="setting-rounding-current">{settings?.rounding ?? ''}</span>{' '}
        <input
          data-testid="setting-rounding"
          value={rounding}
          placeholder="Rounding: half-up or half-even (blank: none)"
          onFocus={focusRounding}
          onChange={(e) => {
            setRounding(e.target.value);
            roundingChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        {fromField('rounding', focusRounding)}{' '}
        Review threshold: <span data-testid="setting-review-threshold-current">{settings?.reviewThreshold ?? ''}</span>{' '}
        <input
          data-testid="setting-review-threshold"
          value={reviewThreshold}
          placeholder="Review threshold (blank: none)"
          onFocus={focusReview}
          onChange={(e) => {
            setReviewThreshold(e.target.value);
            reviewChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        {fromField('review-threshold', focusReview)}{' '}
        Grace days: <span data-testid="setting-grace-days-current">{settings?.graceDays ?? ''}</span>{' '}
        <input
          data-testid="setting-grace-days"
          value={graceDays}
          placeholder="Grace days (blank: off)"
          onFocus={focusGrace}
          onChange={(e) => {
            setGraceDays(e.target.value);
            graceChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Escalate after (days): <span data-testid="setting-escalate-after-current">{settings?.escalateAfter ?? ''}</span>{' '}
        <input
          data-testid="setting-escalate-after"
          value={escalateAfter}
          placeholder="Escalate after days (blank: off)"
          onFocus={focusEscalate}
          onChange={(e) => {
            setEscalateAfter(e.target.value);
            escalateChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Review SLA (days): <span data-testid="setting-review-sla-current">{settings?.reviewSla ?? ''}</span>{' '}
        <input
          data-testid="setting-review-sla"
          value={reviewSla}
          placeholder="Review SLA days (blank: none)"
          onFocus={focusReviewSla}
          onChange={(e) => {
            setReviewSla(e.target.value);
            reviewSlaChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Entry date: <span data-testid="setting-entry-date-current">{settings?.entryDate ?? ''}</span>{' '}
        <input
          data-testid="setting-entry-date"
          value={entryDate}
          placeholder="Entry date YYYY-MM-DD (blank: off)"
          onFocus={focusEntry}
          onChange={(e) => {
            setEntryDate(e.target.value);
            entryChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Pot: <span data-testid="setting-pot-current">{settings?.pot ?? ''}</span>{' '}
        <input
          data-testid="setting-pot"
          value={pot}
          placeholder="Pot (blank: none)"
          onFocus={focusPot}
          onChange={(e) => {
            setPot(e.target.value);
            potChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Alert threshold: <span data-testid="setting-alert-threshold-current">{settings?.alertThreshold ?? ''}</span>{' '}
        <input
          data-testid="setting-alert-threshold"
          value={alertThreshold}
          placeholder="Alert threshold (blank: off)"
          onFocus={focusAlert}
          onChange={(e) => {
            setAlertThreshold(e.target.value);
            alertChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Proof kinds: <span data-testid="setting-proof-kinds-current">{settings?.proofKinds ?? ''}</span>{' '}
        <input
          data-testid="setting-proof-kinds"
          value={proofKinds}
          placeholder="Proof kinds, comma separated: receipt, photo (blank: none)"
          onFocus={focusProofKinds}
          onChange={(e) => {
            setProofKinds(e.target.value);
            proofKindsChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Default grade: <span data-testid="setting-default-grade-current">{settings?.defaultGrade ?? ''}</span>{' '}
        <input
          data-testid="setting-default-grade"
          value={defaultGrade}
          placeholder="Default grade: one of the grades (blank: A)"
          onFocus={focusDefaultGrade}
          onChange={(e) => {
            setDefaultGrade(e.target.value);
            defaultGradeChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Score mid: <span data-testid="setting-score-mid-current">{settings?.scoreMid ?? ''}</span>{' '}
        <input
          data-testid="setting-score-mid"
          value={scoreMid}
          placeholder="Score mid threshold, whole number (blank: none)"
          onFocus={focusScoreMid}
          onChange={(e) => {
            setScoreMid(e.target.value);
            scoreMidChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Score high: <span data-testid="setting-score-high-current">{settings?.scoreHigh ?? ''}</span>{' '}
        <input
          data-testid="setting-score-high"
          value={scoreHigh}
          placeholder="Score high threshold, whole number (blank: none)"
          onFocus={focusScoreHigh}
          onChange={(e) => {
            setScoreHigh(e.target.value);
            scoreHighChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Score log: <span data-testid="setting-score-log-current">{settings?.scoreLog ?? ''}</span>{' '}
        <input
          data-testid="setting-score-log"
          value={scoreLog}
          placeholder="Score log: on (blank: off)"
          onFocus={focusScoreLog}
          onChange={(e) => {
            setScoreLog(e.target.value);
            scoreLogChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Max open claims: <span data-testid="setting-max-open-current">{settings?.maxOpen ?? ''}</span>{' '}
        <input
          data-testid="setting-max-open"
          value={maxOpen}
          placeholder="Max open claims per person (blank: none)"
          onFocus={focusMaxOpen}
          onChange={(e) => {
            setMaxOpen(e.target.value);
            maxOpenChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Pending SLA (days): <span data-testid="setting-pending-sla-current">{settings?.pendingSla ?? ''}</span>{' '}
        <input
          data-testid="setting-pending-sla"
          value={pendingSla}
          placeholder="Pending SLA days (blank: none)"
          onFocus={focusPendingSla}
          onChange={(e) => {
            setPendingSla(e.target.value);
            pendingSlaChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Parent sheet: <span data-testid="setting-parent-current">{settings?.parent ?? ''}</span>{' '}
        <input
          data-testid="setting-parent"
          value={parent}
          placeholder="Parent sheet id (blank: none)"
          onFocus={focusParent}
          onChange={(e) => {
            setParent(e.target.value);
            parentChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        {PERM_ACTIONS.map((a) => (
          <span key={a}>
            May {a}: <span data-testid={`setting-perm-${a}-current`}>{settings?.perms?.[a] ?? ''}</span>{' '}
            <input
              data-testid={`setting-perm-${a}`}
              value={perms[a] ?? ''}
              placeholder={`Roles that may ${a}, comma separated (blank: the standing rule)`}
              onFocus={() => focusPerm(a)}
              onChange={(e) => {
                const value = e.target.value;
                setPerms((p) => ({ ...p, [a]: value }));
                permsChosen.current.add(a);
                saveSetting.reset();
              }}
            />{' '}
          </span>
        ))}
        <button type="submit" data-testid="settings-save">Save settings</button>
        {saveSetting.isError && <span data-testid="settings-error" role="alert"> Could not save: the clerk limit must be an amount of 0.00 or more with at most two decimals, or blank for none; the floor must be an amount of 0.00 or more with at most two decimals; the terms (and a grade's terms) must be a whole number of days, 0 or more; the late percent must be a percent of 0.00 or more with at most two decimals; a grade's grace days must be a whole number of days, 0 or more, or blank for none; the policy date must be a date (YYYY-MM-DD), or blank for the sheet's today; the period numbering from must be a date (YYYY-MM-DD), or blank for off; the cap must be an amount of 0.00 or more with at most two decimals, or blank for none; the proof threshold must be an amount of 0.00 or more with at most two decimals, or blank for none; the note threshold must be an amount of 0.00 or more with at most two decimals, or blank for none; the grace days must be a whole number of days, 0 or more, or blank for off; the escalation gap must be a whole number of days, 0 or more, or blank for off; the entry date must be a date (YYYY-MM-DD), or blank for off; the pot must be an amount of 0.00 or more with at most two decimals, or blank for none; the alert threshold must be an amount of 0.00 or more with at most two decimals, or blank for off; the parent must be another sheet that has been loaded and does not have this sheet as an ancestor, or blank for none; the proof kinds must be a comma separated list of the kinds receipt and photo, or blank for none; a permission must be a comma separated list of the roles clerk and steward, or blank for the standing rule; the fiscal year start month must be a month from 1 to 12; a from-date must be a date (YYYY-MM-DD), or blank for the sheet's today.</span>}
      </form>
      <table data-testid="role-list">
        <thead>
          <tr>
            <th>User</th>
            <th>Role</th>
          </tr>
        </thead>
        <tbody>
          {(settings?.roles ?? []).map((r) => (
            <tr key={r.user} data-testid={`role-row-${r.user}`}>
              <td data-testid="role-user">{r.user}</td>
              <td data-testid="role-value">{r.role}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <form
        data-testid="role-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitRole(() => {
            saveRole.reset();
            return saveRole.mutateAsync({ name: name.trim(), role: role.trim() });
          });
        }}
      >
        <input
          data-testid="role-form-user"
          value={name}
          placeholder="User name"
          onChange={(e) => {
            setName(e.target.value);
            saveRole.reset();
          }}
        />{' '}
        <input
          data-testid="role-form-role"
          value={role}
          placeholder="Role (clerk, steward, viewer or restricted)"
          onChange={(e) => {
            setRole(e.target.value);
            saveRole.reset();
          }}
        />{' '}
        <button type="submit" data-testid="role-form-submit">Set role</button>
        {saveRole.isError && <span data-testid="role-form-error" role="alert"> Could not set the role: it needs a user name and the role clerk, steward, viewer or restricted, and a viewer can change nothing.</span>}
      </form>
      <ul data-testid="delegate-list">
        {(settings?.delegations ?? []).map((d, i) => (
          <li key={i} data-testid="delegate-line">
            {d.user} acts as a steward from {d.from} to {d.to}, for {d.by}
          </li>
        ))}
      </ul>
      <form
        data-testid="delegate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitDelegate(() => {
            saveDelegate.reset();
            return saveDelegate.mutateAsync({ name: delegateUser.trim(), from: delegateFrom.trim(), to: delegateTo.trim() });
          });
        }}
      >
        <input
          data-testid="delegate-user"
          value={delegateUser}
          placeholder="Delegate the steward role to"
          onChange={(e) => {
            setDelegateUser(e.target.value);
            saveDelegate.reset();
          }}
        />{' '}
        <input
          data-testid="delegate-from"
          value={delegateFrom}
          placeholder="From (YYYY-MM-DD)"
          onChange={(e) => {
            setDelegateFrom(e.target.value);
            saveDelegate.reset();
          }}
        />{' '}
        <input
          data-testid="delegate-to"
          value={delegateTo}
          placeholder="To (YYYY-MM-DD)"
          onChange={(e) => {
            setDelegateTo(e.target.value);
            saveDelegate.reset();
          }}
        />{' '}
        <button type="submit" data-testid="delegate-submit">Delegate</button>
        {saveDelegate.isError && <span data-testid="delegate-error" role="alert"> Could not delegate: only a steward can delegate the steward role, it needs a user name, and the to-date cannot be before the from-date.</span>}
      </form>
    </>
  );
}
