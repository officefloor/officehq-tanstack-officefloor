import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { rolesKey, useRoles, type Role } from './roles';

/** The sheet's roles: the form that gives a user name a role (clerk, steward, viewer or restricted — a viewer sees everything and changes nothing; a restricted user sees on the people list only their crews' people and themselves), and every role given. */
function Roles({ sheetId, user }: { sheetId: string; user: string }) {
  const roles = useRoles(sheetId);
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [role, setRole] = useState('');

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: (v: { name: string; role: string }) =>
      putJson<Role>(`/api/sheets/${encodeURIComponent(sheetId)}/roles/${encodeURIComponent(v.name.trim())}`, {
        role: v.role,
        by: user,
      }),
    onSuccess: () => {
      setName('');
      setRole('');
      void queryClient.invalidateQueries({ queryKey: rolesKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <section data-testid="roles">
      <h3>Roles</h3>
      <form
        data-testid="role-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          save.reset();
          save.mutate({ name, role });
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
            list="role-choices"
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              save.reset();
            }}
          />
          <datalist id="role-choices">
            <option value="clerk" />
            <option value="steward" />
            <option value="viewer" />
            <option value="restricted" />
          </datalist>
        </label>{' '}
        <button type="submit" data-testid="role-form-submit" disabled={save.isPending}>
          Set role
        </button>
        {save.isError && (
          <span data-testid="role-form-error">The user name must not be blank and the role must be clerk, steward, viewer or restricted.</span>
        )}
      </form>
      {roles.isError && <p data-testid="roles-load-error">Could not load roles.</p>}
      <table data-testid="roles-table">
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

export const contribution = SheetPage.fill({ order: 7, Component: Roles });
