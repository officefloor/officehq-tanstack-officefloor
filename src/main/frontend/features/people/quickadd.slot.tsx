import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { claimsQueryKey, type Claim } from '../claims/claims';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peopleQueryKey } from './people';

// The quick-add bar above the people list: a name and an amount make a DRAFT claim for that person, dated the
// sheet's entry date (else its today) with an empty note; a name not on the sheet adds the person first, with the sheet's default grade (blank: A). The server
// checks everything before it stores anything, so a refusal adds nobody.
function QuickAddBar() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { name: string; amount: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet ?? '')}/quick-add`, body, user),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') }),
        queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet ?? '') }),
      ]);
      setName('');
      setAmount('');
    },
  });
  const submit = useSubmitOnce(add);
  if (!user || !sheet) return null;

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
        onChange={(e) => { setName(e.target.value); add.reset(); }}
      />
      <input
        data-testid="quick-amount"
        placeholder="Amount"
        value={amount}
        onChange={(e) => { setAmount(e.target.value); add.reset(); }}
      />
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Add draft claim
      </button>
      {add.isError && (
        <span data-testid="quick-error">
          A name is required, the amount must be a number, 0.00 or more, with up to two decimals, and no claim can
          be added whose due-by falls in a closed period.
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 15, Component: QuickAddBar });
