import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimsKey, type Claim } from '../claims/useClaims';
import { peopleKey } from './usePeople';

// The quick-add bar on the people list: a name and an amount make a DRAFT claim dated the sheet's
// today, with an empty note, for the person of that name or alias (case and outer spaces ignored) — adding
// the person with grade A first when no one on the sheet has it. The person form's and the claim
// form's rules apply; a refusal adds no one and nothing.
export function QuickAdd() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();

  const add = useMutation({
    mutationFn: (body: { name: string; amount: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/quick-claims`, { ...body, user }),
    onSuccess: () => {
      setName('');
      setAmount('');
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });

  const changed = (set: (v: string) => void) => (v: string) => {
    set(v);
    add.reset();
  };

  return (
    <form
      data-testid="quick-add"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          add.reset();
          return add.mutateAsync({ name: name.trim(), amount: amount.trim() });
        });
      }}
    >
      <input data-testid="quick-name" value={name} placeholder="Name" onChange={(e) => changed(setName)(e.target.value)} />{' '}
      <input
        data-testid="quick-amount"
        value={amount}
        placeholder="Amount"
        inputMode="decimal"
        onChange={(e) => changed(setAmount)(e.target.value)}
      />{' '}
      <button type="submit" data-testid="quick-submit">Quick add</button>
      {add.isError && (
        <span data-testid="quick-error" role="alert"> Could not add: a name is required, the amount must be 0.00 or more, with at most two decimals, and no claim whose due-by falls inside a closed period can be added.</span>
      )}
    </form>
  );
}
