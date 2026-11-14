import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { plansKey, usePlans } from './plans';
import type { Plan } from './plans';

type PlanFields = { amount: string; start: string; every: string; day: string; end: string };
const EMPTY: PlanFields = { amount: '', start: '', every: '', day: '', end: '' };

// A plan's paused yes/no and its pause and resume buttons: pausing skips its dates from the sheet's today until it is
// resumed. The server refuses pausing a paused plan or resuming a running one; the error shows in the plan's row.
function PlanPause({ person, plan }: { person: Person; plan: Plan }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [error, setError] = usePersonError(person.id, `plan-${plan.id}`);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const toggle = useMutation({
    mutationFn: (action: 'pause' | 'resume') =>
      postJson<Plan>(
        `/api/sheets/${encodeURIComponent(sheet)}/plans/${plan.id}/${action}?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: plansKey(sheet) });
    },
    onError: (_e, action) =>
      setError(action === 'pause' ? 'The plan is already paused.' : 'The plan is not paused.'),
  });

  const press = (action: 'pause' | 'resume') => () => {
    setError('');
    once(() => toggle.mutateAsync(action));
  };

  return (
    <>
      , paused <span data-testid="plan-paused">{plan.paused ? 'yes' : 'no'}</span>{' '}
      <button type="button" data-testid="plan-pause" disabled={toggle.isPending} onClick={press('pause')}>
        Pause
      </button>
      <button type="button" data-testid="plan-resume" disabled={toggle.isPending} onClick={press('resume')}>
        Resume
      </button>
      {error && <span data-testid="plan-error">{error}</span>}
    </>
  );
}

// A person's plans (each its amount, start, interval or day of the month, end, how many claims it has made so far and
// its next five dates after the sheet's today, and whether it is paused, with buttons to pause and resume it) and the form that adds one: an amount, a start (blank: the sheet's today), every N days or a day of the month (1 to
// 31), and an end. The server refuses a bad amount, interval or day, both an interval and a day, or an end before the
// start.
export function PersonPlans({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const mine = (usePlans().data ?? []).filter((p) => p.personId === person.id);
  const [fields, setFields] = useState<PlanFields>(EMPTY);
  const [error, setError] = usePersonError(person.id, 'plan');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (plan: PlanFields) =>
      postJson<Plan>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/plans?user=${encodeURIComponent(user)}`,
        plan,
      ),
    onSuccess: async () => {
      setFields(EMPTY);
      await queryClient.invalidateQueries({ queryKey: plansKey(sheet) });
    },
    onError: () => setError('The plan was refused: check its amount, interval or day, and dates.'),
  });

  const change = (key: keyof PlanFields) => (e: { target: { value: string } }) => {
    setFields((f) => ({ ...f, [key]: e.target.value }));
    setError('');
  };

  const submit = () => {
    setError('');
    once(() => add.mutateAsync(fields));
  };

  return (
    <td>
      {mine.map((p) => (
        <div key={p.id} data-testid={`plan-row-${p.id}`}>
          {p.amount} from {p.start} {p.day != null ? `monthly on day ${p.day}` : `every ${p.every} days`} to {p.end}, made{' '}
          <span data-testid="plan-generated">{p.generated}</span>, next{' '}
          <span data-testid="plan-next-dates">{p.nextDates}</span>
          <PlanPause person={person} plan={p} />
        </div>
      ))}
      <input data-testid="plan-form-amount" placeholder="amount" size={7} value={fields.amount} onChange={change('amount')} />
      <input data-testid="plan-form-start" type="date" value={fields.start} onChange={change('start')} />
      <input data-testid="plan-form-every" placeholder="every N days" size={4} value={fields.every} onChange={change('every')} />
      <input data-testid="plan-form-day" placeholder="or day of month" size={4} value={fields.day} onChange={change('day')} />
      <input data-testid="plan-form-end" type="date" value={fields.end} onChange={change('end')} />
      <button type="button" data-testid="plan-form-submit" disabled={add.isPending} onClick={submit}>
        Add plan
      </button>
      {error && <span data-testid="plan-error">{error}</span>}
    </td>
  );
}
