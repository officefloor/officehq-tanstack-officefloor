import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';
import { rolesKey, settingsKey, useRoles, useSettings, FLOOR_DEFAULT, LATE_PCT_DEFAULT, TERMS_DEFAULT, type Role, type Setting } from './useAccess';

const ROLES = ['clerk', 'steward'];
const AMOUNT = /^\d{1,10}(\.\d{1,2})?$/;
const DAYS = /^\d{1,5}$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;
const ROLE_LIST = /^\s*(clerk|steward)\s*(,\s*(clerk|steward)\s*)*$/;

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

// The settings form: the clerk limit, the most a clerk may settle a claim's due for (blank: no limit), and the
// floor, the least a claim's live due may be (1.00 until it is set), and the terms, the days after its date a claim is
// due by (14 until they are set), and each grade's own terms (blank: the sheet's terms), fixed into a claim's due-by when it is added, and the late percent, what an overdue claim's due gains of its figure for every
// full 30 days it is overdue (0.00 until it is set), and the policy date (blank: the sheet's today), before which a
// claim's live figures round half up and from which they round half to even, and period numbering from (blank: off),
// the date from which, on the sheet's today, an opened claim is numbered within its period, and the cap (blank: none), the most
// a person's due-total may be, and the proof threshold (blank: none), the due above which a claim needs a proof before
// it is settled or approved, and the permission matrix: for each of open, settle, void, reassign, close and reopen, the
// roles that may take it (blank: the standing rule, shown as the placeholder). Every setting but the policy date, period numbering from and the permissions is dated: saving it records
// its value from its from-date (blank: the sheet's today), keeping the values recorded from other dates. The value in
// force on the sheet's today shows beside each. Saving sends only the settings whose fields were touched since the last save.
type SettingField = { key: string; label: string; placeholder: string; blank: boolean; valid: RegExp;
  inputMode: 'decimal' | 'numeric' | 'text'; invalid: string; dated?: false };

const SETTINGS: readonly SettingField[] = [
  { key: 'clerk-limit', label: 'Clerk limit', placeholder: 'no limit', blank: true, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'A clerk limit is an amount with up to two decimals, or blank for no limit' },
  { key: 'floor', label: 'Floor', placeholder: FLOOR_DEFAULT, blank: false, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'A floor is an amount with up to two decimals' },
  { key: 'terms', label: 'Terms (days)', placeholder: TERMS_DEFAULT, blank: false, valid: DAYS, inputMode: 'numeric',
    invalid: 'Terms are a whole number of days' },
  ...(['A', 'B', 'C'] as const).map((grade): SettingField => ({ key: `terms-${grade}`, label: `Terms for grade ${grade} (days)`,
    placeholder: 'sheet\'s terms', blank: true, valid: DAYS, inputMode: 'numeric',
    invalid: 'A grade\'s terms are a whole number of days, or blank for the sheet\'s terms' })),
  { key: 'late-pct', label: 'Late percent', placeholder: LATE_PCT_DEFAULT, blank: false, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'A late percent is a number with up to two decimals' },
  { key: 'policy-date', label: 'Policy date', placeholder: 'today', blank: true, valid: DATE, inputMode: 'text',
    invalid: 'A policy date is a date (yyyy-mm-dd), or blank for the sheet\'s today', dated: false },
  { key: 'period-numbering-from', label: 'Period numbering from', placeholder: 'off', blank: true, valid: DATE, inputMode: 'text',
    invalid: 'Period numbering from is a date (yyyy-mm-dd), or blank for off', dated: false },
  { key: 'cap', label: 'Cap', placeholder: 'none', blank: true, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'A cap is an amount with up to two decimals, or blank for none' },
  { key: 'proof-threshold', label: 'Proof threshold', placeholder: 'none', blank: true, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'A proof threshold is an amount with up to two decimals, or blank for none' },
  ...([['open', 'clerk,steward'], ['settle', 'clerk,steward'], ['void', 'clerk,steward'], ['reassign', 'steward'],
    ['close', 'clerk,steward'], ['reopen', 'steward']] as const).map(([action, standing]): SettingField => ({
    key: `perm-${action}`, label: `Who may ${action}`, placeholder: standing, blank: true, valid: ROLE_LIST, inputMode: 'text',
    invalid: 'A permission is a comma separated list of roles (clerk, steward), or blank for the standing rule', dated: false })),
];

function SettingsPanel({ sheetId }: { sheetId: string; user: string }) {
  const settings = useSettings(sheetId);
  const [values, setValues] = useState<Record<string, string>>({});
  const [froms, setFroms] = useState<Record<string, string>>({});
  const [touched, setTouched] = useState<ReadonlySet<string>>(new Set());
  const [user] = useUser();
  const queryClient = useQueryClient();
  const current = (key: string) => (settings.data ?? []).find((s) => s.key === key)?.value ?? '';
  const touch = (key: string) => setTouched((t) => (t.has(key) ? t : new Set(t).add(key)));
  const save = useMutation({
    mutationFn: async (body: { keys: string[]; values: Record<string, string>; froms: Record<string, string> }) => {
      const changes = SETTINGS.filter((s) => body.keys.includes(s.key));
      if (changes.length === 0) throw new Error('Change a setting first');
      for (const s of changes) {
        const value = (body.values[s.key] ?? '').trim();
        if (value === '' ? !s.blank : !s.valid.test(value)) throw new Error(s.invalid);
        const from = (body.froms[s.key] ?? '').trim();
        if (from !== '' && !DATE.test(from)) throw new Error('A from-date is a date (yyyy-mm-dd), or blank for the sheet\'s today');
      }
      for (const s of changes) {
        await putJson<Setting>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`,
          { key: s.key, value: (body.values[s.key] ?? '').trim(),
            ...(s.dated === false ? {} : { from: (body.froms[s.key] ?? '').trim() }), by: user ?? '' });
      }
    },
    onSuccess: async () => {
      setTouched(new Set());
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
          submit({ keys: [...touched], values, froms });
        }}
      >
        {SETTINGS.map((s) => (
          <span key={s.key}>
            <label>
              {s.label}{' '}
              <input
                data-testid={`setting-${s.key}`}
                inputMode={s.inputMode}
                placeholder={s.placeholder}
                value={values[s.key] ?? ''}
                onFocus={() => touch(s.key)}
                onChange={(e) => {
                  const value = e.target.value;
                  setValues((v) => ({ ...v, [s.key]: value }));
                  touch(s.key);
                  save.reset();
                }}
              />
            </label>{' '}
            {s.dated !== false && (
              <>
                <label>
                  from{' '}
                  <input
                    data-testid={`setting-${s.key}-from`}
                    placeholder="today"
                    value={froms[s.key] ?? ''}
                    onFocus={() => touch(s.key)}
                    onChange={(e) => {
                      const value = e.target.value;
                      setFroms((f) => ({ ...f, [s.key]: value }));
                      touch(s.key);
                      save.reset();
                    }}
                  />
                </label>{' '}
              </>
            )}
            <span>
              In force: <strong data-testid={`setting-${s.key}-current`}>{current(s.key)}</strong>
            </span>{' '}
          </span>
        ))}
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
