import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { ACTIONS, CAP, DATED_KEYS, FLOOR, GRADES, LATE_PCT, LIMIT, PERM_ROLES, POLICY_DATE, PROOF_THRESHOLD, STANDING_RULE, TERMS, ROLES, rolesKey, settingsKey, useRoles, useSettings } from './access';
import type { Role, Settings, SettingsChange } from './access';

type Key = Exclude<keyof Settings, 'gradeTerms' | 'perms'>;

// The sheet's settings: its clerk limit, the highest due a clerk may settle (blank: no limit), and
// its floor, the least a claim's live due may be, and its terms, the days after a claim's date that
// it is due by, and its late percent, what an overdue claim's due gains at each full 30 days overdue (the one in force that day), and
// its policy date (blank: the sheet's today), before which a claim rounds half up, and its period
// numbering from (blank: off), from which a claim opened is numbered within its period, and each
// grade's own terms (blank: the sheet's terms), fixing the due-by of a claim made for it, and its cap
// (blank: none), the most a person's due-total may be, and its proof threshold (blank: none), above
// which a claim's due, on its date, needs a proof to be settled or approved, and its permission
// matrix: for each action the roles that may take it, comma separated (blank: the standing rule), and its
// grace days (blank: off), past which an open period closes when today is changed. Only the settings typed in (or stepped into)
// since the last save are sent, so saving one leaves the other as it is.
function SettingsForm({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const settings = useSettings(sheetId);
  const [fields, setFields] = useState<Omit<Settings, 'gradeTerms' | 'perms'>>({
    cap: '',
    proofThreshold: '',
    clerkLimit: '',
    floor: '',
    terms: '',
    latePct: '',
    policyDate: '',
    periodNumberingFrom: '',
    graceDays: '',
  });
  const [touched, setTouched] = useState<Set<Key>>(new Set());
  const [gradeFields, setGradeFields] = useState<Record<string, string>>({});
  const [gradesTouched, setGradesTouched] = useState<Set<string>>(new Set());
  const [permFields, setPermFields] = useState<Record<string, string>>({});
  const [permsTouched, setPermsTouched] = useState<Set<string>>(new Set());
  // The from-date typed for each figure setting, by its store key (blank: the sheet's today).
  const [fromFields, setFromFields] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const save = useMutation({
    mutationFn: (body: SettingsChange) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, body, user),
    onSuccess: (saved) => {
      setTouched(new Set());
      setGradesTouched(new Set());
      setPermsTouched(new Set());
      queryClient.setQueryData(settingsKey(sheetId), saved);
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
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: rolesKey(sheetId) }),
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
      setError('A role is clerk, steward or viewer.');
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
