import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { listRoles, rolesKey, setRole } from './api';

const EMPTY = { user: '', role: '' };

/** The roles assigned on the sheet (any other name is a clerk) and a form that assigns one. */
export function Roles({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const roles = useQuery({ queryKey: rolesKey(sheetId), queryFn: () => listRoles(sheetId) });
  const [form, setForm] = useState(EMPTY);

  const save = useMutation({
    mutationFn: (input: typeof EMPTY) => setRole(sheetId, user, input.user, input.role),
    onSuccess: () => {
      setForm(EMPTY);
      return queryClient.invalidateQueries({ queryKey: rolesKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();

  const change = (patch: Partial<typeof EMPTY>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  return (
    <section>
      <h3>Roles</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = form;
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(input);
          });
        }}
      >
        <input data-testid="role-form-user" placeholder="User" value={form.user}
          onChange={(e) => change({ user: e.target.value })} />{' '}
        <input data-testid="role-form-role" placeholder="clerk or steward" list="role-names" value={form.role}
          onChange={(e) => change({ role: e.target.value })} />
        <datalist id="role-names">
          <option value="clerk" />
          <option value="steward" />
        </datalist>{' '}
        <button type="submit" data-testid="role-form-submit" disabled={save.isPending}>
          Set role
        </button>
        {save.isError && <span data-testid="role-form-error">A role needs a user name and is clerk or steward.</span>}
      </form>
      <table>
        <tbody>
          {(roles.data ?? []).map((r) => (
            <tr key={r.user} data-testid={`role-row-${r.user}`}>
              <td>{r.user}</td>
              <td data-testid="role-value">{r.role}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
