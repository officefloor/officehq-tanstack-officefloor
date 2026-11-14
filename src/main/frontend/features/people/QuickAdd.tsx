import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey } from '../claims/api';
import { peopleKey, quickAdd, type QuickAddInput } from './api';

const EMPTY: QuickAddInput = { name: '', amount: '' };

/** A bar that adds a DRAFT claim to a person by name, adding the person (grade A) when the name is
 *  not on the sheet. */
export function QuickAdd({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<QuickAddInput>(EMPTY);

  const add = useMutation({
    mutationFn: (input: QuickAddInput) => quickAdd(sheetId, user, input),
    onSuccess: () => {
      setForm(EMPTY);
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });

  const submitOnce = useSubmitOnce();

  const change = (patch: Partial<QuickAddInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    add.reset();
  };

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const input = form;
        submitOnce(() => {
          add.reset();
          return add.mutateAsync(input);
        });
      }}
    >
      <label>
        Quick add: name{' '}
        <input data-testid="quick-name" value={form.name} onChange={(e) => change({ name: e.target.value })} />
      </label>{' '}
      <label>
        Amount{' '}
        <input data-testid="quick-amount" value={form.amount} onChange={(e) => change({ amount: e.target.value })} />
      </label>{' '}
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Add claim
      </button>
      {add.isError && (
        <span data-testid="quick-error">A quick add needs a name and an amount of 0.00 or more with up to two decimals, on a date outside a closed period.</span>
      )}
    </form>
  );
}
