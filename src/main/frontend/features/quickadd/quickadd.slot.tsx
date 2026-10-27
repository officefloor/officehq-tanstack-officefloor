import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsKey } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { peopleKey } from '../people/peopleQuery';
import { useUser } from '../session/useUser';

/**
 * The quick-add bar: a name and an amount make a DRAFT claim dated the sheet's today, for the person
 * of that name or alias (ignoring case), who is added with grade A when not on the sheet. The server applies
 * the person and claim forms' rules and stores nothing when it refuses.
 */
function QuickAdd({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { name: string; amount: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/quick`, body, user),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
      setName('');
      setAmount('');
    },
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The claim could not be quick-added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('Type a name.');
      return;
    }
    once(() => add.mutateAsync({ name: name.trim(), amount: amount.trim() }));
  };

  return (
    <form data-testid="quick-add" onSubmit={submit}>
      <label>
        Name{' '}
        <input
          data-testid="quick-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <label>
        Amount{' '}
        <input
          data-testid="quick-amount"
          value={amount}
          onChange={(e) => {
            setAmount(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Quick add
      </button>
      {error && <p data-testid="quick-error">{error}</p>}
    </form>
  );
}

// Just above the people list; keyed by sheet, so a typed name or an error never carries over.
export const contribution = SheetPanel.fill({
  order: 9,
  Component: ({ sheetId }) => <QuickAdd key={sheetId} sheetId={sheetId} />,
});
