import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { peopleKey } from '../people/people';
import { AMOUNT, claimsKey } from '../claims/claims';
import type { Claim } from '../claims/claims';

type Fields = { name: string; amount: string };
const EMPTY: Fields = { name: '', amount: '' };

// The quick-add bar under the people list: a name and an amount make a DRAFT claim dated the
// sheet's today, with no note, for that person, adding them (grade A) when the name is not on the
// sheet. The server makes it one change: a refusal adds no one.
function QuickAdd({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: Fields) => postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/quick`, body, user),
    onSuccess: () => {
      setFields(EMPTY);
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: () => setError('That claim could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (fields.name.trim() === '') {
      setError('A name is required.');
      return;
    }
    if (!AMOUNT.test(fields.amount.trim())) {
      setError('An amount is a number of 0.00 or more, with up to two decimals.');
      return;
    }
    once((done) => add.mutate(fields, { onSettled: done }));
  };

  const change = (next: Partial<Fields>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
  };

  return (
    <section data-testid="quick-add">
      <form onSubmit={submit}>
        <label>
          Name{' '}
          <input data-testid="quick-name" value={fields.name} onChange={(e) => change({ name: e.target.value })} />
        </label>{' '}
        <label>
          Amount{' '}
          <input
            data-testid="quick-amount"
            value={fields.amount}
            onChange={(e) => change({ amount: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
          Quick add
        </button>
        {error && (
          <p role="alert" data-testid="quick-error">
            {error}
          </p>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 1, Component: QuickAdd });
