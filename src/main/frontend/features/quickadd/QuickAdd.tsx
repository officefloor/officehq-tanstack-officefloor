import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { AMOUNT, claimsKey } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { peopleKey } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';

type QuickInput = { name: string; amount: string };

const EMPTY: QuickInput = { name: '', amount: '' };

// The quick-add bar on the people list: a DRAFT claim of the amount, dated the sheet's today, for the
// named person — added first with grade A when the name is not on the sheet. The server does it as
// one change, so a refused claim adds no person.
export function QuickAdd() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [form, setForm] = useState<QuickInput>(EMPTY);
  const [error, setError] = useState('');
  const once = useOneAtATime();
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationFn: (input: QuickInput) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/quick-claims?user=${encodeURIComponent(user)}`, input),
    onSuccess: async () => {
      setForm(EMPTY);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]);
    },
    onError: () => setError('That claim could not be added.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.name.trim()) {
      setError('Type a name.');
      return;
    }
    if (!AMOUNT.test(form.amount.trim())) {
      setError('Type an amount of 0.00 or more, with at most two decimals.');
      return;
    }
    once(() => add.mutateAsync(form));
  };

  const change = (patch: Partial<QuickInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  return (
    <form onSubmit={submit} data-testid="quick-form">
      <input
        data-testid="quick-name"
        placeholder="Name"
        value={form.name}
        onChange={(e) => change({ name: e.target.value })}
      />
      <input
        data-testid="quick-amount"
        placeholder="Amount"
        value={form.amount}
        onChange={(e) => change({ amount: e.target.value })}
      />
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Quick add
      </button>
      {error && <span data-testid="quick-error">{error}</span>}
    </form>
  );
}
