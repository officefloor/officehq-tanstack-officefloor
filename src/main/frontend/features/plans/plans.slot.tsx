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

/** One plan: its dates, how many claims it has made, its next dates, whether it is paused, and the buttons that pause
 *  and resume it; a refused pause or resume shows in the plan's own error, cleared by the next action on the row. */
function PlanRow({ sheetId, user, person, plan: p }: { sheetId: string; user: string; person: Person; plan: Plan }) {
  const queryClient = useQueryClient();
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const toggle = useMutation({
    mutationFn: (action: 'pause' | 'resume') =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheetId)}/plans/${p.id}/${action}`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
    },
    onError: (e, action) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not ${action} the plan.`),
    onSettled: once.done,
  });

  const act = (action: 'pause' | 'resume') => {
    begin();
    if (!once.start()) return;
    toggle.mutate(action);
  };

  return (
    <li data-testid={`plan-row-${p.id}`}>
      {p.amount} from {p.start} {p.day !== null ? `monthly on day ${p.day}` : `every ${p.every} days`} to {p.end}; made{' '}
      <span data-testid="plan-generated">{p.generated}</span>; next{' '}
      <span data-testid="plan-next-dates">{p.nextDates.join(', ')}</span>; paused{' '}
      <span data-testid="plan-paused">{p.paused ? 'yes' : 'no'}</span>{' '}
      <button type="button" data-testid="plan-pause" disabled={toggle.isPending} onClick={() => act('pause')}>
        Pause
      </button>{' '}
      <button type="button" data-testid="plan-resume" disabled={toggle.isPending} onClick={() => act('resume')}>
        Resume
      </button>
      {error && (
        <p data-testid="plan-error" role="alert">
          {error}
        </p>
      )}
    </li>
  );
}

/** The person's plans, each with how many claims it has made so far and its next five dates after the sheet's today, and the form that adds one: an amount, a start
 *  date, every N days (a whole number, at least 1) or
 *  monthly on a day of the month (1 to 31), and an end date, not before the start. */
function PersonPlans({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const plans = (usePlans(sheetId).data ?? []).filter((p) => p.personId === person.id);
  const [amount, setAmount] = useState('');
  const [start, setStart] = useState('');
  const [every, setEvery] = useState('');
  const [day, setDay] = useState('');
  const [end, setEnd] = useState('');
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (body: { user: string; amount: string; start: string; every: string; day: string; end: string }) =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/plans`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
      setAmount('');
      setStart('');
      setEvery('');
      setDay('');
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
    add.mutate({ user, amount: amount.trim(), start: start.trim(), every: every.trim(), day: day.trim(), end: end.trim() });
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
          <PlanRow key={p.id} sheetId={sheetId} user={user} person={person} plan={p} />
        ))}
      </ul>
      <form onSubmit={submit} style={{ display: 'inline' }}>
        <input data-testid="plan-form-amount" placeholder="amount" value={amount} onChange={change(setAmount)} />{' '}
        <input data-testid="plan-form-start" placeholder="start yyyy-mm-dd" value={start} onChange={change(setStart)} />{' '}
        <input data-testid="plan-form-every" placeholder="every N days" value={every} onChange={change(setEvery)} />{' '}
        <input data-testid="plan-form-day" placeholder="or day of month" value={day} onChange={change(setDay)} />{' '}
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
