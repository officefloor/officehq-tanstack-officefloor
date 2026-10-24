import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { rolesPath, rolesQueryKey, useRoles, type Role } from './roles';

// The sheet's roles: each user name given one, and the form that gives a name a role — clerk (what any
// name is by default) or steward.
function Roles() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const roles = useRoles();
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { user: string; role: string }) => putJson<Role>(rolesPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: rolesQueryKey(sheet ?? '') });
      setName('');
      setRole('');
    },
  });
  const submit = useSubmitOnce(save);

  if (!user || !sheet) return null;
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
          submit({ user: name, role });
        }}
      >
        <input
          data-testid="role-form-user"
          placeholder="User name"
          value={name}
          onChange={(e) => { setName(e.target.value); save.reset(); }}
        />
        <input
          data-testid="role-form-role"
          placeholder="Role (clerk or steward)"
          value={role}
          onChange={(e) => { setRole(e.target.value); save.reset(); }}
        />
        <button type="submit" data-testid="role-form-submit" disabled={save.isPending}>
          Set role
        </button>
        {save.isError && (
          <span data-testid="role-form-error">The user name must not be blank, and the role must be clerk or steward.</span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 7, Component: Roles });
