import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { DAYS, LIMIT, PERCENT, POLICY_DATE, ROLES, settingsKey, useSettings, type Settings } from './useSettings';

/** The settings the form sets: each null while its field is untouched, so a save leaves it as it is. */
type SettingsEdit = { clerkLimit: string | null; floor: string | null; terms: string | null; latePct: string | null; policyDate: string | null };

/**
 * The sheet's settings: its clerk limit (a clerk settles a claim only when its due is at or below
 * it; blank is no limit), its floor (a claim's live due is at least it; blank is the default, 1.00),
 * its terms (a claim is due by its date plus this many days; blank is the default, 14), its late
 * percent (an overdue claim's due gains it of its figure for every full 30 days; blank is 0.00),
 * its policy date (a claim dated before it rounds half up, on or after it half to even; blank is
 * the sheet's today),
 * and the roles given to user names (clerk, the default, or steward). A save sets only the settings
 * whose fields were touched since the last one.
 */
function SettingsPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data } = useSettings(sheet);
  const base = `/api/sheets/${encodeURIComponent(sheet)}`;
  const [edit, setEdit] = useState<SettingsEdit>({ clerkLimit: null, floor: null, terms: null, latePct: null, policyDate: null });
  const [limitInvalid, setLimitInvalid] = useState(false);
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [roleInvalid, setRoleInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const saveSettings = useMutation({
    mutationFn: (e: SettingsEdit) => postJson<Settings>(`${base}/settings`, { ...e, user }),
    onSuccess: (saved) => {
      setEdit({ clerkLimit: null, floor: null, terms: null, latePct: null, policyDate: null });
      queryClient.setQueryData(settingsKey(sheet), saved);
      // The policy date decides how each claim's live figures round.
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
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
  // A field is set by the next save once it has been touched, starting from the value in force.
  const touch = (key: keyof SettingsEdit, current: string) =>
    setEdit((was) => (was[key] !== null ? was : { ...was, [key]: current }));
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
          const limit = edit.clerkLimit?.trim() ?? null;
          const floor = edit.floor?.trim() ?? null;
          const terms = edit.terms?.trim() ?? null;
          const latePct = edit.latePct?.trim() ?? null;
          const policyDate = edit.policyDate?.trim() ?? null;
          if (
            (limit !== null && !LIMIT.test(limit)) ||
            (floor !== null && !LIMIT.test(floor)) ||
            (terms !== null && !DAYS.test(terms)) ||
            (latePct !== null && !PERCENT.test(latePct)) ||
            (policyDate !== null && !POLICY_DATE.test(policyDate))
          ) {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            saveSettings.reset();
            setLimitInvalid(true);
            return;
          }
          setLimitInvalid(false);
          submitOnce(() => saveSettings.mutateAsync({ clerkLimit: limit, floor, terms, latePct, policyDate }));
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
            value={edit.clerkLimit ?? (data?.clerkLimit ?? '')}
            onFocus={() => touch('clerkLimit', data?.clerkLimit ?? '')}
            onChange={(e) => {
              setEdit((was) => ({ ...was, clerkLimit: e.target.value }));
              clearSettingsError();
            }}
          />
        </label>{' '}
        Floor:{' '}
        <strong data-testid="setting-floor-current">
          {saveSettings.isPending ? '' : (data?.floor ?? '')}
        </strong>{' '}
        <label>
          Set to{' '}
          <input
            data-testid="setting-floor"
            placeholder="blank: 1.00"
            value={edit.floor ?? (data?.floor ?? '')}
            onFocus={() => touch('floor', data?.floor ?? '')}
            onChange={(e) => {
              setEdit((was) => ({ ...was, floor: e.target.value }));
              clearSettingsError();
            }}
          />
        </label>{' '}
        Terms:{' '}
        <strong data-testid="setting-terms-current">
          {saveSettings.isPending ? '' : (data?.terms ?? '')}
        </strong>{' '}
        <label>
          Set to{' '}
          <input
            data-testid="setting-terms"
            placeholder="blank: 14 days"
            value={edit.terms ?? String(data?.terms ?? '')}
            onFocus={() => touch('terms', String(data?.terms ?? ''))}
            onChange={(e) => {
              setEdit((was) => ({ ...was, terms: e.target.value }));
              clearSettingsError();
            }}
          />
        </label>{' '}
        Late percent:{' '}
        <strong data-testid="setting-late-pct-current">
          {saveSettings.isPending ? '' : (data?.latePct ?? '')}
        </strong>{' '}
        <label>
          Set to{' '}
          <input
            data-testid="setting-late-pct"
            placeholder="blank: 0.00"
            value={edit.latePct ?? (data?.latePct ?? '')}
            onFocus={() => touch('latePct', data?.latePct ?? '')}
            onChange={(e) => {
              setEdit((was) => ({ ...was, latePct: e.target.value }));
              clearSettingsError();
            }}
          />
        </label>{' '}
        Policy date:{' '}
        <strong data-testid="setting-policy-date-current">
          {saveSettings.isPending ? '' : (data?.policyDate ?? '')}
        </strong>{' '}
        <label>
          Set to{' '}
          <input
            data-testid="setting-policy-date"
            placeholder="blank: the sheet's today"
            value={edit.policyDate ?? (data?.policyDate ?? '')}
            onFocus={() => touch('policyDate', data?.policyDate ?? '')}
            onChange={(e) => {
              setEdit((was) => ({ ...was, policyDate: e.target.value }));
              clearSettingsError();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="settings-save" disabled={saveSettings.isPending}>
          Save
        </button>
        {(limitInvalid || saveSettings.isError) && (
          <span data-testid="settings-error" role="alert">
            {' '}The clerk limit and the floor must be amounts with up to two decimals (a blank limit is no
            limit, a blank floor is 1.00), the terms a whole number of days (blank is 14), and the late percent a percent with up to
            two decimals (blank is 0.00), and the policy date a date, yyyy-mm-dd (blank is the sheet's today).
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
