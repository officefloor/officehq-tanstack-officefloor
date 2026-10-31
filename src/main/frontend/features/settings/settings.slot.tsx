import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { DAYS, FROM_DATE, TERM_GRADES, LIMIT, PERCENT, POLICY_DATE, ROLES, settingsKey, useSettings, type Settings } from './useSettings';

/** The settings the form sets: each null while its field is untouched, so a save leaves it as it is. */
type SettingsEdit = { clerkLimit: string | null; floor: string | null; terms: string | null; latePct: string | null; policyDate: string | null; periodNumberingFrom: string | null; cap: string | null; gradeTerms: Record<string, string> };

/** The from-date typed for each dated setting, by key (blank: from the sheet's today). */
type Froms = Record<string, string>;

/**
 * The sheet's settings: its clerk limit (a clerk settles a claim only when its due is at or below
 * it; blank is no limit), its floor (a claim's live due is at least it; blank is the default, 1.00),
 * its terms (a claim is due by its date plus this many days; blank is the default, 14), the terms
 * of each grade (a claim of a person of that grade is due by them; blank uses the sheet's terms), its late
 * percent (an overdue claim's due gains it of its figure for every full 30 days; blank is 0.00),
 * its policy date (a claim dated before it rounds half up, on or after it half to even; blank is
 * the sheet's today), its period numbering from (while it is a date on or before the sheet's
 * today, a claim opened is numbered within its period; blank is off), its cap (a person's due-total
 * is at most it, the rest their excess; blank is no cap),
 * and the roles given to user names (clerk, the default, or steward). A save sets only the settings
 * whose fields were touched since the last one. The clerk limit, the floor, the terms (the sheet's and
 * each grade's), the late percent and the cap are dated: a save records each from its from-date (blank
 * is the sheet's today), and each shows the value in force on the sheet's today.
 */
function SettingsPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data } = useSettings(sheet);
  const base = `/api/sheets/${encodeURIComponent(sheet)}`;
  const [edit, setEdit] = useState<SettingsEdit>({ clerkLimit: null, floor: null, terms: null, latePct: null, policyDate: null, periodNumberingFrom: null, cap: null, gradeTerms: {} });
  const [froms, setFroms] = useState<Froms>({});
  const [limitInvalid, setLimitInvalid] = useState(false);
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [roleInvalid, setRoleInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const saveSettings = useMutation({
    mutationFn: ({ e, from }: { e: SettingsEdit; from: Froms }) => postJson<Settings>(`${base}/settings`, { ...e, from, user }),
    onSuccess: (saved) => {
      setEdit({ clerkLimit: null, floor: null, terms: null, latePct: null, policyDate: null, periodNumberingFrom: null, cap: null, gradeTerms: {} });
      setFroms({});
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
  const touch = (key: Exclude<keyof SettingsEdit, 'gradeTerms'>, current: string) =>
    setEdit((was) => (was[key] !== null ? was : { ...was, [key]: current }));
  // The from-date field of a dated setting, by its key.
  const fromField = (key: string) => (
    <label>
      from{' '}
      <input
        data-testid={`setting-${key}-from`}
        placeholder="blank: the sheet's today"
        size={10}
        value={froms[key] ?? ''}
        onChange={(e) => {
          const value = e.target.value;
          setFroms((was) => ({ ...was, [key]: value }));
          clearSettingsError();
        }}
      />
    </label>
  );
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
          const periodNumberingFrom = edit.periodNumberingFrom?.trim() ?? null;
          const cap = edit.cap?.trim() ?? null;
          const gradeTerms = Object.fromEntries(Object.entries(edit.gradeTerms).map(([g, v]) => [g, v.trim()]));
          const from = Object.fromEntries(Object.entries(froms).map(([k, v]) => [k, v.trim()]).filter(([, v]) => v !== ''));
          if (
            (limit !== null && !LIMIT.test(limit)) ||
            (floor !== null && !LIMIT.test(floor)) ||
            (terms !== null && !DAYS.test(terms)) ||
            Object.values(gradeTerms).some((v) => !DAYS.test(v)) ||
            (latePct !== null && !PERCENT.test(latePct)) ||
            (policyDate !== null && !POLICY_DATE.test(policyDate)) ||
            (periodNumberingFrom !== null && !POLICY_DATE.test(periodNumberingFrom)) ||
            (cap !== null && !LIMIT.test(cap)) ||
            Object.values(from).some((v) => !FROM_DATE.test(v))
          ) {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            saveSettings.reset();
            setLimitInvalid(true);
            return;
          }
          setLimitInvalid(false);
          submitOnce(() => saveSettings.mutateAsync({ e: { clerkLimit: limit, floor, terms, latePct, policyDate, periodNumberingFrom, cap, gradeTerms }, from }));
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
        {fromField('clerk-limit')}{' '}
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
        {fromField('floor')}{' '}
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
        {fromField('terms')}{' '}
        {TERM_GRADES.map((g) => {
          const own = data?.gradeTerms?.[g] !== undefined ? String(data.gradeTerms[g]) : '';
          return (
            <span key={g}>
              Terms {g}:{' '}
              <strong data-testid={`setting-terms-${g}-current`}>{saveSettings.isPending ? '' : own}</strong>{' '}
              <label>
                Set to{' '}
                <input
                  data-testid={`setting-terms-${g}`}
                  placeholder="blank: the sheet's terms"
                  value={edit.gradeTerms[g] ?? own}
                  onChange={(e) => {
                    const value = e.target.value;
                    setEdit((was) => ({ ...was, gradeTerms: { ...was.gradeTerms, [g]: value } }));
                    clearSettingsError();
                  }}
                />
              </label>{' '}
              {fromField(`terms-${g}`)}{' '}
            </span>
          );
        })}
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
        {fromField('late-pct')}{' '}
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
        Period numbering from:{' '}
        <strong data-testid="setting-period-numbering-from-current">
          {saveSettings.isPending ? '' : (data?.periodNumberingFrom ?? '')}
        </strong>{' '}
        <label>
          Set to{' '}
          <input
            data-testid="setting-period-numbering-from"
            placeholder="blank: off"
            value={edit.periodNumberingFrom ?? (data?.periodNumberingFrom ?? '')}
            onFocus={() => touch('periodNumberingFrom', data?.periodNumberingFrom ?? '')}
            onChange={(e) => {
              setEdit((was) => ({ ...was, periodNumberingFrom: e.target.value }));
              clearSettingsError();
            }}
          />
        </label>{' '}
        Cap:{' '}
        <strong data-testid="setting-cap-current">
          {saveSettings.isPending ? '' : (data?.cap ?? '')}
        </strong>{' '}
        <label>
          Set to{' '}
          <input
            data-testid="setting-cap"
            placeholder="blank: no cap"
            value={edit.cap ?? (data?.cap ?? '')}
            onFocus={() => touch('cap', data?.cap ?? '')}
            onChange={(e) => {
              setEdit((was) => ({ ...was, cap: e.target.value }));
              clearSettingsError();
            }}
          />
        </label>{' '}
        {fromField('cap')}{' '}
        <button type="submit" data-testid="settings-save" disabled={saveSettings.isPending}>
          Save
        </button>
        {(limitInvalid || saveSettings.isError) && (
          <span data-testid="settings-error" role="alert">
            {' '}The clerk limit and the floor must be amounts with up to two decimals (a blank limit is no
            limit, a blank floor is 1.00), the terms a whole number of days (blank is 14; blank for a grade uses the sheet's terms), and the late percent a percent with up to
            two decimals (blank is 0.00), and the policy date a date, yyyy-mm-dd (blank is the sheet's today), and the period numbering from a date, yyyy-mm-dd (blank is off), and the cap an amount with up to two decimals (blank is no cap), and each from-date a date, yyyy-mm-dd (blank is the sheet's today).
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
