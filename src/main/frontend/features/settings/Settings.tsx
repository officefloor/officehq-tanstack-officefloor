import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, putJson } from '../../api/http';
import { useSingleSubmit } from '../../api/useSingleSubmit';
import { useCurrentUser } from '../session/SignIn';

// The sheet's settings and roles. The clerk limit (two decimals; blank: no limit) is the highest due
// a clerk may settle; a steward settles anything. Every user name is a clerk unless assigned steward
// here. The server enforces both when a claim is settled. The floor (two decimals, 1.00 until set) is
// the least live due a claim has; see `claimDue`. The terms (whole days, 14 until set) are how long
// after its date a claim is due by; a grade's
// terms (blank until set) are used instead for its people's claims. A claim's due-by is fixed by the server
// when it is created, from the terms of its person's grade in force on its date; see `Claim.dueBy`. The late percent (two decimals, 0.00 until set) is
// charged on an overdue claim's figure for every full 30 days it is overdue; see `lateCharge`. The policy
// date (yyyy-MM-dd; blank: the sheet's today) decides how a claim's live figures round; see `roundsHalfUp`.
// Period numbering from (yyyy-MM-dd; blank: off): from that day of the sheet's today on, a claim opened is
// numbered within its period (<period name>-<n>, X-<n> in none); the server gives out the numbers.
type Setting = { key: string; value: string };
type Role = { user: string; role: string };

const SETTINGS = [{ key: 'clerk-limit', label: 'Clerk limit' }, { key: 'floor', label: 'Floor' },
  { key: 'terms', label: 'Terms (days)' },
  { key: 'terms-A', label: 'Terms A' }, { key: 'terms-B', label: 'Terms B' }, { key: 'terms-C', label: 'Terms C' },
  { key: 'late-pct', label: 'Late %' },
  { key: 'policy-date', label: 'Policy date' }, { key: 'period-numbering-from', label: 'Period numbering from' }];

const DEFAULT_FLOOR = '1.00';

const DEFAULT_LATE_PCT = '0.00';

const settingsKey = (sheet: string) => ['sheet', sheet, 'settings'];
const settingsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/settings`;
const rolesKey = (sheet: string) => ['sheet', sheet, 'roles'];
const rolesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/roles`;

function useSettings(sheet: string): Setting[] {
  return useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Setting[]>(settingsPath(sheet)),
    enabled: !!sheet,
  }).data ?? [];
}

/** The sheet's floor: the least live due a claim has, with two decimals. */
export function useFloor(sheet: string): string {
  return useSettings(sheet).find((s) => s.key === 'floor')?.value || DEFAULT_FLOOR;
}

/** The sheet's late percent, with two decimals. */
export function useLatePct(sheet: string): string {
  return useSettings(sheet).find((s) => s.key === 'late-pct')?.value || DEFAULT_LATE_PCT;
}

/** The sheet's policy date as set (yyyy-MM-dd); undefined while it is blank. */
export function usePolicyDate(sheet: string): string | undefined {
  return useSettings(sheet).find((s) => s.key === 'policy-date')?.value || undefined;
}

export function Settings({ sheet }: { sheet: string }) {
  return (
    <section>
      <h2>Settings</h2>
      <SettingsForm sheet={sheet} />
      <Roles sheet={sheet} />
    </section>
  );
}

function SettingsForm({ sheet }: { sheet: string }) {
  const queryClient = useQueryClient();
  const current = useSettings(sheet);
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const submitOnce = useSingleSubmit();
  // Only the settings changed in the form are saved; each is written (and audited) on its own.
  const save = useMutation({
    mutationFn: async (changes: Record<string, string>) => {
      for (const [key, value] of Object.entries(changes)) {
        await putJson<Setting>(`${settingsPath(sheet)}/${encodeURIComponent(key)}${by}`, { value });
      }
    },
    onSuccess: () => setDraft({}),
    onSettled: () => queryClient.invalidateQueries({ queryKey: settingsKey(sheet) }),
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          save.reset();
          // A value that is not blank or an amount with up to two decimals is refused by the server (400);
          // the floor may not be blank.
          return save.mutateAsync(Object.fromEntries(Object.entries(draft).map(([k, v]) => [k, v.trim()])));
        });
      }}
    >
      {SETTINGS.map(({ key, label }) => (
        <span key={key}>
          <label>
            {label}{' '}
            <input
              data-testid={`setting-${key}`}
              inputMode="decimal"
              value={draft[key] ?? ''}
              onChange={(e) => {
                save.reset();
                setDraft((d) => ({ ...d, [key]: e.target.value }));
              }}
              // Deleting in the (empty) field also marks the setting as changed, so it can be cleared.
              onKeyDown={(e) => {
                if (e.key !== 'Delete' && e.key !== 'Backspace') return;
                const value = e.currentTarget.value;
                save.reset();
                setDraft((d) => (key in d ? d : { ...d, [key]: value }));
              }}
            />
          </label>{' '}
          (now: <strong data-testid={`setting-${key}-current`}>{current.find((s) => s.key === key)?.value ?? ''}</strong>){' '}
        </span>
      ))}
      <button type="submit" data-testid="settings-save" disabled={save.isPending}>
        Save settings
      </button>
      {save.isError && (
        <span role="alert" data-testid="settings-error">
          {' '}
          {(save.error instanceof HttpError && save.error.status === 400 && save.error.reason)
            || 'The settings could not be saved.'}
        </span>
      )}
    </form>
  );
}

function Roles({ sheet }: { sheet: string }) {
  const queryClient = useQueryClient();
  const roles = useQuery({
    queryKey: rolesKey(sheet),
    queryFn: () => getJson<Role[]>(rolesPath(sheet)),
    enabled: !!sheet,
  }).data ?? [];
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const submitOnce = useSingleSubmit();
  const save = useMutation({
    mutationFn: (body: Role) =>
      putJson<Role>(`${rolesPath(sheet)}/${encodeURIComponent(body.user)}${by}`, { role: body.role }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: rolesKey(sheet) });
      setName('');
      setRole('');
    },
  });

  return (
    <>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            // A blank name or a role other than clerk or steward is refused by the server (400).
            return save.mutateAsync({ user: name.trim(), role: role.trim() });
          });
        }}
      >
        <label>
          User{' '}
          <input
            data-testid="role-form-user"
            value={name}
            onChange={(e) => {
              save.reset();
              setName(e.target.value);
            }}
          />
        </label>{' '}
        <label>
          Role{' '}
          <input
            data-testid="role-form-role"
            list="role-options"
            value={role}
            onChange={(e) => {
              save.reset();
              setRole(e.target.value);
            }}
          />
          <datalist id="role-options">
            <option value="clerk" />
            <option value="steward" />
          </datalist>
        </label>{' '}
        <button type="submit" data-testid="role-form-submit" disabled={save.isPending}>
          Set role
        </button>
        {save.isError && (
          <span role="alert" data-testid="role-form-error">
            {' '}
            {(save.error instanceof HttpError && save.error.status === 400 && save.error.reason)
              || 'The role could not be saved.'}
          </span>
        )}
      </form>
      <table>
        <thead>
          <tr>
            <th>User</th>
            <th>Role</th>
          </tr>
        </thead>
        <tbody>
          {roles.map((r) => (
            <tr key={r.user} data-testid={`role-row-${r.user}`}>
              <td>{r.user}</td>
              <td data-testid="role-value">{r.role}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}
