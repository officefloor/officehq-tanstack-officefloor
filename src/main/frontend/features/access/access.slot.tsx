import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';
import { poolsKey } from '../pools/pools.slot';
import { ratesKey } from '../rates/useRates';
import { sheetTodayKey } from '../today/today.slot';
import { delegationsKey, rolesKey, settingsKey, useDelegations, useRoles, useSettings, FLOOR_DEFAULT, LATE_PCT_DEFAULT, TERMS_DEFAULT, type Delegation, type Role, type Setting } from './useAccess';

const ROLES = ['clerk', 'steward', 'viewer'];
const AMOUNT = /^\d{1,10}(\.\d{1,2})?$/;
const DAYS = /^\d{1,5}$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;
const SHEET_ID = /^.{1,200}$/;
const ROLE_LIST = /^\s*(clerk|steward)\s*(,\s*(clerk|steward)\s*)*$/;

// The role form: assigns a user name a role, clerk, steward or viewer (any name not listed is a clerk; a viewer
// sees everything and may change nothing). The
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
      if (!ROLES.includes(body.role.trim())) throw new Error('A role is clerk, steward or viewer');
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
            placeholder="clerk, steward or viewer"
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

// The delegation form: a steward delegates the steward role to a user for a from-date (blank: the sheet's today) to a
// to-date, inclusive by the sheet's today; while the sheet's today is inside the range the user acts as a steward. The
// server refuses anyone but a steward, or a to-date before the from-date. The delegations are listed under it.
function DelegatePanel({ sheetId }: { sheetId: string; user: string }) {
  const delegations = useDelegations(sheetId);
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: async (body: { user: string; from: string; to: string }) => {
      if (body.user.trim() === '') throw new Error('Enter a user name');
      if (body.from.trim() !== '' && !DATE.test(body.from.trim())) throw new Error('Enter a date as YYYY-MM-DD');
      if (body.to.trim() === '') throw new Error('Enter a to-date');
      if (!DATE.test(body.to.trim())) throw new Error('Enter a date as YYYY-MM-DD');
      return postJson<Delegation>(`/api/sheets/${encodeURIComponent(sheetId)}/delegations`, { ...body, by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: delegationsKey(sheetId) });
    },
  });
  const submit = useSubmitOnce(save);
  const field = (testId: string, label: string, value: string, set: (v: string) => void, placeholder?: string) => (
    <label>
      {label}{' '}
      <input
        data-testid={testId}
        placeholder={placeholder}
        value={value}
        onChange={(e) => {
          set(e.target.value);
          save.reset();
        }}
      />
    </label>
  );
  return (
    <section data-testid="delegate-panel">
      <h2>Delegations</h2>
      <form
        data-testid="delegate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ user: name, from, to });
        }}
      >
        {field('delegate-user', 'User', name, setName)}{' '}
        {field('delegate-from', 'From', from, setFrom, 'today')}{' '}
        {field('delegate-to', 'To', to, setTo, 'yyyy-mm-dd')}{' '}
        <button type="submit" data-testid="delegate-submit" disabled={save.isPending}>
          Delegate steward
        </button>
        {save.isError && (
          <p data-testid="delegate-error" role="alert">
            {save.error.message}
          </p>
        )}
      </form>
      {delegations.isError && (
        <p data-testid="delegations-error" role="alert">
          {delegations.error.message}
        </p>
      )}
      <ul data-testid="delegate-list">
        {(delegations.data ?? []).map((d, i) => (
          <li key={i} data-testid="delegate-line">
            <span data-testid="delegate-line-user">{d.user}</span> acts as steward for{' '}
            <span data-testid="delegate-line-by">{d.by}</span> from{' '}
            <span data-testid="delegate-line-from">{d.from}</span> to{' '}
            <span data-testid="delegate-line-to">{d.to}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

// The settings form: the clerk limit, the most a clerk may settle a claim's due for (blank: no limit), and the
// floor, the least a claim's live due may be (1.00 until it is set), and the terms, the days after its date a claim is
// due by (14 until they are set), and each grade's own terms (blank: the sheet's terms), fixed into a claim's due-by when it is added, and each grade's grace (blank: none),
// the days past its due-by a claim of that grade (on its date) goes overdue and counts its late charge from, and the late percent, what an overdue claim's due gains of its figure for every
// full 30 days it is overdue (the percent in force on each 30-day boundary date) (0.00 until it is set), and the policy date (blank: the sheet's today), before which a
// claim's live figures round half up and from which they round half to even, and period numbering from (blank: off),
// the date from which, on the sheet's today, an opened claim is numbered within its period, and the grace days (blank: off),
// the entry date (blank: off), the default date of a new claim in place of the sheet's today, the fiscal start month
// (1 to 12, 1 until it is set), the month the sheet's fiscal year starts in,
// the days after its to-date an open period closes by itself when the sheet's today moves past them, and the cap (blank: none), the most
// a person's due-total may be, and the proof threshold (blank: none), the due above which a claim needs a proof before
// it is settled or approved, and each grade's proof threshold (blank: the sheet's), the one a claim of that grade on its
// date takes in its place, and the permission matrix: for each of open, settle, void, reassign, close and reopen, the
// roles that may take it (blank: the standing rule, shown as the placeholder), and the parent sheet (blank: none), whose rates (then its parent's, up to three levels up) stand in for a grade without one in force on a date. Every setting but the policy date, period numbering from, the entry date, the grace days, the pot, the alert threshold (blank: off; a person whose due-total crosses above it writes an ALERT line), the parent and the permissions is dated: saving it records
// its value from its from-date (blank: the sheet's today), keeping the values recorded from other dates. The value in
// force on the sheet's today shows beside each. Saving sends only the settings whose fields were touched since the last save.
type SettingField = { key: string; label: string; placeholder: string; blank: boolean; valid: RegExp;
  inputMode: 'decimal' | 'numeric' | 'text'; invalid: string; dated?: false; starting?: string };

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
  ...(['A', 'B', 'C'] as const).map((grade): SettingField => ({ key: `grace-${grade}`, label: `Grace for grade ${grade} (days)`,
    placeholder: 'none', blank: true, valid: DAYS, inputMode: 'numeric',
    invalid: 'A grade\'s grace is a whole number of days, or blank for none' })),
  { key: 'late-pct', label: 'Late percent', placeholder: LATE_PCT_DEFAULT, blank: false, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'A late percent is a number with up to two decimals' },
  { key: 'policy-date', label: 'Policy date', placeholder: 'today', blank: true, valid: DATE, inputMode: 'text',
    invalid: 'A policy date is a date (yyyy-mm-dd), or blank for the sheet\'s today', dated: false },
  { key: 'period-numbering-from', label: 'Period numbering from', placeholder: 'off', blank: true, valid: DATE, inputMode: 'text',
    invalid: 'Period numbering from is a date (yyyy-mm-dd), or blank for off', dated: false },
  { key: 'entry-date', label: 'Entry date', placeholder: 'off', blank: true, valid: DATE, inputMode: 'text',
    invalid: 'An entry date is a date (yyyy-mm-dd), or blank for off', dated: false },
  { key: 'grace-days', label: 'Grace days', placeholder: 'off', blank: true, valid: DAYS, inputMode: 'numeric',
    invalid: 'Grace days are a whole number of days, or blank for off', dated: false },
  { key: 'fiscal-start-month', label: 'Fiscal year start month', placeholder: '1', blank: false, valid: /^(0?[1-9]|1[0-2])$/,
    inputMode: 'numeric', invalid: 'A fiscal start month is a month from 1 to 12', dated: false, starting: '1' },
  { key: 'cap', label: 'Cap', placeholder: 'none', blank: true, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'A cap is an amount with up to two decimals, or blank for none' },
  { key: 'proof-threshold', label: 'Proof threshold', placeholder: 'none', blank: true, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'A proof threshold is an amount with up to two decimals, or blank for none' },
  ...(['A', 'B', 'C'] as const).map((grade): SettingField => ({ key: `proof-threshold-${grade}`,
    label: `Proof threshold for grade ${grade}`, placeholder: 'sheet\'s threshold', blank: true, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'A grade\'s proof threshold is an amount with up to two decimals, or blank for the sheet\'s' })),
  { key: 'note-threshold', label: 'Note threshold', placeholder: 'none', blank: true, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'A note threshold is an amount with up to two decimals, or blank for none' },
  { key: 'pot', label: 'Pot', placeholder: 'none', blank: true, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'A pot is an amount with up to two decimals, or blank for none', dated: false },
  { key: 'alert-threshold', label: 'Alert threshold', placeholder: 'off', blank: true, valid: AMOUNT, inputMode: 'decimal',
    invalid: 'An alert threshold is an amount with up to two decimals, or blank for off', dated: false },
  { key: 'parent', label: 'Parent sheet', placeholder: 'none', blank: true, valid: SHEET_ID, inputMode: 'text',
    invalid: 'A parent is a sheet id, or blank for none', dated: false },
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
      // The pot changes each pool member's cut.
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      // The parent's rates stand in for the sheet's missing ones; the parent shows with the sheet's today.
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
      await queryClient.invalidateQueries({ queryKey: sheetTodayKey(sheetId) });
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
              In force: <strong data-testid={`setting-${s.key}-current`}>{current(s.key) || (s.starting ?? '')}</strong>
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
  SheetBody.fill({ order: 3.5, Component: DelegatePanel }),
  SheetBody.fill({ order: 4, Component: SettingsPanel }),
];
