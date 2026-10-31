import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { LIMIT, ROLES, settingsKey, useSettings, type Settings } from './useSettings';

/**
 * The sheet's settings: its clerk limit (a clerk settles a claim only when its due is at or below
 * it; blank is no limit), and the roles given to user names (clerk, the default, or steward).
 */
function SettingsPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data } = useSettings(sheet);
  const base = `/api/sheets/${encodeURIComponent(sheet)}`;
  const [limit, setLimit] = useState('');
  const [limitInvalid, setLimitInvalid] = useState(false);
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [roleInvalid, setRoleInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const saveSettings = useMutation({
    mutationFn: (clerkLimit: string) => postJson<Settings>(`${base}/settings`, { clerkLimit, user }),
    onSuccess: (saved) => {
      setLimit('');
      queryClient.setQueryData(settingsKey(sheet), saved);
    },
  });
  const saveRole = useMutation({
    mutationFn: (r: { name: string; role: string }) => postJson<Settings>(`${base}/roles`, { ...r, user }),
    onSuccess: (saved) => {
      setName('');
      setRole('');
      queryClient.setQueryData(settingsKey(sheet), saved);
    },
  });

  // An error goes as soon as its form is changed or submitted again.
  const clearSettingsError = () => {
    saveSettings.reset();
    setLimitInvalid(false);
  };
  const clearRoleError = () => {
    saveRole.reset();
    setRoleInvalid(false);
  };

  return (
    <section data-testid="settings-panel">
      <h2>Settings</h2>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!LIMIT.test(limit.trim())) {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            saveSettings.reset();
            setLimitInvalid(true);
            return;
          }
          setLimitInvalid(false);
          submitOnce(() => saveSettings.mutateAsync(limit.trim()));
        }}
      >
        {/* While a save is in flight the limit in force is not yet known: saving the same value again
            shows it only once the save has landed. */}
        Clerk limit:{' '}
        <strong data-testid="setting-clerk-limit-current">
          {saveSettings.isPending ? '' : (data?.clerkLimit ?? '')}
        </strong>{' '}
        <label>
          Set to{' '}
          <input
            data-testid="setting-clerk-limit"
            placeholder="blank: no limit"
            value={limit}
            onChange={(e) => {
              setLimit(e.target.value);
              clearSettingsError();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="settings-save" disabled={saveSettings.isPending}>
          Save
        </button>
        {(limitInvalid || saveSettings.isError) && (
          <span data-testid="settings-error" role="alert">
            {' '}The clerk limit must be an amount with up to two decimals, or blank for no limit.
          </span>
        )}
      </form>
      <form
        data-testid="role-form"
        onSubmit={(e) => {
          e.preventDefault();
          const r = role.trim().toLowerCase();
          if (name.trim() === '' || !(ROLES as readonly string[]).includes(r)) {
            saveRole.reset();
            setRoleInvalid(true);
            return;
          }
          setRoleInvalid(false);
          submitOnce(() => saveRole.mutateAsync({ name: name.trim(), role: r }));
        }}
      >
        <label>
          User{' '}
          <input
            data-testid="role-form-user"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              clearRoleError();
            }}
          />
        </label>{' '}
        <label>
          Role{' '}
          <input
            data-testid="role-form-role"
            placeholder="clerk or steward"
            list="role-options"
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              clearRoleError();
            }}
          />
          <datalist id="role-options">
            {ROLES.map((r) => (
              <option key={r} value={r} />
            ))}
          </datalist>
        </label>{' '}
        <button type="submit" data-testid="role-form-submit" disabled={saveRole.isPending}>
          Set role
        </button>
        {(roleInvalid || saveRole.isError) && (
          <span data-testid="role-form-error" role="alert">
            {' '}Give a user name the role clerk or steward.
          </span>
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
          {(data?.roles ?? []).map((r) => (
            <tr key={r.name} data-testid={`role-row-${r.name}`}>
              <td>{r.name}</td>
              <td data-testid="role-value">{r.role}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 4, Component: SettingsPanel });
