import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';

/** A user name's role on the sheet: clerk, steward or viewer. A name with no role assigned is a clerk. */
export type Role = { user: string; role: string };

export const rolesKey = (sheetId: string) => ['sheet', sheetId, 'roles'] as const;

const ROLES = ['clerk', 'steward', 'viewer'];

/** The sheet's assigned roles and the form that assigns one (replacing any the name had). */
function Roles({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const roles = useQuery({
    queryKey: rolesKey(sheetId),
    queryFn: () => getJson<Role[]>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`),
  }).data ?? [];
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (body: { name: string; role: string }) =>
      putJson<Role>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`, { user, ...body }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: rolesKey(sheetId) });
      setName('');
      setRole('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : 'Could not set this role: give a user name and clerk, steward or viewer.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('Give the user name the role is for.');
      return;
    }
    if (!ROLES.includes(role.trim())) {
      setError('The role must be clerk, steward or viewer.');
      return;
    }
    if (!once.start()) return;
    save.mutate({ name: name.trim(), role: role.trim() });
  };

  const change = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
  };

  return (
    <section data-testid="roles">
      <h2>Roles</h2>
      <form data-testid="role-form" onSubmit={submit}>
        <input data-testid="role-form-user" placeholder="User name" value={name} onChange={change(setName)} />{' '}
        <input data-testid="role-form-role" placeholder="clerk, steward or viewer" list="role-options" value={role}
          onChange={change(setRole)} />
        <datalist id="role-options">
          {ROLES.map((r) => (
            <option key={r} value={r} />
          ))}
        </datalist>{' '}
        <button type="submit" data-testid="role-form-submit" disabled={save.isPending}>
          Set role
        </button>
        {error && (
          <p data-testid="role-form-error" role="alert">
            {error}
          </p>
        )}
      </form>
      <p>Anyone not listed is a clerk. A viewer sees everything and can change nothing.</p>
      <ul>
        {roles.map((r) => (
          <li key={r.user} data-testid={`role-row-${r.user}`}>
            <span data-testid="role-user">{r.user}</span> <span data-testid="role-value">{r.role}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 8, Component: Roles });
