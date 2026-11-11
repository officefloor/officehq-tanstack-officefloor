import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { poolsKey } from '../pools/pools';
import { periodsKey } from '../periods/periods';
import { ACTIONS, ALERT_THRESHOLD, CAP, DATED_KEYS, FLOOR, GRADES, LATE_PCT, LIMIT, NOTE_THRESHOLD, PERM_ROLES, REVIEW_THRESHOLD, POLICY_DATE, POT, PROOF_KINDS, PROOF_THRESHOLD, ROUNDING, STANDING_RULE, TERMS, ROLES, gradeClerkLimitKey, gradeGraceKey, gradeProofThresholdKey, policyVersionOn, rolesKey, settingsKey, useRoles, useSettings } from './access';
import type { Role, Settings, SettingsChange } from './access';
import { useGradeCodes } from '../grades/grades';

type Key = Exclude<keyof Settings, 'gradeTerms' | 'perms' | 'gradeGrace' | 'gradeProofThreshold' | 'gradeClerkLimit'>;

// The sheet's settings: its clerk limit, the highest due a clerk may settle (blank: no limit), and
// its floor, the least a claim's live due may be, and its terms, the days after a claim's date that
// it is due by, and its late percent, what an overdue claim's due gains at each full 30 days overdue (the one in force that day), and
// its policy date (blank: the sheet's today), before which a claim rounds half up, and its period
// numbering from (blank: off), from which a claim opened is numbered within its period, and each
// grade's own terms (blank: the sheet's terms), fixing the due-by of a claim made for it, and its cap
// (blank: none), the most a person's due-total may be, and its proof threshold (blank: none), above
// which a claim's due, on its date, needs a proof to be settled or approved, and its note threshold
// (blank: none), above which a claim's due, on its date, needs a note to be opened, and its permission
// matrix: for each action the roles that may take it, comma separated (blank: the standing rule), and its
// grace days (blank: off), past which an open period closes when today is changed, and each grade's
// grace days (blank: none), dated, past a claim's due-by before it is overdue, and each grade's proof threshold
// (blank: none, the sheet's), dated, in place of the sheet's for a claim of the grade, and its pot (blank:
// none), not dated, shared out as cuts among a balanced pool's members, and its parent sheet (blank: none), not
// dated, whose rates a grade takes on a date this sheet has none in force, and its fiscal year start month
// (1 to 12), not dated, and its alert threshold (blank: off), not dated, above which a person's due-total
// is alerted, and its rounding mode (half-up or half-even; blank: none, by the policy date), dated, by which
// a claim dated then rounds, and its review threshold (blank: none), dated, above which a claim's due on its
// date sends it to REVIEW instead of OPEN when it is opened, and its escalation gap in days (blank: off),
// not dated, at least which after a claim's previous reminder a reminder escalates it, once, and its review
// SLA in days (blank: none), not dated, past which after its review day a REVIEW claim is late, and its
// proof kinds (blank: none), not dated, a comma separated list of receipt or photo. Only the settings typed in (or stepped into)
// since the last save are sent, so saving one leaves the other as it is.
function SettingsForm({ sheetId }: { sheetId: string }) {
  const sheetGrades = useGradeCodes(sheetId);
  const queryClient = useQueryClient();
  const settings = useSettings(sheetId);
  const [fields, setFields] = useState<Omit<Settings, 'gradeTerms' | 'perms' | 'gradeGrace' | 'gradeProofThreshold' | 'gradeClerkLimit'>>({
    cap: '',
    proofThreshold: '',
    noteThreshold: '',
    clerkLimit: '',
    floor: '',
    terms: '',
    latePct: '',
    policyDate: '',
    periodNumberingFrom: '',
    graceDays: '',
    entryDate: '',
    pot: '',
    parent: '',
    fiscalStartMonth: '',
    alertThreshold: '',
    rounding: '',
    reviewThreshold: '',
    escalateAfter: '',
    reviewSla: '',
    proofKinds: '',
    defaultGrade: '',
    scoreMid: '',
    scoreHigh: '',
    scoreLog: '',
    maxOpen: '',
    pendingSla: '',
  });
  const [touched, setTouched] = useState<Set<Key>>(new Set());
  const [gradeFields, setGradeFields] = useState<Record<string, string>>({});
  const [gradesTouched, setGradesTouched] = useState<Set<string>>(new Set());
  const [graceFields, setGraceFields] = useState<Record<string, string>>({});
  const [graceTouched, setGraceTouched] = useState<Set<string>>(new Set());
  const [thresholdFields, setThresholdFields] = useState<Record<string, string>>({});
  const [thresholdsTouched, setThresholdsTouched] = useState<Set<string>>(new Set());
  const [limitFields, setLimitFields] = useState<Record<string, string>>({});
  const [limitsTouched, setLimitsTouched] = useState<Set<string>>(new Set());
  const [permFields, setPermFields] = useState<Record<string, string>>({});
  const [permsTouched, setPermsTouched] = useState<Set<string>>(new Set());
  // The from-date typed for each figure setting, by its store key (blank: the sheet's today).
  const [fromFields, setFromFields] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const [user] = useUser();

  // The fields the save in flight sent: only those are cleared when it lands, so a field touched
  // while it was in flight is still sent by the next save.
  const sent = useRef<Set<Key>>(new Set());
  const once = useOnce();
  const save = useMutation({
    mutationFn: (body: SettingsChange) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, body, user),
    onSuccess: (saved, body) => {
      setTouched((t) => new Set([...t].filter((k) => !sent.current.has(k))));
      setGradesTouched(new Set());
      setGraceTouched(new Set());
      setThresholdsTouched(new Set());
      setLimitsTouched(new Set());
      setPermsTouched(new Set());
      queryClient.setQueryData(settingsKey(sheetId), saved);
      queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      // A new parent changes the rates in force, and every figure taken from them.
      if (body.parent !== undefined) void queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
      // A new fiscal start month changes the fiscal year each period falls in.
      if (body.fiscalStartMonth !== undefined) void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: () => setError('Those settings could not be saved.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const body: SettingsChange = {};
    if (touched.has('clerkLimit')) {
      const clerkLimit = fields.clerkLimit.trim();
      if (clerkLimit !== '' && !LIMIT.test(clerkLimit)) {
        setError('A clerk limit is 0.00 or more, with up to two decimals.');
        return;
      }
      body.clerkLimit = clerkLimit;
    }
    if (touched.has('floor')) {
      const floor = fields.floor.trim();
      if (!FLOOR.test(floor)) {
        setError('A floor is 0.00 or more, with up to two decimals.');
        return;
      }
      body.floor = floor;
    }
    if (touched.has('terms')) {
      const terms = fields.terms.trim();
      if (!TERMS.test(terms)) {
        setError('Terms are a whole number of days, 0 or more.');
        return;
      }
      body.terms = terms;
    }
    if (touched.has('latePct')) {
      const latePct = fields.latePct.trim();
      if (!LATE_PCT.test(latePct)) {
        setError('A late percent is 0.00 or more, with up to two decimals.');
        return;
      }
      body.latePct = latePct;
    }
    if (touched.has('policyDate')) {
      const policyDate = fields.policyDate.trim();
      if (policyDate !== '' && (!POLICY_DATE.test(policyDate) || Number.isNaN(Date.parse(policyDate)))) {
        setError('A policy date is a date, yyyy-MM-dd, or blank.');
        return;
      }
      body.policyDate = policyDate;
    }
    if (touched.has('periodNumberingFrom')) {
      const from = fields.periodNumberingFrom.trim();
      if (from !== '' && (!POLICY_DATE.test(from) || Number.isNaN(Date.parse(from)))) {
        setError('A period numbering from is a date, yyyy-MM-dd, or blank.');
        return;
      }
      body.periodNumberingFrom = from;
    }
    if (touched.has('graceDays')) {
      const graceDays = fields.graceDays.trim();
      if (graceDays !== '' && !TERMS.test(graceDays)) {
        setError('Grace days are a whole number of days, 0 or more, or blank.');
        return;
      }
      body.graceDays = graceDays;
    }
    if (touched.has('entryDate')) {
      const entryDate = fields.entryDate.trim();
      if (entryDate !== '' && (!POLICY_DATE.test(entryDate) || Number.isNaN(Date.parse(entryDate)))) {
        setError('An entry date is a date, yyyy-MM-dd, or blank.');
        return;
      }
      body.entryDate = entryDate;
    }
    if (touched.has('pot')) {
      const pot = fields.pot.trim();
      if (pot !== '' && !POT.test(pot)) {
        setError('A pot is 0.00 or more, with up to two decimals, or blank.');
        return;
      }
      body.pot = pot;
    }
    if (touched.has('alertThreshold')) {
      const threshold = fields.alertThreshold.trim();
      if (threshold !== '' && !ALERT_THRESHOLD.test(threshold)) {
        setError('An alert threshold is 0.00 or more, with up to two decimals, or blank.');
        return;
      }
      body.alertThreshold = threshold;
    }
    if (touched.has('parent')) body.parent = fields.parent.trim();
    if (touched.has('fiscalStartMonth')) {
      const month = fields.fiscalStartMonth.trim();
      if (!/^(0?[1-9]|1[0-2])$/.test(month)) {
        setError('A fiscal start month is a month, 1 to 12.');
        return;
      }
      body.fiscalStartMonth = month;
    }
    if (touched.has('cap')) {
      const cap = fields.cap.trim();
      if (cap !== '' && !CAP.test(cap)) {
        setError('A cap is 0.00 or more, with up to two decimals, or blank.');
        return;
      }
      body.cap = cap;
    }
    if (touched.has('proofThreshold')) {
      const threshold = fields.proofThreshold.trim();
      if (threshold !== '' && !PROOF_THRESHOLD.test(threshold)) {
        setError('A proof threshold is 0.00 or more, with up to two decimals, or blank.');
        return;
      }
      body.proofThreshold = threshold;
    }
    if (touched.has('noteThreshold')) {
      const threshold = fields.noteThreshold.trim();
      if (threshold !== '' && !NOTE_THRESHOLD.test(threshold)) {
        setError('A note threshold is 0.00 or more, with up to two decimals, or blank.');
        return;
      }
      body.noteThreshold = threshold;
    }
    if (touched.has('rounding')) {
      const rounding = fields.rounding.trim();
      if (rounding !== '' && !ROUNDING.test(rounding)) {
        setError('A rounding mode is half-up or half-even, or blank.');
        return;
      }
      body.rounding = rounding;
    }
    if (touched.has('reviewThreshold')) {
      const threshold = fields.reviewThreshold.trim();
      if (threshold !== '' && !REVIEW_THRESHOLD.test(threshold)) {
        setError('A review threshold is 0.00 or more, with up to two decimals, or blank.');
        return;
      }
      body.reviewThreshold = threshold;
    }
    if (touched.has('escalateAfter')) {
      const escalateAfter = fields.escalateAfter.trim();
      if (escalateAfter !== '' && !TERMS.test(escalateAfter)) {
        setError('An escalation gap is a whole number of days, 0 or more, or blank.');
        return;
      }
      body.escalateAfter = escalateAfter;
    }
    if (touched.has('reviewSla')) {
      const reviewSla = fields.reviewSla.trim();
      if (reviewSla !== '' && !TERMS.test(reviewSla)) {
        setError('A review SLA is a whole number of days, 0 or more, or blank.');
        return;
      }
      body.reviewSla = reviewSla;
    }
    if (touched.has('proofKinds')) {
      const proofKinds = fields.proofKinds.trim();
      const kinds = proofKinds.split(',').map((k) => k.trim());
      if (proofKinds !== '' && !kinds.every((k) => PROOF_KINDS.includes(k))) {
        setError('Proof kinds are receipt or photo, separated by commas, or blank.');
        return;
      }
      body.proofKinds = proofKinds;
    }
    if (touched.has('defaultGrade')) {
      const defaultGrade = fields.defaultGrade.trim();
      if (defaultGrade !== '' && !sheetGrades.includes(defaultGrade)) {
        setError('A default grade is one of the sheet\'s grades, or blank.');
        return;
      }
      body.defaultGrade = defaultGrade;
    }
    for (const [key, which] of [['scoreMid', 'mid'], ['scoreHigh', 'high']] as const) {
      if (!touched.has(key)) continue;
      const value = fields[key].trim();
      if (value !== '' && !/^-?\d{1,9}$/.test(value)) {
        setError(`A ${which} score threshold is a whole number, or blank.`);
        return;
      }
      body[key] = value;
    }
    if (touched.has('scoreLog')) {
      const scoreLog = fields.scoreLog.trim();
      if (scoreLog !== '' && scoreLog !== 'on') {
        setError('The score log is on, or blank.');
        return;
      }
      body.scoreLog = scoreLog;
    }
    if (touched.has('maxOpen')) {
      const maxOpen = fields.maxOpen.trim();
      if (maxOpen !== '' && !TERMS.test(maxOpen)) {
        setError('A maximum of open claims is a whole number, 0 or more, or blank.');
        return;
      }
      body.maxOpen = maxOpen;
    }
    if (touched.has('pendingSla')) {
      const pendingSla = fields.pendingSla.trim();
      if (pendingSla !== '' && !TERMS.test(pendingSla)) {
        setError('A pending SLA is a whole number of days, 0 or more, or blank.');
        return;
      }
      body.pendingSla = pendingSla;
    }
    if (gradesTouched.size > 0) {
      const gradeTerms: Record<string, string> = {};
      for (const grade of GRADES) {
        if (!gradesTouched.has(grade)) continue;
        const terms = (gradeFields[grade] ?? '').trim();
        if (terms !== '' && !TERMS.test(terms)) {
          setError(`Terms for grade ${grade} are a whole number of days, 0 or more, or blank.`);
          return;
        }
        gradeTerms[grade] = terms;
      }
      body.gradeTerms = gradeTerms;
    }
    if (graceTouched.size > 0) {
      const gradeGrace: Record<string, string> = {};
      for (const grade of GRADES) {
        if (!graceTouched.has(grade)) continue;
        const days = (graceFields[grade] ?? '').trim();
        if (days !== '' && !TERMS.test(days)) {
          setError(`Grace days for grade ${grade} are a whole number of days, 0 or more, or blank.`);
          return;
        }
        gradeGrace[grade] = days;
      }
      body.gradeGrace = gradeGrace;
    }
    if (thresholdsTouched.size > 0) {
      const gradeProofThreshold: Record<string, string> = {};
      for (const grade of GRADES) {
        if (!thresholdsTouched.has(grade)) continue;
        const threshold = (thresholdFields[grade] ?? '').trim();
        if (threshold !== '' && !PROOF_THRESHOLD.test(threshold)) {
          setError(`A proof threshold for grade ${grade} is 0.00 or more, with up to two decimals, or blank.`);
          return;
        }
        gradeProofThreshold[grade] = threshold;
      }
      body.gradeProofThreshold = gradeProofThreshold;
    }
    if (limitsTouched.size > 0) {
      const gradeClerkLimit: Record<string, string> = {};
      for (const grade of GRADES) {
        if (!limitsTouched.has(grade)) continue;
        const limit = (limitFields[grade] ?? '').trim();
        if (limit !== '' && !LIMIT.test(limit)) {
          setError(`A clerk limit for grade ${grade} is 0.00 or more, with up to two decimals, or blank.`);
          return;
        }
        gradeClerkLimit[grade] = limit;
      }
      body.gradeClerkLimit = gradeClerkLimit;
    }
    if (permsTouched.size > 0) {
      const perms: Record<string, string> = {};
      for (const action of ACTIONS) {
        if (!permsTouched.has(action)) continue;
        const roles = (permFields[action] ?? '').trim();
        if (roles !== '' && !roles.split(',').every((r) => (PERM_ROLES as readonly string[]).includes(r.trim()))) {
          setError(`Who may ${action} is clerk or steward, separated by commas, or blank.`);
          return;
        }
        perms[action] = roles;
      }
      body.perms = perms;
    }
    const from: Record<string, string> = {};
    const datedKeys: string[] = [
      ...Object.entries(DATED_KEYS).filter(([key]) => touched.has(key as Key)).map(([, store]) => store),
      ...GRADES.filter((grade) => gradesTouched.has(grade)).map((grade) => `terms-${grade}`),
      ...GRADES.filter((grade) => graceTouched.has(grade)).map(gradeGraceKey),
      ...GRADES.filter((grade) => thresholdsTouched.has(grade)).map(gradeProofThresholdKey),
      ...GRADES.filter((grade) => limitsTouched.has(grade)).map(gradeClerkLimitKey),
    ];
    for (const key of datedKeys) {
      const date = (fromFields[key] ?? '').trim();
      if (date === '') continue;
      if (!POLICY_DATE.test(date) || Number.isNaN(Date.parse(date))) {
        setError('A from-date is a date, yyyy-MM-dd, or blank.');
        return;
      }
      from[key] = date;
    }
    if (Object.keys(from).length > 0) body.from = from;
    if (Object.keys(body).length === 0) {
      setError('Type a setting to save.');
      return;
    }
    sent.current = new Set(touched);
    once((done) => save.mutate(body, { onSettled: done }));
  };

  const touch = (key: Key) => setTouched((t) => (t.has(key) ? t : new Set(t).add(key)));
  const change = (key: Key, value: string) => {
    setFields((f) => ({ ...f, [key]: value }));
    touch(key);
    setError('');
  };
  const changeFrom = (key: string, value: string) => {
    setFromFields((f) => ({ ...f, [key]: value }));
    setError('');
  };
  // The from-date input of a figure setting: blank saves it from the sheet's today.
  const fromInput = (key: string) => (
    <label>
      from{' '}
      <input
        data-testid={`setting-${key}-from`}
        placeholder="today"
        value={fromFields[key] ?? ''}
        onChange={(e) => changeFrom(key, e.target.value)}
      />
    </label>
  );
  const touchGrade = (grade: string) => setGradesTouched((t) => (t.has(grade) ? t : new Set(t).add(grade)));
  const changeGrade = (grade: string, value: string) => {
    setGradeFields((f) => ({ ...f, [grade]: value }));
    touchGrade(grade);
    setError('');
  };

  const touchGrace = (grade: string) => setGraceTouched((t) => (t.has(grade) ? t : new Set(t).add(grade)));
  const changeGrace = (grade: string, value: string) => {
    setGraceFields((f) => ({ ...f, [grade]: value }));
    touchGrace(grade);
    setError('');
  };

  const touchThreshold = (grade: string) => setThresholdsTouched((t) => (t.has(grade) ? t : new Set(t).add(grade)));
  const changeThreshold = (grade: string, value: string) => {
    setThresholdFields((f) => ({ ...f, [grade]: value }));
    touchThreshold(grade);
    setError('');
  };

  const touchLimit = (grade: string) => setLimitsTouched((t) => (t.has(grade) ? t : new Set(t).add(grade)));
  const changeLimit = (grade: string, value: string) => {
    setLimitFields((f) => ({ ...f, [grade]: value }));
    touchLimit(grade);
    setError('');
  };

  const touchPerm = (action: string) => setPermsTouched((t) => (t.has(action) ? t : new Set(t).add(action)));
  const changePerm = (action: string, value: string) => {
    setPermFields((f) => ({ ...f, [action]: value }));
    touchPerm(action);
    setError('');
  };

  return (
    <section data-testid="settings">
      <h2>Settings</h2>
      <form onSubmit={submit} data-testid="settings-form">
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            inputMode="decimal"
            value={fields.clerkLimit}
            onFocus={() => touch('clerkLimit')}
            onChange={(e) => change('clerkLimit', e.target.value)}
          />
        </label>{' '}
        {fromInput('clerk-limit')}{' '}
        <span>
          now: <strong data-testid="setting-clerk-limit-current">{settings.data?.clerkLimit ?? ''}</strong>
        </span>{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            <label>
              Clerk limit for grade {grade}{' '}
              <input
                data-testid={`setting-${gradeClerkLimitKey(grade)}`}
                inputMode="decimal"
                placeholder="sheet's"
                value={limitFields[grade] ?? ''}
                onFocus={() => touchLimit(grade)}
                onChange={(e) => changeLimit(grade, e.target.value)}
              />
            </label>{' '}
            {fromInput(gradeClerkLimitKey(grade))}{' '}
            <span>
              now:{' '}
              <strong data-testid={`setting-${gradeClerkLimitKey(grade)}-current`}>
                {settings.data?.gradeClerkLimit?.[grade] ?? ''}
              </strong>
            </span>{' '}
          </span>
        ))}
        <label>
          Floor{' '}
          <input
            data-testid="setting-floor"
            inputMode="decimal"
            value={fields.floor}
            onFocus={() => touch('floor')}
            onChange={(e) => change('floor', e.target.value)}
          />
        </label>{' '}
        {fromInput('floor')}{' '}
        <span>
          now: <strong data-testid="setting-floor-current">{settings.data?.floor ?? ''}</strong>
        </span>{' '}
        <label>
          Terms (days){' '}
          <input
            data-testid="setting-terms"
            inputMode="numeric"
            value={fields.terms}
            onFocus={() => touch('terms')}
            onChange={(e) => change('terms', e.target.value)}
          />
        </label>{' '}
        {fromInput('terms')}{' '}
        <span>
          now: <strong data-testid="setting-terms-current">{settings.data?.terms ?? ''}</strong>
        </span>{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            <label>
              Terms for grade {grade}{' '}
              <input
                data-testid={`setting-terms-${grade}`}
                inputMode="numeric"
                placeholder="sheet's"
                value={gradeFields[grade] ?? ''}
                onFocus={() => touchGrade(grade)}
                onChange={(e) => changeGrade(grade, e.target.value)}
              />
            </label>{' '}
            {fromInput(`terms-${grade}`)}{' '}
            <span>
              now: <strong data-testid={`setting-terms-${grade}-current`}>{settings.data?.gradeTerms?.[grade] ?? ''}</strong>
            </span>{' '}
          </span>
        ))}
        {GRADES.map((grade) => (
          <span key={grade}>
            <label>
              Grace days for grade {grade}{' '}
              <input
                data-testid={`setting-${gradeGraceKey(grade)}`}
                inputMode="numeric"
                placeholder="none"
                value={graceFields[grade] ?? ''}
                onFocus={() => touchGrace(grade)}
                onChange={(e) => changeGrace(grade, e.target.value)}
              />
            </label>{' '}
            {fromInput(gradeGraceKey(grade))}{' '}
            <span>
              now: <strong data-testid={`setting-${gradeGraceKey(grade)}-current`}>{settings.data?.gradeGrace?.[grade] ?? ''}</strong>
            </span>{' '}
          </span>
        ))}
        <label>
          Late percent{' '}
          <input
            data-testid="setting-late-pct"
            inputMode="decimal"
            value={fields.latePct}
            onFocus={() => touch('latePct')}
            onChange={(e) => change('latePct', e.target.value)}
          />
        </label>{' '}
        {fromInput('late-pct')}{' '}
        <span>
          now: <strong data-testid="setting-late-pct-current">{settings.data?.latePct ?? ''}</strong>
        </span>{' '}
        <label>
          Policy date{' '}
          <input
            data-testid="setting-policy-date"
            placeholder="yyyy-mm-dd"
            value={fields.policyDate}
            onFocus={() => touch('policyDate')}
            onChange={(e) => change('policyDate', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-policy-date-current">{settings.data?.policyDate ?? ''}</strong>
        </span>{' '}
        <label>
          Period numbering from{' '}
          <input
            data-testid="setting-period-numbering-from"
            placeholder="yyyy-mm-dd"
            value={fields.periodNumberingFrom}
            onFocus={() => touch('periodNumberingFrom')}
            onChange={(e) => change('periodNumberingFrom', e.target.value)}
          />
        </label>{' '}
        <span>
          now:{' '}
          <strong data-testid="setting-period-numbering-from-current">{settings.data?.periodNumberingFrom ?? ''}</strong>
        </span>{' '}
        <label>
          Grace days{' '}
          <input
            data-testid="setting-grace-days"
            inputMode="numeric"
            placeholder="off"
            value={fields.graceDays}
            onFocus={() => touch('graceDays')}
            onChange={(e) => change('graceDays', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-grace-days-current">{settings.data?.graceDays ?? ''}</strong>
        </span>{' '}
        <label>
          Entry date{' '}
          <input
            data-testid="setting-entry-date"
            placeholder="off"
            value={fields.entryDate}
            onFocus={() => touch('entryDate')}
            onChange={(e) => change('entryDate', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-entry-date-current">{settings.data?.entryDate ?? ''}</strong>
        </span>{' '}
        <label>
          Pot{' '}
          <input
            data-testid="setting-pot"
            inputMode="decimal"
            placeholder="none"
            value={fields.pot}
            onFocus={() => touch('pot')}
            onChange={(e) => change('pot', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-pot-current">{settings.data?.pot ?? ''}</strong>
        </span>{' '}
        <label>
          Alert threshold{' '}
          <input
            data-testid="setting-alert-threshold"
            inputMode="decimal"
            placeholder="off"
            value={fields.alertThreshold}
            onFocus={() => touch('alertThreshold')}
            onChange={(e) => change('alertThreshold', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-alert-threshold-current">{settings.data?.alertThreshold ?? ''}</strong>
        </span>{' '}
        <label>
          Fiscal year starts in month{' '}
          <input
            data-testid="setting-fiscal-start-month"
            inputMode="numeric"
            placeholder="1"
            value={fields.fiscalStartMonth}
            onFocus={() => touch('fiscalStartMonth')}
            onChange={(e) => change('fiscalStartMonth', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-fiscal-start-month-current">{settings.data?.fiscalStartMonth ?? ''}</strong>
        </span>{' '}
        <label>
          Parent sheet{' '}
          <input
            data-testid="setting-parent"
            placeholder="none"
            value={fields.parent}
            onFocus={() => touch('parent')}
            onChange={(e) => change('parent', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-parent-current">{settings.data?.parent ?? ''}</strong>
        </span>{' '}
        <label>
          Cap{' '}
          <input
            data-testid="setting-cap"
            inputMode="decimal"
            placeholder="none"
            value={fields.cap}
            onFocus={() => touch('cap')}
            onChange={(e) => change('cap', e.target.value)}
          />
        </label>{' '}
        {fromInput('cap')}{' '}
        <span>
          now: <strong data-testid="setting-cap-current">{settings.data?.cap ?? ''}</strong>
        </span>{' '}
        <label>
          Proof threshold{' '}
          <input
            data-testid="setting-proof-threshold"
            inputMode="decimal"
            placeholder="none"
            value={fields.proofThreshold}
            onFocus={() => touch('proofThreshold')}
            onChange={(e) => change('proofThreshold', e.target.value)}
          />
        </label>{' '}
        {fromInput('proof-threshold')}{' '}
        <span>
          now: <strong data-testid="setting-proof-threshold-current">{settings.data?.proofThreshold ?? ''}</strong>
        </span>{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            <label>
              Proof threshold for grade {grade}{' '}
              <input
                data-testid={`setting-${gradeProofThresholdKey(grade)}`}
                inputMode="decimal"
                placeholder="sheet's"
                value={thresholdFields[grade] ?? ''}
                onFocus={() => touchThreshold(grade)}
                onChange={(e) => changeThreshold(grade, e.target.value)}
              />
            </label>{' '}
            {fromInput(gradeProofThresholdKey(grade))}{' '}
            <span>
              now:{' '}
              <strong data-testid={`setting-${gradeProofThresholdKey(grade)}-current`}>
                {settings.data?.gradeProofThreshold?.[grade] ?? ''}
              </strong>
            </span>{' '}
          </span>
        ))}
        <label>
          Note threshold{' '}
          <input
            data-testid="setting-note-threshold"
            inputMode="decimal"
            placeholder="none"
            value={fields.noteThreshold}
            onFocus={() => touch('noteThreshold')}
            onChange={(e) => change('noteThreshold', e.target.value)}
          />
        </label>{' '}
        {fromInput('note-threshold')}{' '}
        <span>
          now: <strong data-testid="setting-note-threshold-current">{settings.data?.noteThreshold ?? ''}</strong>
        </span>{' '}
        <label>
          Rounding{' '}
          <input
            data-testid="setting-rounding"
            placeholder="none"
            value={fields.rounding}
            onFocus={() => touch('rounding')}
            onChange={(e) => change('rounding', e.target.value)}
          />
        </label>{' '}
        {fromInput('rounding')}{' '}
        <span>
          now: <strong data-testid="setting-rounding-current">{settings.data?.rounding ?? ''}</strong>
        </span>{' '}
        <label>
          Review threshold{' '}
          <input
            data-testid="setting-review-threshold"
            inputMode="decimal"
            placeholder="none"
            value={fields.reviewThreshold}
            onFocus={() => touch('reviewThreshold')}
            onChange={(e) => change('reviewThreshold', e.target.value)}
          />
        </label>{' '}
        {fromInput('review-threshold')}{' '}
        <span>
          now: <strong data-testid="setting-review-threshold-current">{settings.data?.reviewThreshold ?? ''}</strong>
        </span>{' '}
        <label>
          Escalate after days{' '}
          <input
            data-testid="setting-escalate-after"
            inputMode="numeric"
            placeholder="off"
            value={fields.escalateAfter}
            onFocus={() => touch('escalateAfter')}
            onChange={(e) => change('escalateAfter', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-escalate-after-current">{settings.data?.escalateAfter ?? ''}</strong>
        </span>{' '}
        <label>
          Review SLA days{' '}
          <input
            data-testid="setting-review-sla"
            inputMode="numeric"
            placeholder="none"
            value={fields.reviewSla}
            onFocus={() => touch('reviewSla')}
            onChange={(e) => change('reviewSla', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-review-sla-current">{settings.data?.reviewSla ?? ''}</strong>
        </span>{' '}
        <label>
          Proof kinds{' '}
          <input
            data-testid="setting-proof-kinds"
            placeholder="none"
            value={fields.proofKinds}
            onFocus={() => touch('proofKinds')}
            onChange={(e) => change('proofKinds', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-proof-kinds-current">{settings.data?.proofKinds ?? ''}</strong>
        </span>{' '}
        <label>
          Default grade{' '}
          <input
            data-testid="setting-default-grade"
            placeholder="A"
            value={fields.defaultGrade}
            onFocus={() => touch('defaultGrade')}
            onChange={(e) => change('defaultGrade', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-default-grade-current">{settings.data?.defaultGrade ?? ''}</strong>
        </span>{' '}
        <label>
          Mid score{' '}
          <input
            data-testid="setting-score-mid"
            inputMode="numeric"
            placeholder="none"
            value={fields.scoreMid}
            onFocus={() => touch('scoreMid')}
            onChange={(e) => change('scoreMid', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-score-mid-current">{settings.data?.scoreMid ?? ''}</strong>
        </span>{' '}
        <label>
          High score{' '}
          <input
            data-testid="setting-score-high"
            inputMode="numeric"
            placeholder="none"
            value={fields.scoreHigh}
            onFocus={() => touch('scoreHigh')}
            onChange={(e) => change('scoreHigh', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-score-high-current">{settings.data?.scoreHigh ?? ''}</strong>
        </span>{' '}
        <label>
          Score log{' '}
          <input
            data-testid="setting-score-log"
            placeholder="off"
            value={fields.scoreLog}
            onFocus={() => touch('scoreLog')}
            onChange={(e) => change('scoreLog', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-score-log-current">{settings.data?.scoreLog ?? ''}</strong>
        </span>{' '}
        <label>
          Max open claims per person{' '}
          <input
            data-testid="setting-max-open"
            inputMode="numeric"
            placeholder="none"
            value={fields.maxOpen}
            onFocus={() => touch('maxOpen')}
            onChange={(e) => change('maxOpen', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-max-open-current">{settings.data?.maxOpen ?? ''}</strong>
        </span>{' '}
        <label>
          Pending SLA days{' '}
          <input
            data-testid="setting-pending-sla"
            inputMode="numeric"
            placeholder="none"
            value={fields.pendingSla}
            onFocus={() => touch('pendingSla')}
            onChange={(e) => change('pendingSla', e.target.value)}
          />
        </label>{' '}
        <span>
          now: <strong data-testid="setting-pending-sla-current">{settings.data?.pendingSla ?? ''}</strong>
        </span>{' '}
        {ACTIONS.map((action) => (
          <span key={action}>
            <label>
              Who may {action}{' '}
              <input
                data-testid={`setting-perm-${action}`}
                placeholder={STANDING_RULE[action]}
                value={permFields[action] ?? ''}
                onFocus={() => touchPerm(action)}
                onChange={(e) => changePerm(action, e.target.value)}
              />
            </label>{' '}
            <span>
              now: <strong data-testid={`setting-perm-${action}-current`}>{settings.data?.perms?.[action] ?? ''}</strong>
            </span>{' '}
          </span>
        ))}
        <span>
          Policy version <strong data-testid="policy-version">{settings.data ? policyVersionOn(settings.data) : ''}</strong>
        </span>{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {error && (
          <p role="alert" data-testid="settings-error">
            {error}
          </p>
        )}
      </form>
    </section>
  );
}

// The sheet's roles: a form that makes a user a clerk, a steward or a viewer, and every role assigned.
function Roles({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const roles = useRoles(sheetId);
  const [fields, setFields] = useState<Role>({ user: '', role: '' });
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const save = useMutation({
    mutationFn: (body: Role) =>
      postJson<Role>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`, body, user),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: rolesKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: ['sheet', sheetId, 'my-role'] });
    },
    onError: () => setError('That role could not be set.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const name = fields.user.trim();
    const role = fields.role.trim();
    if (name === '') {
      setError('A user name is required.');
      return;
    }
    if (!(ROLES as readonly string[]).includes(role)) {
      setError('A role is clerk, steward, viewer or restricted.');
      return;
    }
    once((done) => save.mutate({ user: name, role }, { onSettled: done }));
  };

  const change = (next: Partial<Role>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
  };

  return (
    <section data-testid="roles">
      <h2>Roles</h2>
      <form onSubmit={submit} data-testid="role-form">
        <label>
          User{' '}
          <input data-testid="role-form-user" value={fields.user} onChange={(e) => change({ user: e.target.value })} />
        </label>{' '}
        <label>
          Role{' '}
          <input
            data-testid="role-form-role"
            list="role-options"
            value={fields.role}
            onChange={(e) => change({ role: e.target.value })}
          />
          <datalist id="role-options">
            {ROLES.map((r) => (
              <option key={r} value={r} />
            ))}
          </datalist>
        </label>{' '}
        <button type="submit" data-testid="role-form-submit" disabled={save.isPending}>
          Set role
        </button>
        {error && (
          <p role="alert" data-testid="role-form-error">
            {error}
          </p>
        )}
      </form>
      <table data-testid="role-list">
        <thead>
          <tr>
            <th>User</th>
            <th>Role</th>
          </tr>
        </thead>
        <tbody>
          {(roles.data ?? []).map((r) => (
            <tr key={r.user} data-testid={`role-row-${r.user}`}>
              <td data-testid="role-user">{r.user}</td>
              <td data-testid="role-value">{r.role}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contributions = [
  SheetBody.fill({ order: -9, Component: SettingsForm }),
  SheetBody.fill({ order: -8, Component: Roles }),
];
