import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Role, ROLES, rolesKey, useRoles } from './roles';

// The roles assigned on the sheet (any other name is a clerk), and the form that assigns one.
function Roles() {
  const [by] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const roles = useRoles(sheet);
  const [user, setUser] = useState('');
  const [role, setRole] = useState('');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: (r: { user: string; role: string }) =>
      postJson<Role>(`/api/sheets/${encodeURIComponent(sheet)}/roles`, { ...r, by }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: rolesKey(sheet) });
      setUser('');
      setRole('');
    },
  });
  const submit = useSubmitOnce(save);

  return (
    <section data-testid="roles">
      <h2>Roles</h2>
      <table>
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
      <form
        data-testid="role-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ user, role });
        }}
      >
        <input
          data-testid="role-form-user"
          placeholder="User name"
          value={user}
          onChange={(e) => {
            setUser(e.target.value);
            save.reset();
          }}
        />
        <input
          data-testid="role-form-role"
          placeholder={ROLES.join(' or ')}
          list="role-form-roles"
          value={role}
          onChange={(e) => {
            setRole(e.target.value);
            save.reset();
          }}
        />
        <datalist id="role-form-roles">
          {ROLES.map((r) => (
            <option key={r} value={r} />
          ))}
        </datalist>
        <button type="submit" data-testid="role-form-submit" disabled={save.isPending}>
          Set role
        </button>
        {save.isError && (
          <span data-testid="role-form-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The role was refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 50, Component: Roles });
