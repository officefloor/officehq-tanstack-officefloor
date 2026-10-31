import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from '../people/usePersonAction';
import { plansKey, usePlans, type Plan } from './usePlans';

// The person's plans, each with how many claims it has made so far, its next five dates after the sheet's today and whether it is
// paused (with its pause and resume buttons), and the form that adds one: an amount
// the claim form takes, a start (blank: the sheet's today), every N days (a whole number, at least 1) or
// instead monthly on a day of the month (1 to 31), and an end not before the start, and optionally a descendant sheet
// whose person of the same name gets its claims. A refusal shows the plan error on the row.
export function PlanControl({ personId }: { personId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const plans = usePlans(sheet).filter((p) => p.personId === personId);
  const [amount, setAmount] = useState('');
  const [start, setStart] = useState('');
  const [every, setEvery] = useState('');
  const [day, setDay] = useState('');
  const [end, setEnd] = useState('');
  const [target, setTarget] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (body: { amount: string; start: string; every: string; day: string; end: string; sheet: string }) =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/plans`, { ...body, user }),
    onSuccess: () => {
      setAmount('');
      setStart('');
      setEvery('');
      setDay('');
      setEnd('');
      setTarget('');
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
          <PlanRow key={p.id} plan={p} />
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          taken();
          once(() => add.mutateAsync({ amount: amount.trim(), start: start.trim(), every: every.trim(), day: day.trim(), end: end.trim(), sheet: target.trim() }));
        }}
      >
        <input data-testid="plan-form-amount" value={amount} placeholder="Amount" inputMode="decimal" onChange={changed(setAmount)} />{' '}
        <input data-testid="plan-form-start" value={start} placeholder="Start yyyy-mm-dd" onChange={changed(setStart)} />{' '}
        <input data-testid="plan-form-every" value={every} placeholder="Every N days" inputMode="numeric" onChange={changed(setEvery)} />{' '}
        <input data-testid="plan-form-day" value={day} placeholder="Or day of month" inputMode="numeric" onChange={changed(setDay)} />{' '}
        <input data-testid="plan-form-end" value={end} placeholder="End yyyy-mm-dd" onChange={changed(setEnd)} />{' '}
        <input data-testid="plan-form-sheet" value={target} placeholder="On sheet (a descendant)" onChange={changed(setTarget)} />{' '}
        <button type="submit" data-testid="plan-form-submit">Add plan</button>
        {add.isError && (
          <span data-testid="plan-error" role="alert">
            {' '}
            The plan could not be added: the amount must be 0.00 or more with at most two decimals, every a whole number of days of
            at least 1 or the day of the month 1 to 31 (not both), the end not before the start, and the sheet blank or one of this sheet's descendants.
          </span>
        )}
      </form>
    </div>
  );
}

// One plan's line. Pause skips the plan's dates from the sheet's today until it is resumed; pausing a paused plan or resuming a
// running one is refused, shown as the plan error on the line. The amount entry changes the plan's amount from the sheet's today
// (the claims it generates from then on take it, those already made keep theirs); a bad amount is refused the same way. The
// plan's amount changes are listed under it, oldest first.
function PlanRow({ plan: p }: { plan: Plan }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const toggle = useMutation({
    mutationFn: (action: 'pause' | 'resume') =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheet)}/plans/${encodeURIComponent(p.id)}/${action}`, { user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: plansKey(sheet) }),
  });
  const [amount, setAmount] = useState('');
  const change = useMutation({
    mutationFn: (value: string) =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheet)}/plans/${encodeURIComponent(p.id)}/amount`, { amount: value, user }),
    onSuccess: () => {
      setAmount('');
      return queryClient.invalidateQueries({ queryKey: plansKey(sheet) });
    },
  });
  const taken = usePersonAction(p.personId, () => {
    toggle.reset();
    change.reset();
  });
  const act = (action: 'pause' | 'resume') => {
    taken();
    once(() => toggle.mutateAsync(action));
  };

  return (
    <li data-testid={`plan-row-${p.id}`}>
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
      <span data-testid="plan-start">{p.start}</span> to <span data-testid="plan-end">{p.end}</span>
      {p.sheet !== null && <> on sheet <span data-testid="plan-sheet">{p.sheet}</span></>}, made{' '}
      <span data-testid="plan-generated">{p.generated}</span>, next{' '}
      <span data-testid="plan-next-dates">{p.nextDates.join(', ')}</span>, paused{' '}
      <span data-testid="plan-paused">{p.paused ? 'yes' : 'no'}</span>{' '}
      <button type="button" data-testid="plan-pause" onClick={() => act('pause')}>
        Pause
      </button>{' '}
      <button type="button" data-testid="plan-resume" onClick={() => act('resume')}>
        Resume
      </button>{' '}
      <input
        data-testid="plan-amount"
        value={amount}
        placeholder="New amount"
        inputMode="decimal"
        onChange={(e) => {
          setAmount(e.target.value);
          change.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="plan-amount-save"
        onClick={() => {
          taken();
          once(() => change.mutateAsync(amount.trim()));
        }}
      >
        Change amount
      </button>
      {toggle.isError && (
        <span data-testid="plan-error" role="alert">
          {' '}
          The plan could not be {toggle.variables === 'pause' ? 'paused: it is already paused' : 'resumed: it is not paused'}.
        </span>
      )}
      {change.isError && (
        <span data-testid="plan-error" role="alert">
          {' '}
          The plan's amount could not be changed: it must be 0.00 or more with at most two decimals.
        </span>
      )}
      <ul>
        {p.changes.map((c, i) => (
          <li key={i} data-testid="plan-change-line">
            {c}
          </li>
        ))}
      </ul>
    </li>
  );
}
