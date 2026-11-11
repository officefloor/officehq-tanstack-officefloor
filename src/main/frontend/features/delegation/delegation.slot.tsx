import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { DATE, delegationsKey, useDelegations } from './delegation';
import type { Delegation, DelegationChange } from './delegation';

// A steward delegates the steward role to a user for a from-date and a to-date, inclusive (blank from:
// the sheet's today; blank to: the from-date). While the sheet's today is inside the range the user acts
// as a steward in every rule. Anyone but a steward (by their own role) is refused, as is a to-date
// before the from-date. Every delegation made is listed below the form.
function Delegations({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const delegations = useDelegations(sheetId);
  const [fields, setFields] = useState<DelegationChange>({ user: '', from: '', to: '' });
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const save = useMutation({
    mutationFn: (body: DelegationChange) =>
      postJson<Delegation>(`/api/sheets/${encodeURIComponent(sheetId)}/delegations`, body, user),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: delegationsKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: ['sheet', sheetId, 'my-role'] });
    },
    onError: () => setError('That delegation could not be made.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const name = fields.user.trim();
    const from = fields.from.trim();
    const to = fields.to.trim();
    if (name === '') {
      setError('A user name is required.');
      return;
    }
    for (const date of [from, to]) {
      if (date !== '' && (!DATE.test(date) || Number.isNaN(Date.parse(date)))) {
        setError('A date is yyyy-MM-dd, or blank.');
        return;
      }
    }
    if (from !== '' && to !== '' && to < from) {
      setError('A to-date is on or after the from-date.');
      return;
    }
    once((done) => save.mutate({ user: name, from, to }, { onSettled: done }));
  };

  const change = (next: Partial<DelegationChange>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
  };

  return (
    <section data-testid="delegations">
      <h2>Delegations</h2>
      <form onSubmit={submit} data-testid="delegate-form">
        <label>
          User{' '}
          <input data-testid="delegate-user" value={fields.user} onChange={(e) => change({ user: e.target.value })} />
        </label>{' '}
        <label>
          From{' '}
          <input
            data-testid="delegate-from"
            placeholder="today"
            value={fields.from}
            onChange={(e) => change({ from: e.target.value })}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            data-testid="delegate-to"
            placeholder="from-date"
            value={fields.to}
            onChange={(e) => change({ to: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="delegate-submit" disabled={save.isPending}>
          Delegate steward
        </button>
        {error && (
          <p role="alert" data-testid="delegate-error">
            {error}
          </p>
        )}
      </form>
      <ul data-testid="delegate-list">
        {(delegations.data ?? []).map((d) => (
          <li key={d.id} data-testid="delegate-line">
            <span data-testid="delegate-line-user">{d.user}</span> acts as steward from{' '}
            <span data-testid="delegate-line-from">{d.from}</span> to <span data-testid="delegate-line-to">{d.to}</span>{' '}
            for <span data-testid="delegate-line-by">{d.by}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -7, Component: Delegations });
