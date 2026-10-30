import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from '../people/usePersonAction';
import { plansKey, usePlans, type Plan } from './usePlans';

// The person's plans, each with how many claims it has made so far and its next five dates after the sheet's today, and the form that adds one: an amount
// the claim form takes, a start (blank: the sheet's today), every N days (a whole number, at least 1) or
// instead monthly on a day of the month (1 to 31), and an end not before the start. A refusal shows the plan error on the row.
export function PlanControl({ personId }: { personId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const plans = usePlans(sheet).filter((p) => p.personId === personId);
  const [amount, setAmount] = useState('');
  const [start, setStart] = useState('');
  const [every, setEvery] = useState('');
  const [day, setDay] = useState('');
  const [end, setEnd] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (body: { amount: string; start: string; every: string; day: string; end: string }) =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/plans`, { ...body, user }),
    onSuccess: () => {
      setAmount('');
      setStart('');
      setEvery('');
      setDay('');
      setEnd('');
      return queryClient.invalidateQueries({ queryKey: plansKey(sheet) });
    },
  });
  const taken = usePersonAction(personId, () => add.reset());
  const changed = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    add.reset();
  };

  return (
    <div>
      <ul>
        {plans.map((p) => (
          <li key={p.id} data-testid={`plan-row-${p.id}`}>
            <span data-testid="plan-amount-shown">{p.amount}</span>{' '}
            {p.day !== null ? (
              <>
                monthly on day <span data-testid="plan-day">{p.day}</span>
              </>
            ) : (
              <>
                every <span data-testid="plan-every">{p.every}</span> days
              </>
            )}{' '}
            from{' '}
            <span data-testid="plan-start">{p.start}</span> to <span data-testid="plan-end">{p.end}</span>, made{' '}
            <span data-testid="plan-generated">{p.generated}</span>, next{' '}
            <span data-testid="plan-next-dates">{p.nextDates.join(', ')}</span>
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          taken();
          once(() => add.mutateAsync({ amount: amount.trim(), start: start.trim(), every: every.trim(), day: day.trim(), end: end.trim() }));
        }}
      >
        <input data-testid="plan-form-amount" value={amount} placeholder="Amount" inputMode="decimal" onChange={changed(setAmount)} />{' '}
        <input data-testid="plan-form-start" value={start} placeholder="Start yyyy-mm-dd" onChange={changed(setStart)} />{' '}
        <input data-testid="plan-form-every" value={every} placeholder="Every N days" inputMode="numeric" onChange={changed(setEvery)} />{' '}
        <input data-testid="plan-form-day" value={day} placeholder="Or day of month" inputMode="numeric" onChange={changed(setDay)} />{' '}
        <input data-testid="plan-form-end" value={end} placeholder="End yyyy-mm-dd" onChange={changed(setEnd)} />{' '}
        <button type="submit" data-testid="plan-form-submit">Add plan</button>
        {add.isError && (
          <span data-testid="plan-error" role="alert">
            {' '}
            The plan could not be added: the amount must be 0.00 or more with at most two decimals, every a whole number of days of
            at least 1 or the day of the month 1 to 31 (not both), and the end not before the start.
          </span>
        )}
      </form>
    </div>
  );
}
