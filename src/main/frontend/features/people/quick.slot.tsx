import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Claim, claimsKey } from '../claims/claims';
import { peopleKey } from './people';

// The quick-add bar over the people list: a name and an amount add a DRAFT claim dated the entry date
// (while it is set, else the sheet's today) for that person, adding them first (grade A) when the name is not on the sheet. The server
// applies the person and claim forms' rules and refuses the whole quick add, storing nothing.
function QuickAdd() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationFn: (f: { name: string; amount: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/quick-add`, { ...f, user }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]);
      setName('');
      setAmount('');
    },
  });
  const submit = useSubmitOnce(add);

  return (
    <form
      data-testid="quick-add"
      onSubmit={(e) => {
        e.preventDefault();
        submit({ name, amount });
      }}
    >
      <h2>Quick add</h2>
      <input
        data-testid="quick-name"
        placeholder="Name"
        value={name}
        onChange={(e) => {
          setName(e.target.value);
          add.reset();
        }}
      />
      <input
        data-testid="quick-amount"
        placeholder="Amount"
        value={amount}
        onChange={(e) => {
          setAmount(e.target.value);
          add.reset();
        }}
      />
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Add claim
      </button>
      {add.isError && (
        <span data-testid="quick-error" role="alert">
          {' '}
          {add.error instanceof HttpError && add.error.reason ? `${add.error.reason}.` : 'The quick add was refused.'}
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 15, Component: QuickAdd });
