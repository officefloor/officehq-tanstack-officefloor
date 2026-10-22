import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { usePersonError } from '../people/rowError';
import type { Person } from '../people/people';
import { plansKey, usePlans } from './plans';
import type { Plan } from './plans';

/** The person's plans, each with how many claims it has made so far, and the form that adds one: an amount, a start
 *  date, every N days (a whole number, at least 1) and an end date, not before the start. */
function PersonPlans({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const plans = (usePlans(sheetId).data ?? []).filter((p) => p.personId === person.id);
  const [amount, setAmount] = useState('');
  const [start, setStart] = useState('');
  const [every, setEvery] = useState('');
  const [end, setEnd] = useState('');
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (body: { user: string; amount: string; start: string; every: string; end: string }) =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/plans`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
      setAmount('');
      setStart('');
      setEvery('');
      setEnd('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not add a plan for ${person.name}.`),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    begin();
    if (!once.start()) return;
    add.mutate({ user, amount: amount.trim(), start: start.trim(), every: every.trim(), end: end.trim() });
  };

  const change = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
  };

  return (
    <td>
      Plans
      <ul>
        {plans.map((p) => (
          <li key={p.id} data-testid={`plan-row-${p.id}`}>
            {p.amount} from {p.start} every {p.every} days to {p.end}; made{' '}
            <span data-testid="plan-generated">{p.generated}</span>
          </li>
        ))}
      </ul>
      <form onSubmit={submit} style={{ display: 'inline' }}>
        <input data-testid="plan-form-amount" placeholder="amount" value={amount} onChange={change(setAmount)} />{' '}
        <input data-testid="plan-form-start" placeholder="start yyyy-mm-dd" value={start} onChange={change(setStart)} />{' '}
        <input data-testid="plan-form-every" placeholder="every N days" value={every} onChange={change(setEvery)} />{' '}
        <input data-testid="plan-form-end" placeholder="end yyyy-mm-dd" value={end} onChange={change(setEnd)} />{' '}
        <button type="submit" data-testid="plan-form-submit" disabled={add.isPending}>
          Add plan
        </button>
      </form>
      {error && (
        <p data-testid="plan-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 47, Component: PersonPlans });
