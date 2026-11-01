import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { rolesKey, rolesPath, useRoles } from './roles';
import type { Role } from './roles';

type Draft = { user: string; role: string };
const EMPTY: Draft = { user: '', role: '' };

// The roles given on the sheet, and a form that gives a user name a role (clerk, steward or viewer). Any name not
// listed is a clerk.
function Roles({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: roles = [] } = useRoles(sheet);
  const [draft, setDraft] = useState<Draft>(EMPTY);

  const save = useMutation({
    mutationFn: (body: Draft) => postJson<Role[]>(byUser(rolesPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(rolesKey(sheet), updated);
      setDraft(EMPTY);
    },
  });
  const send = useSubmitOnce(save);

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a blank name or a role that is not clerk, steward or viewer.
    send(draft);
  };

  return (
    <section>
      <h2>Roles</h2>
      <form onSubmit={submit}>
        <label>
          User{' '}
          <input data-testid="role-form-user" value={draft.user} onChange={(e) => change({ user: e.target.value })} />
        </label>{' '}
        <label>
          Role{' '}
          <input
            data-testid="role-form-role"
            placeholder="clerk, steward or viewer"
            value={draft.role}
            onChange={(e) => change({ role: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="role-form-submit" disabled={save.isPending}>
          Set role
        </button>
        {save.isError && (
          <span data-testid="role-form-error" role="alert">
            {' '}A role needs a user name and a role of clerk, steward or viewer.
          </span>
        )}
      </form>
      <ul>
        {roles.map((r) => (
          <li key={r.user} data-testid={`role-row-${r.user}`}>
            <span data-testid="role-user">{r.user}</span>: <span data-testid="role-value">{r.role}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 7, Component: Roles });
