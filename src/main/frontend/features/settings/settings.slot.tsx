import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useUser } from '../session/useUser';
import { ROLES, rolesKey, rolesQuery, settingsKey, settingsQuery } from './settingsQuery';
import type { Role, RoleName, Settings as SheetSettings, SettingsChange } from './settingsQuery';

const AMOUNT = /^\d{1,9}(\.\d{1,2})?$/;
const DAYS = /^\d{1,4}$/;

/** The sheet's settings — the clerk limit a clerk may settle up to (blank: no limit) and the floor a
 *  claim's live due is at least, and the terms (days after its date a claim is due by) and the late percent (what an overdue claim gains per full 30 days) — and the form
 *  they are saved through. A blank floor, terms or late percent is left as it is, and so is a blank clerk
 *  limit saved with any of them. */
function Settings({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const settings = useQuery(settingsQuery(sheetId));
  const [limit, setLimit] = useState('');
  const [floor, setFloor] = useState('');
  const [terms, setTerms] = useState('');
  const [latePct, setLatePct] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: SettingsChange) =>
      putJson<SheetSettings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheetId) });
      setLimit('');
      setFloor('');
      setTerms('');
      setLatePct('');
    },
    onError: () => setError('The settings could not be saved.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const l = limit.trim();
    if (l !== '' && !AMOUNT.test(l)) {
      setError('The clerk limit must be an amount of 0.00 or more, with up to two decimals.');
      return;
    }
    const f = floor.trim();
    if (f !== '' && !AMOUNT.test(f)) {
      setError('The floor must be an amount of 0.00 or more, with up to two decimals.');
      return;
    }
    const t = terms.trim();
    if (t !== '' && !DAYS.test(t)) {
      setError('The terms must be a whole number of days, 0 or more.');
      return;
    }
    const p = latePct.trim();
    if (p !== '' && !AMOUNT.test(p)) {
      setError('The late percent must be a percent of 0.00 or more, with up to two decimals.');
      return;
    }
    const body: SettingsChange = {};
    if (l !== '' || (f === '' && t === '' && p === '')) body.clerkLimit = l;
    if (f !== '') body.floor = f;
    if (t !== '') body.terms = t;
    if (p !== '') body.latePct = p;
    once(() => save.mutateAsync(body));
  };

  return (
    <section data-testid="settings">
      <h3>Settings</h3>
      <form data-testid="settings-form" onSubmit={submit}>
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            placeholder="no limit"
            value={limit}
            onChange={(e) => {
              setLimit(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Floor{' '}
          <input
            data-testid="setting-floor"
            placeholder="unchanged"
            value={floor}
            onChange={(e) => {
              setFloor(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Terms (days){' '}
          <input
            data-testid="setting-terms"
            placeholder="unchanged"
            value={terms}
            onChange={(e) => {
              setTerms(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Late percent{' '}
          <input
            data-testid="setting-late-pct"
            placeholder="unchanged"
            value={latePct}
            onChange={(e) => {
              setLatePct(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {error && <p data-testid="settings-error">{error}</p>}
      </form>
      {settings.isError && <p data-testid="settings-load-error">Could not load the settings.</p>}
      <p>
        Clerk limit in force: <span data-testid="setting-clerk-limit-current">{settings.data?.clerkLimit ?? ''}</span>
      </p>
      <p>
        Floor in force: <span data-testid="setting-floor-current">{settings.data?.floor ?? ''}</span>
      </p>
      <p>
        Terms in force: <span data-testid="setting-terms-current">{settings.data?.terms ?? ''}</span>
      </p>
      <p>
        Late percent in force: <span data-testid="setting-late-pct-current">{settings.data?.latePct ?? ''}</span>
      </p>
    </section>
  );
}

/** The roles the sheet gives user names (any name not listed is a clerk), and the form a role is
 *  set through. */
function Roles({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const roles = useQuery(rolesQuery(sheetId));
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: Role) =>
      putJson<Role>(
        `/api/sheets/${encodeURIComponent(sheetId)}/roles/${encodeURIComponent(body.user)}`,
        { role: body.role },
        user,
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: rolesKey(sheetId) });
      setName('');
      setRole('');
    },
    onError: () => setError('The role could not be set.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const n = name.trim();
    const r = role.trim();
    if (n === '') {
      setError('A user name is needed.');
      return;
    }
    if (!ROLES.includes(r as RoleName)) {
      setError('The role must be clerk or steward.');
      return;
    }
    once(() => save.mutateAsync({ user: n, role: r as RoleName }));
  };

  return (
    <section data-testid="roles">
      <h3>Roles</h3>
      <form data-testid="role-form" onSubmit={submit}>
        <label>
          User{' '}
          <input
            data-testid="role-form-user"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Role{' '}
          <input
            data-testid="role-form-role"
            placeholder="clerk or steward"
            list="role-names"
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              setError('');
            }}
          />
          <datalist id="role-names">
            {ROLES.map((r) => (
              <option key={r} value={r} />
            ))}
          </datalist>
        </label>{' '}
        <button type="submit" data-testid="role-form-submit" disabled={save.isPending}>
          Set role
        </button>
        {error && <p data-testid="role-form-error">{error}</p>}
      </form>
      {roles.isError && <p data-testid="roles-load-error">Could not load the roles.</p>}
      <table data-testid="roles-list">
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

// Keyed by sheet, so a typed value or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 6,
  Component: ({ sheetId }) => (
    <>
      <Settings key={`settings-${sheetId}`} sheetId={sheetId} />
      <Roles key={`roles-${sheetId}`} sheetId={sheetId} />
    </>
  ),
});
