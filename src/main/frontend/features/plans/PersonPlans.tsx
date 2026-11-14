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

// A person's plans (each its amount, start, interval or day of the month, end and how many claims it has made so far)
// and the form that adds one: an amount, a start (blank: the sheet's today), every N days or a day of the month (1 to
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
          <span data-testid="plan-generated">{p.generated}</span>
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
