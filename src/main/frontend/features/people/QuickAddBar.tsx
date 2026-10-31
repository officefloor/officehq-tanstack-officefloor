import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { claimsKey, type Claim } from '../claims/useClaims';
import { peopleKey } from './usePeople';

/** Adds a DRAFT claim by person name: dated the sheet's today, with an empty note, for the person
 *  of that name — created with grade A when the name is not on the sheet. One change on the
 *  server: a refused claim leaves no new person behind. */
export function QuickAddBar({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (q: { name: string; amount: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/quick-add`, { ...q, user }),
    onSuccess: async () => {
      setName('');
      setAmount('');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]);
    },
  });

  return (
    <form
      data-testid="quick-add"
      onSubmit={(e) => {
        e.preventDefault();
        add.reset();
        submitOnce(() => add.mutateAsync({ name, amount }));
      }}
    >
      <label>
        Quick add: name{' '}
        <input
          data-testid="quick-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            add.reset();
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
            add.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Add claim
      </button>
      {add.isError && (
        <span data-testid="quick-error" role="alert">
          {' '}
          Could not add this claim: a name and an amount of 0.00 or more are needed, dated outside a closed period.
        </span>
      )}
    </form>
  );
}
