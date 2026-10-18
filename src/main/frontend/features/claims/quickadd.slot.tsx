import { useState } from 'react';
import type { FormEvent } from 'react';
import { HttpError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { PeopleBar } from '../../slots/defs/peopleBar';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { peopleKey } from '../people/people';
import { periodsKey, useClosedPeriodOf } from '../periods/periods';
import { AMOUNT, claimsKey, quickAddClaim } from './claims';

type QuickInput = { name: string; amount: string };
const EMPTY: QuickInput = { name: '', amount: '' };

// The quick-add bar: a name and an amount make a DRAFT claim dated the sheet's today, with an empty
// note, for the named person (matched ignoring case; created with grade A when not on the sheet).
// The person and claim forms' rules apply: a name is required, the amount must be 0.00 or more with
// up to two decimals, and no claim may be dated inside a closed period.
function QuickAdd() {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const today = useSheetToday();
  const closedOf = useClosedPeriodOf();
  const [form, setForm] = useState<QuickInput>(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (input: QuickInput) => quickAddClaim(sheet!, user, input),
    onSuccess: async () => {
      setForm(EMPTY);
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) });
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet!) });
    },
    onError: async (e) => {
      const reason = e instanceof HttpError ? (e.body as { reason?: string } | undefined)?.reason : undefined;
      setError(reason === 'retired'
        ? 'The claim could not be added: the person retired before today.'
        : e instanceof HttpError && e.status === 409
        ? 'The claim could not be added: today is inside a closed period.'
        : 'The claim could not be added: a name is required and the amount must be 0.00 or more, with up to two decimals.');
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet!) });
    },
  });

  const change = (patch: Partial<QuickInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const input = { name: form.name.trim(), amount: form.amount.trim() };
    if (!input.name) {
      setError('A person needs a name.');
      return;
    }
    if (!AMOUNT.test(input.amount)) {
      setError('The amount must be a number of 0.00 or more, with up to two decimals.');
      return;
    }
    const closed = today ? closedOf(today) : undefined;
    if (closed) {
      setError(`The claim is dated inside the closed period ${closed.name}.`);
      return;
    }
    once(() => save.mutateAsync(input));
  };

  return (
    <form data-testid="quick-add" onSubmit={submit}>
      <input
        data-testid="quick-name"
        placeholder="Name"
        value={form.name}
        onChange={(e) => change({ name: e.target.value })}
      />
      <input
        data-testid="quick-amount"
        placeholder="Amount"
        inputMode="decimal"
        value={form.amount}
        onChange={(e) => change({ amount: e.target.value })}
      />
      <button type="submit" data-testid="quick-submit" disabled={save.isPending}>
        Quick add
      </button>
      {error && <span data-testid="quick-error">{error}</span>}
    </form>
  );
}

export const contribution = PeopleBar.fill({ order: 10, Component: QuickAdd });
