import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { delegationsPath, delegationsQueryKey, useDelegations, type Delegation } from './roles';

// The sheet's delegations: each time a steward delegated the steward role to a user name for a from-date to a
// to-date, inclusive, by the sheet's today (blank: today), and the form that delegates. Anyone but a steward (by
// their own role), or a to-date before the from-date, is refused by the server and shows the form's error.
function Delegations() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const delegations = useDelegations();
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { user: string; from: string; to: string }) =>
      postJson<Delegation>(delegationsPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: delegationsQueryKey(sheet ?? '') });
      setName('');
      setFrom('');
      setTo('');
    },
  });
  const submit = useSubmitOnce(save);

  if (!user || !sheet) return null;
  return (
    <section data-testid="delegations">
      <h2>Delegations</h2>
      <ul>
        {(delegations.data ?? []).map((d, i) => (
          <li key={i} data-testid="delegate-line">
            {d.by} delegated steward to {d.user} from {d.from} to {d.to}
          </li>
        ))}
      </ul>
      <form
        data-testid="delegate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ user: name, from, to });
        }}
      >
        <input
          data-testid="delegate-user"
          placeholder="User name"
          value={name}
          onChange={(e) => { setName(e.target.value); save.reset(); }}
        />
        <input
          data-testid="delegate-from"
          placeholder="From (YYYY-MM-DD)"
          value={from}
          onChange={(e) => { setFrom(e.target.value); save.reset(); }}
        />
        <input
          data-testid="delegate-to"
          placeholder="To (YYYY-MM-DD)"
          value={to}
          onChange={(e) => { setTo(e.target.value); save.reset(); }}
        />
        <button type="submit" data-testid="delegate-submit" disabled={save.isPending}>
          Delegate
        </button>
        {save.isError && (
          <span data-testid="delegate-error">Only a steward can delegate the steward role; the user name must not be blank, each date must be YYYY-MM-DD or blank, and the to-date must not be before the from-date.</span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 7.5, Component: Delegations });
