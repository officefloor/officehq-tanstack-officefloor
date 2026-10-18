import { useState } from 'react';
import type { FormEvent } from 'react';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { LIMIT, ROLES, rolesKey, saveSettings, setRole, settingsKey, useRoles, useSettings } from './settings';

// The sheet's settings: the clerk limit (blank: no limit) and the floor (blank: unchanged), each with
// the one in force shown beside it.
function SettingsPanel() {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const settings = useSettings().data;
  const [limit, setLimit] = useState('');
  const [floor, setFloor] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (input: { clerkLimit: string; floor: string }) => saveSettings(sheet!, user, input),
    onSuccess: async (saved) => {
      queryClient.setQueryData(settingsKey(sheet!), saved);
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheet!) });
    },
    onError: () => setError('The settings could not be saved: the clerk limit must be blank or 0.00 or more, and the floor blank or 0.00 or more, with up to two decimals.'),
  });

  if (!sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const value = limit.trim();
    if (!LIMIT.test(value)) {
      setError('The clerk limit must be blank or 0.00 or more, with up to two decimals.');
      return;
    }
    const floorValue = floor.trim();
    if (!LIMIT.test(floorValue)) {
      setError('The floor must be blank or 0.00 or more, with up to two decimals.');
      return;
    }
    once(() => save.mutateAsync({ clerkLimit: value, floor: floorValue }));
  };

  return (
    <section data-testid="settings">
      <h2>Settings</h2>
      <form data-testid="settings-form" onSubmit={submit}>
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            placeholder="No limit"
            inputMode="decimal"
            value={limit}
            onChange={(e) => {
              setLimit(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <span>
          now <strong data-testid="setting-clerk-limit-current">{settings?.clerkLimit ?? ''}</strong>
        </span>{' '}
        <label>
          Floor{' '}
          <input
            data-testid="setting-floor"
            placeholder="Unchanged"
            inputMode="decimal"
            value={floor}
            onChange={(e) => {
              setFloor(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <span>
          now <strong data-testid="setting-floor-current">{settings?.floor ?? ''}</strong>
        </span>{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {error && <span data-testid="settings-error">{error}</span>}
      </form>
    </section>
  );
}

// The roles on the sheet: a form that gives a user name a role (clerk or steward), replacing any it had,
// and each name given one. A name not listed is a clerk.
function Roles() {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const roles = useRoles().data ?? [];
  const [form, setForm] = useState({ user: '', role: '' });
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (input: { user: string; role: string }) => setRole(sheet!, user, input),
    onSuccess: async () => {
      setForm({ user: '', role: '' });
      await queryClient.invalidateQueries({ queryKey: rolesKey(sheet!) });
    },
    onError: () => setError('The role could not be set: the user name must not be empty and the role must be clerk or steward.'),
  });

  if (!sheet) return null;

  const change = (patch: Partial<typeof form>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const name = form.user.trim();
    const role = form.role.trim();
    if (!name) {
      setError('Type a user name.');
      return;
    }
    if (!(ROLES as string[]).includes(role)) {
      setError(`The role must be ${ROLES.join(' or ')}.`);
      return;
    }
    once(() => save.mutateAsync({ user: name, role }));
  };

  return (
    <section data-testid="roles">
      <h2>Roles</h2>
      <form data-testid="role-form" onSubmit={submit}>
        <input data-testid="role-form-user" placeholder="User name" value={form.user} onChange={(e) => change({ user: e.target.value })} />
        <input
          data-testid="role-form-role"
          placeholder="clerk or steward"
          list="role-options"
          value={form.role}
          onChange={(e) => change({ role: e.target.value })}
        />
        <datalist id="role-options">
          {ROLES.map((r) => (
            <option key={r} value={r} />
          ))}
        </datalist>
        <button type="submit" data-testid="role-form-submit" disabled={save.isPending}>
          Set role
        </button>
        {error && <span data-testid="role-form-error">{error}</span>}
      </form>
      <table data-testid="role-list">
        <thead>
          <tr>
            <th>User</th>
            <th>Role</th>
          </tr>
        </thead>
        <tbody>
          {roles.map((r) => (
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
  HomeMain.fill({ order: 7, Component: SettingsPanel }),
  HomeMain.fill({ order: 8, Component: Roles }),
];
