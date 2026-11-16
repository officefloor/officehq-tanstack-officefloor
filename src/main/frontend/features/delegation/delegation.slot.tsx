import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { delegationsKey, useDelegations, type Delegation } from './delegation';

/** The sheet's delegations: the form with which a steward delegates the steward role to a user for a from- and
 *  to-date, both inclusive (a blank from-date is the sheet's today, a blank to-date the from-date), and every
 *  delegation made. */
function Delegations({ sheetId, user }: { sheetId: string; user: string }) {
  const delegations = useDelegations(sheetId);
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: (v: { name: string; from: string; to: string }) =>
      postJson<Delegation>(`/api/sheets/${encodeURIComponent(sheetId)}/delegations`, {
        user: v.name,
        from: v.from,
        to: v.to,
        by: user,
      }),
    onSuccess: () => {
      setName('');
      setFrom('');
      setTo('');
      void queryClient.invalidateQueries({ queryKey: delegationsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    save.reset();
  };

  return (
    <section data-testid="delegations">
      <h3>Delegations</h3>
      <form
        data-testid="delegate-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          save.reset();
          save.mutate({ name, from, to });
        }}
      >
        <label>
          User <input data-testid="delegate-user" value={name} onChange={field(setName)} />
        </label>{' '}
        <label>
          From <input data-testid="delegate-from" placeholder="yyyy-MM-dd" value={from} onChange={field(setFrom)} />
        </label>{' '}
        <label>
          To <input data-testid="delegate-to" placeholder="yyyy-MM-dd" value={to} onChange={field(setTo)} />
        </label>{' '}
        <button type="submit" data-testid="delegate-submit" disabled={save.isPending}>
          Delegate
        </button>
        {save.isError && (
          <span data-testid="delegate-error">
            Only a steward may delegate, to a user named, with dates yyyy-MM-dd and the to-date not before the from-date.
          </span>
        )}
      </form>
      {delegations.isError && <p data-testid="delegations-load-error">Could not load delegations.</p>}
      <ul data-testid="delegations-list">
        {(delegations.data ?? []).map((d) => (
          <li key={d.id} data-testid="delegate-line">
            {d.user} acts as steward for {d.steward} from {d.from} to {d.to}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 8, Component: Delegations });
