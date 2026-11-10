import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { CAP, DATED_KEYS, FLOOR, GRADES, LATE_PCT, LIMIT, POLICY_DATE, TERMS, ROLES, rolesKey, settingsKey, useRoles, useSettings } from './access';
import type { Role, Settings, SettingsChange } from './access';

type Key = Exclude<keyof Settings, 'gradeTerms'>;

// The sheet's settings: its clerk limit, the highest due a clerk may settle (blank: no limit), and
// its floor, the least a claim's live due may be, and its terms, the days after a claim's date that
// it is due by, and its late percent, what an overdue claim's due gains per full 30 days overdue, and
// its policy date (blank: the sheet's today), before which a claim rounds half up, and its period
// numbering from (blank: off), from which a claim opened is numbered within its period, and each
// grade's own terms (blank: the sheet's terms), fixing the due-by of a claim made for it, and its cap
// (blank: none), the most a person's due-total may be. Only the settings typed in (or stepped into)
// since the last save are sent, so saving one leaves the other as it is.
function SettingsForm({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const settings = useSettings(sheetId);
  const [fields, setFields] = useState<Omit<Settings, 'gradeTerms'>>({
    cap: '',
    clerkLimit: '',
    floor: '',
    terms: '',
    latePct: '',
    policyDate: '',
    periodNumberingFrom: '',
  });
  const [touched, setTouched] = useState<Set<Key>>(new Set());
  const [gradeFields, setGradeFields] = useState<Record<string, string>>({});
  const [gradesTouched, setGradesTouched] = useState<Set<string>>(new Set());
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
    if (touched.has('cap')) {
      const cap = fields.cap.trim();
      if (cap !== '' && !CAP.test(cap)) {
        setError('A cap is 0.00 or more, with up to two decimals, or blank.');
        return;
      }
      body.cap = cap;
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

// The sheet's roles: a form that makes a user a clerk or a steward, and every role assigned.
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
      setError('A role is clerk or steward.');
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
