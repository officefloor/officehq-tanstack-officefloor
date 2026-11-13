import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { ROLES, settingsKey, useSettings } from './settings';
import type { Settings, UserRole } from './settings';
import { useOneAtATime } from '../../query/useOneAtATime';

const EMPTY: UserRole = { user: '', role: '' };

// The roles given to user names on the sheet (any other name is a clerk) and the form that gives one.
export function Roles() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const roles = useSettings().data?.roles ?? [];
  const [form, setForm] = useState<UserRole>(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const save = useMutation({
    mutationFn: (input: UserRole) =>
      putJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/roles?user=${encodeURIComponent(user)}`, input),
    onSuccess: async (saved) => {
      setForm(EMPTY);
      setError('');
      queryClient.setQueryData(settingsKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheet) });
    },
    onError: () => setError('That role could not be set.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const name = form.user.trim();
    const role = form.role.trim().toLowerCase();
    if (name === '') {
      setError('Type a user name.');
      return;
    }
    if (!ROLES.includes(role)) {
      setError(`Type a role: ${ROLES.join(' or ')}.`);
      return;
    }
    once(() => save.mutateAsync({ user: name, role }));
  };

  const change = (patch: Partial<UserRole>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  return (
    <section data-testid="roles">
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
      <form onSubmit={submit} data-testid="role-form">
        <input
          data-testid="role-form-user"
          placeholder="User name"
          value={form.user}
          onChange={(e) => change({ user: e.target.value })}
        />
        <input
          data-testid="role-form-role"
          placeholder={`Role (${ROLES.join(' or ')})`}
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
    </section>
  );
}
