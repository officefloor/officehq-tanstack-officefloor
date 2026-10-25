import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';
import { rolesKey, settingsKey, useRoles, useSettings, type Role, type Setting } from './useAccess';

const ROLES = ['clerk', 'steward'];
const AMOUNT = /^\d{1,10}(\.\d{1,2})?$/;

// The role form: assigns a user name a role, clerk or steward (any name not listed is a clerk). The
// assigned roles are listed under it. The server decides who may settle what.
function RolesPanel({ sheetId }: { sheetId: string; user: string }) {
  const roles = useRoles(sheetId);
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: async (body: { user: string; role: string }) => {
      if (body.user.trim() === '') throw new Error('Enter a user name');
      if (!ROLES.includes(body.role.trim())) throw new Error('A role is clerk or steward');
      return putJson<Role>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`, { ...body, by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: rolesKey(sheetId) });
    },
  });
  const submit = useSubmitOnce(save);
  return (
    <section data-testid="roles-panel">
      <h2>Roles</h2>
      <form
        data-testid="role-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ user: name, role });
        }}
      >
        <label>
          User{' '}
          <input
            data-testid="role-form-user"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Role{' '}
          <input
            data-testid="role-form-role"
            list="role-options"
            placeholder="clerk or steward"
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              save.reset();
            }}
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
        {save.isError && (
          <p data-testid="role-form-error" role="alert">
            {save.error.message}
          </p>
        )}
      </form>
      {roles.isError && (
        <p data-testid="roles-error" role="alert">
          {roles.error.message}
        </p>
      )}
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

// The settings form: the clerk limit, the most a clerk may settle a claim's due for (blank: no limit).
// The value in force shows beside it.
function SettingsPanel({ sheetId }: { sheetId: string; user: string }) {
  const settings = useSettings(sheetId);
  const [limit, setLimit] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const current = (settings.data ?? []).find((s) => s.key === 'clerk-limit')?.value ?? '';
  const save = useMutation({
    mutationFn: async (body: { key: string; value: string }) => {
      const value = body.value.trim();
      if (value !== '' && !AMOUNT.test(value)) {
        throw new Error('A clerk limit is an amount with up to two decimals, or blank for no limit');
      }
      return putJson<Setting>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, { key: body.key, value, by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheetId) });
    },
  });
  const submit = useSubmitOnce(save);
  return (
    <section data-testid="settings-panel">
      <h2>Settings</h2>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ key: 'clerk-limit', value: limit });
        }}
      >
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            inputMode="decimal"
            placeholder="no limit"
            value={limit}
            onChange={(e) => {
              setLimit(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <span>
          In force: <strong data-testid="setting-clerk-limit-current">{current}</strong>
        </span>{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <p data-testid="settings-error" role="alert">
            {save.error.message}
          </p>
        )}
      </form>
      {settings.isError && (
        <p data-testid="settings-load-error" role="alert">
          {settings.error.message}
        </p>
      )}
    </section>
  );
}

export const contributions = [
  SheetBody.fill({ order: 3, Component: RolesPanel }),
  SheetBody.fill({ order: 4, Component: SettingsPanel }),
];
