import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Delegation, delegationsKey, useDelegations } from './roles';

// The steward role delegated on the sheet, and the form a steward delegates it with. While the sheet's
// today is within a delegation's dates, its user acts as a steward.
function Delegations() {
  const [by] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const delegations = useDelegations(sheet);
  const [user, setUser] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: (d: { user: string; from: string; to: string }) =>
      postJson<Delegation>(`/api/sheets/${encodeURIComponent(sheet)}/delegations`, { ...d, by }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: delegationsKey(sheet) });
      setUser('');
      setFrom('');
      setTo('');
    },
  });
  const submit = useSubmitOnce(save);

  return (
    <section data-testid="delegations">
      <h2>Delegations</h2>
      <ul>
        {(delegations.data ?? []).map((d, i) => (
          <li key={i} data-testid="delegate-line">
            {d.user} acts as steward for {d.by}, {d.from} to {d.to}
          </li>
        ))}
      </ul>
      <form
        data-testid="delegate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ user, from, to });
        }}
      >
        <input
          data-testid="delegate-user"
          placeholder="User name"
          value={user}
          onChange={(e) => {
            setUser(e.target.value);
            save.reset();
          }}
        />
        <input
          data-testid="delegate-from"
          placeholder="From (yyyy-mm-dd, blank for today)"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            save.reset();
          }}
        />
        <input
          data-testid="delegate-to"
          placeholder="To (yyyy-mm-dd)"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            save.reset();
          }}
        />
        <button type="submit" data-testid="delegate-submit" disabled={save.isPending}>
          Delegate
        </button>
        {save.isError && (
          <span data-testid="delegate-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The delegation was refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 51, Component: Delegations });
