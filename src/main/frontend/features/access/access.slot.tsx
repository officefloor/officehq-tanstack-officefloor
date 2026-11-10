import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { FLOOR, LATE_PCT, LIMIT, TERMS, ROLES, rolesKey, settingsKey, useRoles, useSettings } from './access';
import type { Role, Settings, SettingsChange } from './access';

type Key = keyof Settings;

// The sheet's settings: its clerk limit, the highest due a clerk may settle (blank: no limit), and
// its floor, the least a claim's live due may be, and its terms, the days after a claim's date that
// it is due by, and its late percent, what an overdue claim's due gains per full 30 days overdue. Only the settings typed in (or stepped into)
// since the last save are sent, so saving one leaves the other as it is.
function SettingsForm({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const settings = useSettings(sheetId);
  const [fields, setFields] = useState<Settings>({ clerkLimit: '', floor: '', terms: '', latePct: '' });
  const [touched, setTouched] = useState<Set<Key>>(new Set());
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const save = useMutation({
    mutationFn: (body: SettingsChange) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, body, user),
    onSuccess: (saved) => {
      setTouched(new Set());
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
        <span>
          now: <strong data-testid="setting-terms-current">{settings.data?.terms ?? ''}</strong>
        </span>{' '}
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
        <span>
          now: <strong data-testid="setting-late-pct-current">{settings.data?.latePct ?? ''}</strong>
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
