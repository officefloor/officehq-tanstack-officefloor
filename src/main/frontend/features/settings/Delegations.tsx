import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { delegate, delegationsKey, listDelegations } from './api';

const EMPTY = { user: '', from: '', to: '' };

/** The sheet's delegations of the steward role (newest first) and a form with which a steward makes one:
 *  a user, from a date to a date, inclusive (a blank date is the sheet's today). */
export function Delegations({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const delegations = useQuery({ queryKey: delegationsKey(sheetId), queryFn: () => listDelegations(sheetId) });
  const [form, setForm] = useState(EMPTY);

  const save = useMutation({
    mutationFn: (input: typeof EMPTY) => delegate(sheetId, user, input.user, input.from, input.to),
    onSuccess: () => {
      setForm(EMPTY);
      return queryClient.invalidateQueries({ queryKey: delegationsKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();

  const change = (patch: Partial<typeof EMPTY>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  return (
    <section>
      <h3>Delegations</h3>
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
        <input data-testid="delegate-user" placeholder="User" value={form.user}
          onChange={(e) => change({ user: e.target.value })} />{' '}
        <input data-testid="delegate-from" placeholder="From (yyyy-mm-dd)" value={form.from}
          onChange={(e) => change({ from: e.target.value })} />{' '}
        <input data-testid="delegate-to" placeholder="To (yyyy-mm-dd)" value={form.to}
          onChange={(e) => change({ to: e.target.value })} />{' '}
        <button type="submit" data-testid="delegate-submit" disabled={save.isPending}>
          Delegate steward
        </button>
        {save.isError && (
          <span data-testid="delegate-error">Only a steward can delegate, to a user, with a to-date on or after the from-date.</span>
        )}
      </form>
      <ul>
        {(delegations.data ?? []).map((d) => (
          <li key={d.id} data-testid="delegate-line">
            {d.user} acts as steward for {d.by} from {d.from} to {d.to}
          </li>
        ))}
      </ul>
    </section>
  );
}
