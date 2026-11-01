import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import type { Person } from '../people/usePeople';
import { plansKey, usePlans, type Plan } from './usePlans';

/**
 * A person's plans: an amount, a start date (blank for the sheet's today), every N days (a whole
 * number, at least 1) or instead monthly on a day of the month (1 to 31), and an end date. A bad amount or interval, or an end before the start, is
 * refused. Each plan shows how many claims it has made so far
 * and its next five plan dates after the sheet's today, up to its end. A plan can be paused and
 * resumed (pausing a paused plan or resuming a running one is refused); a plan date on or after the
 * day paused and before the day resumed is skipped, so it is left out of the next dates. A plan's
 * amount can be changed (a bad amount is refused): the new amount applies to the claims made from then
 * on. Each plan lists its changes, oldest first, as `<date> amount=<amount>`.
 */
function PersonPlans({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: plans = [] } = usePlans(sheet);
  const [amount, setAmount] = useState('');
  const [start, setStart] = useState('');
  const [every, setEvery] = useState('');
  const [day, setDay] = useState('');
  const [end, setEnd] = useState('');
  const [newAmounts, setNewAmounts] = useState<Record<number, string>>({});

  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: () =>
      postJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/plans`, {
        amount,
        start,
        every,
        day,
        end,
        user,
      }),
    onSuccess: async () => {
      setAmount('');
      setStart('');
      setEvery('');
      setDay('');
      setEnd('');
      await queryClient.invalidateQueries({ queryKey: plansKey(sheet) });
    },
  });
  const pause = useMutation({
    mutationFn: ({ planId, on }: { planId: number; on: boolean }) =>
      postJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheet)}/plans/${planId}/${on ? 'pause' : 'resume'}`, { user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: plansKey(sheet) }),
  });
  const changeAmount = useMutation({
    mutationFn: ({ planId, amount }: { planId: number; amount: string }) =>
      postJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheet)}/plans/${planId}/amount`, { amount, user }),
    onSuccess: async (_data, { planId }) => {
      setNewAmounts((was) => ({ ...was, [planId]: '' }));
      await queryClient.invalidateQueries({ queryKey: plansKey(sheet) });
    },
  });
  const change = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    add.reset();
    pause.reset();
    changeAmount.reset();
  };

  return (
    <td>
      <ul>
        {plans
          .filter((p) => p.personId === person.id)
          .map((p) => (
            <li key={p.id} data-testid={`plan-row-${p.id}`}>
              <span data-testid="plan-amount-shown">{p.amount}</span> from{' '}
              <span data-testid="plan-start">{p.start}</span>{' '}
              {p.every != null ? (
                <>
                  every <span data-testid="plan-every">{p.every}</span> days
                </>
              ) : (
                <>
                  monthly on day <span data-testid="plan-day">{p.day}</span>
                </>
              )}{' '}
              to <span data-testid="plan-end">{p.end}</span>; made{' '}
              <span data-testid="plan-generated">{p.generated}</span>; next{' '}
              <span data-testid="plan-next-dates">{p.nextDates.join(', ')}</span>; paused{' '}
              <span data-testid="plan-paused">{p.paused ? 'yes' : 'no'}</span>{' '}
              <button
                type="button"
                data-testid="plan-pause"
                disabled={pause.isPending}
                onClick={() => {
                  pause.reset();
                  changeAmount.reset();
                  pause.mutate({ planId: p.id, on: true });
                }}
              >
                Pause
              </button>{' '}
              <button
                type="button"
                data-testid="plan-resume"
                disabled={pause.isPending}
                onClick={() => {
                  pause.reset();
                  changeAmount.reset();
                  pause.mutate({ planId: p.id, on: false });
                }}
              >
                Resume
              </button>{' '}
              <input
                data-testid="plan-amount"
                aria-label="New plan amount"
                placeholder="new amount"
                size={7}
                value={newAmounts[p.id] ?? ''}
                onChange={(e) => {
                  setNewAmounts((was) => ({ ...was, [p.id]: e.target.value }));
                  changeAmount.reset();
                  pause.reset();
                }}
              />{' '}
              <button
                type="button"
                data-testid="plan-amount-save"
                disabled={changeAmount.isPending}
                onClick={() => {
                  changeAmount.reset();
                  pause.reset();
                  changeAmount.mutate({ planId: p.id, amount: newAmounts[p.id] ?? '' });
                }}
              >
                Save amount
              </button>
              <ul>
                {p.changes.map((line, i) => (
                  <li key={i} data-testid="plan-change-line">
                    {line}
                  </li>
                ))}
              </ul>
              {changeAmount.isError && changeAmount.variables?.planId === p.id && (
                <span data-testid="plan-error" role="alert">
                  {' '}
                  {changeAmount.error instanceof HttpError && changeAmount.error.body
                    ? changeAmount.error.body
                    : 'Could not change this amount (more than 0, up to two decimals).'}
                </span>
              )}
              {pause.isError && pause.variables?.planId === p.id && (
                <span data-testid="plan-error" role="alert">
                  {' '}
                  {pause.error instanceof HttpError && pause.error.body
                    ? pause.error.body
                    : 'Could not pause or resume this plan (a paused plan cannot be paused, a running one cannot be resumed).'}
                </span>
              )}
            </li>
          ))}
      </ul>
      <form
        data-testid="plan-form"
        onSubmit={(e) => {
          e.preventDefault();
          add.reset();
          pause.reset();
          changeAmount.reset();
          submitOnce(() => add.mutateAsync());
        }}
      >
        <input data-testid="plan-form-amount" aria-label="Plan amount" placeholder="amount" size={7} value={amount} onChange={change(setAmount)} />
        <input data-testid="plan-form-start" aria-label="Plan start" placeholder="start yyyy-mm-dd" size={10} value={start} onChange={change(setStart)} />
        <input data-testid="plan-form-every" aria-label="Plan every N days" placeholder="every" size={4} value={every} onChange={change(setEvery)} />
        <input data-testid="plan-form-day" aria-label="Plan day of the month" placeholder="or day 1-31" size={4} value={day} onChange={change(setDay)} />
        <input data-testid="plan-form-end" aria-label="Plan end" placeholder="end yyyy-mm-dd" size={10} value={end} onChange={change(setEnd)} />
        <button type="submit" data-testid="plan-form-submit" disabled={add.isPending}>
          Add plan
        </button>
        {add.isError && (
          <span data-testid="plan-error" role="alert">
            {' '}
            {add.error instanceof HttpError && add.error.body
              ? add.error.body
              : 'Could not add this plan (an amount, every at least 1 or a day 1 to 31, an end not before the start).'}
          </span>
        )}
      </form>
    </td>
  );
}

/**
 * Makes the plans' claims: for every plan, in the order added, a DRAFT claim for each plan date up
 * to its end and the sheet's today that it has none for yet. Shows how many were made; the count
 * goes while the next press runs.
 */
function GeneratePlans({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const generate = useMutation({
    mutationFn: () => postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheet)}/generate-plans`, { user }),
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: plansKey(sheet) }),
      ]),
  });
  return (
    <section data-testid="plans-panel">
      <button
        type="button"
        data-testid="plan-generate"
        disabled={generate.isPending}
        onClick={() => {
          generate.reset();
          generate.mutate();
        }}
      >
        Generate plan claims
      </button>
      {generate.isSuccess && (
        <span>
          {' '}
          Made <strong data-testid="plan-generated-count">{generate.data.made}</strong>
        </span>
      )}
      {generate.isError && (
        <span data-testid="plan-generate-error" role="alert">
          {' '}
          Could not generate the plans' claims.
        </span>
      )}
    </section>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 60, Component: () => <th>Plans</th> }),
  PersonCells.fill({ order: 60, Component: PersonPlans }),
  SheetPanels.fill({ order: 15, Component: GeneratePlans }),
];
