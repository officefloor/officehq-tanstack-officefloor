import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { addPlan, changePlanAmount, listPlans, pausePlan, plansKey, type Plan, type PlanInput } from './api';
import { Fig } from '../session/Fig';

const EMPTY: PlanInput = { amount: '', start: '', every: '', day: '', end: '', sheet: '' };

/** A person's plans (each its amount, start, every N days or day of the month, end, how many claims it has made so far, its next dates and whether it is paused, with pause and resume buttons, and its amount's changes with an entry that changes it)
 *  and a form that adds one; a refused add shows an error on the row and adds nothing. */
export function PersonPlans({ sheetId, user, personId }: { sheetId: string; user: string; personId: number }) {
  const queryClient = useQueryClient();
  const plans = useQuery({ queryKey: plansKey(sheetId), queryFn: () => listPlans(sheetId) });
  const [form, setForm] = useState<PlanInput>(EMPTY);
  const submitOnce = useSubmitOnce();

  const add = useMutation({
    mutationFn: (input: PlanInput) => addPlan(sheetId, user, personId, input),
    onSuccess: () => {
      setForm(EMPTY);
      return queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
    },
  });

  const change = (patch: Partial<PlanInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    add.reset();
  };

  return (
    <div>
      Plans
      <table>
        <tbody>
          {(plans.data ?? []).filter((pl) => pl.personId === personId).map((pl) => (
            <tr key={pl.id} data-testid={`plan-row-${pl.id}`}>
              <td data-testid="plan-amount-shown"><Fig>{Number(pl.amount).toFixed(2)}</Fig></td>
              <td data-testid="plan-start">{pl.start}</td>
              {pl.day != null
                ? <td>monthly on day <span data-testid="plan-day">{pl.day}</span></td>
                : <td>every <span data-testid="plan-every">{pl.every}</span> days</td>}
              <td>to <span data-testid="plan-end">{pl.end}</span></td>
              <td>{pl.targetSheet ? <>on sheet <span data-testid="plan-sheet">{pl.targetSheet}</span></> : null}</td>
              <td>made <span data-testid="plan-generated">{pl.generated}</span></td>
              <td>next <span data-testid="plan-next-dates">{(pl.nextDates ?? []).join(', ')}</span></td>
              <PlanPause sheetId={sheetId} user={user} plan={pl} />
              <PlanAmount sheetId={sheetId} user={user} plan={pl} />
            </tr>
          ))}
        </tbody>
      </table>
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
        <input data-testid="plan-form-amount" placeholder="Amount" value={form.amount}
          onChange={(e) => change({ amount: e.target.value })} />{' '}
        <input type="date" data-testid="plan-form-start" value={form.start}
          onChange={(e) => change({ start: e.target.value })} />{' '}
        <input data-testid="plan-form-every" placeholder="Every N days" value={form.every}
          onChange={(e) => change({ every: e.target.value })} />{' '}
        <input data-testid="plan-form-day" placeholder="or day of month" value={form.day}
          onChange={(e) => change({ day: e.target.value })} />{' '}
        <input type="date" data-testid="plan-form-end" value={form.end}
          onChange={(e) => change({ end: e.target.value })} />{' '}
        <input data-testid="plan-form-sheet" placeholder="On sheet (a descendant)" value={form.sheet}
          onChange={(e) => change({ sheet: e.target.value })} />{' '}
        <button type="submit" data-testid="plan-form-submit" disabled={add.isPending}>
          Add plan
        </button>
        {add.isError && (
          <span data-testid="plan-error">A plan needs an amount of 0.00 or more with up to two decimals, a start, either every a whole number of days (at least 1) or a day of the month (1 to 31), an end not before the start, and a sheet (if any) that is a descendant of this one.</span>
        )}
      </form>
    </div>
  );
}

/** The plan's paused yes/no and its pause and resume buttons; a refused pause or resume shows an error on the row. */
function PlanPause({ sheetId, user, plan }: { sheetId: string; user: string; plan: Plan }) {
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const pause = useMutation({
    mutationFn: (on: boolean) => pausePlan(sheetId, user, plan.id, on),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: plansKey(sheetId) }),
  });
  const act = (on: boolean) => submitOnce(() => {
    pause.reset();
    return pause.mutateAsync(on);
  });

  return (
    <td>
      paused <span data-testid="plan-paused">{plan.paused ? 'yes' : 'no'}</span>{' '}
      <button type="button" data-testid="plan-pause" disabled={pause.isPending} onClick={() => act(true)}>Pause</button>{' '}
      <button type="button" data-testid="plan-resume" disabled={pause.isPending} onClick={() => act(false)}>Resume</button>
      {pause.isError && (
        <span data-testid="plan-error">A paused plan cannot be paused again, nor a running plan resumed.</span>
      )}
    </td>
  );
}

/** The plan's amount changes, oldest first, each as `<date> amount=<amount>`, and an entry and save button
 *  that change its amount; a refused change shows an error on the row. */
function PlanAmount({ sheetId, user, plan }: { sheetId: string; user: string; plan: Plan }) {
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const [amount, setAmount] = useState('');
  const save = useMutation({
    mutationFn: (value: string) => changePlanAmount(sheetId, user, plan.id, value),
    onSuccess: () => {
      setAmount('');
      return queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
    },
  });

  return (
    <td>
      <ul>
        {(plan.changes ?? []).map((c, i) => (
          <li key={i} data-testid="plan-change-line">{c.changedOn} amount={Number(c.amount).toFixed(2)}</li>
        ))}
      </ul>
      <input data-testid="plan-amount" placeholder="New amount" value={amount}
        onChange={(e) => {
          setAmount(e.target.value);
          save.reset();
        }} />{' '}
      <button type="button" data-testid="plan-amount-save" disabled={save.isPending}
        onClick={() => {
          const value = amount;
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(value);
          });
        }}>
        Save amount
      </button>
      {save.isError && (
        <span data-testid="plan-error">A plan's amount is 0.00 or more with up to two decimals.</span>
      )}
    </td>
  );
}
