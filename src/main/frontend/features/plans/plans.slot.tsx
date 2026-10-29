import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsKey } from '../claims/claimsQuery';
import { peopleKey } from '../people/peopleQuery';
import { useUser } from '../session/useUser';
import { plansKey, plansQuery } from './plansQuery';
import type { Plan } from './plansQuery';

type PlanBody = { amount: string; start: string; every: string; day: string; end: string };

/**
 * A plan's row: its figures, whether it is paused, and the buttons that pause and resume it on the sheet's today. Pausing
 * a paused plan or resuming a running one is refused with the row's `plan-error`, cleared by the row's next action.
 */
function PlanRow({ sheetId, plan: p }: { sheetId: string; plan: Plan }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useState('');
  const once = useSubmitOnce();
  const pause = useMutation({
    mutationFn: (paused: boolean) =>
      putJson<Plan>(`/api/sheets/${encodeURIComponent(sheetId)}/plans/${p.id}/pause`, { paused }, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: plansKey(sheetId) }),
    onError: (e: Error) =>
      setError(e instanceof RefusedError && e.message ? e.message : 'The plan could not be changed.'),
  });
  const press = (paused: boolean) => () => {
    setError('');
    once(() => pause.mutateAsync(paused));
  };

  return (
    <tr data-testid={`plan-row-${p.id}`}>
      <td data-testid="plan-amount-shown">{p.amount}</td>
      <td data-testid="plan-start">{p.start}</td>
      <td data-testid="plan-every">{p.day > 0 ? '' : p.every}</td>
      <td data-testid="plan-day">{p.day > 0 ? p.day : ''}</td>
      <td data-testid="plan-end">{p.end}</td>
      <td>
        made <span data-testid="plan-generated">{p.generated}</span>
      </td>
      <td>
        next <span data-testid="plan-next-dates">{p.nextDates}</span>
      </td>
      <td>
        paused <span data-testid="plan-paused">{p.paused}</span>{' '}
        <button type="button" data-testid="plan-pause" disabled={pause.isPending} onClick={press(true)}>
          Pause
        </button>{' '}
        <button type="button" data-testid="plan-resume" disabled={pause.isPending} onClick={press(false)}>
          Resume
        </button>
        {error && <p data-testid="plan-error">{error}</p>}
      </td>
    </tr>
  );
}

/**
 * The person's plans, each with how many claims it has made so far, and the form a plan is added through: an amount,
 * a start date, every N days (a whole number, at least 1) or a day of the month (1 to 31, a day beyond the month's length
 * on its last day) and an end date (a blank date is the sheet's today). An end
 * before the start, or a bad amount or interval, is refused with the row's `plan-error`.
 */
function PersonPlans({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const plans = (useQuery(plansQuery(sheetId)).data ?? []).filter((p) => p.personId === personId);
  const [amount, setAmount] = useState('');
  const [start, setStart] = useState('');
  const [every, setEvery] = useState('');
  const [day, setDay] = useState('');
  const [end, setEnd] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: PlanBody) =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/plans`, body, user),
    onSuccess: async () => {
      setAmount('');
      setStart('');
      setEvery('');
      setDay('');
      setEnd('');
      await queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
    },
    onError: (e: Error) =>
      setError(e instanceof RefusedError && e.message ? e.message : 'The plan could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => add.mutateAsync({ amount: amount.trim(), start: start.trim(), every: every.trim(), day: day.trim(), end: end.trim() }));
  };

  const edit = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
  };

  return (
    <td>
      <table>
        <tbody>
          {plans.map((p) => (
            <PlanRow key={p.id} sheetId={sheetId} plan={p} />
          ))}
        </tbody>
      </table>
      <form onSubmit={submit}>
        <input data-testid="plan-form-amount" placeholder="Amount" value={amount} onChange={edit(setAmount)} />{' '}
        <input data-testid="plan-form-start" type="date" value={start} onChange={edit(setStart)} />{' '}
        every <input data-testid="plan-form-every" placeholder="days" size={4} value={every} onChange={edit(setEvery)} /> days or monthly on day{' '}
        <input data-testid="plan-form-day" placeholder="1-31" size={3} value={day} onChange={edit(setDay)} /> to{' '}
        <input data-testid="plan-form-end" type="date" value={end} onChange={edit(setEnd)} />{' '}
        <button type="submit" data-testid="plan-form-submit" disabled={add.isPending}>
          Add plan
        </button>
      </form>
      {error && <p data-testid="plan-error">{error}</p>}
    </td>
  );
}

/** Makes the plans' DRAFT claims due by the sheet's today, and shows how many were made. */
function GeneratePlans({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useState('');
  const [made, setMade] = useState<number | undefined>();
  const once = useSubmitOnce();
  const generate = useMutation({
    mutationFn: () => postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/plans/generate`, {}, user),
    onSuccess: async (r) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
      setMade(r.made);
    },
    onError: (e: Error) =>
      setError(e instanceof RefusedError && e.message ? e.message : 'The plans could not be generated.'),
  });

  return (
    <section data-testid="plans">
      <button
        type="button"
        data-testid="plan-generate"
        disabled={generate.isPending}
        onClick={() => {
          setError('');
          setMade(undefined);
          once(() => generate.mutateAsync());
        }}
      >
        Generate plan claims
      </button>
      {made !== undefined && (
        <>
          {' '}
          Made <span data-testid="plan-generated-count">{made}</span>
        </>
      )}
      {error && <p data-testid="plan-generate-error">{error}</p>}
    </section>
  );
}

// Keyed by sheet, so a typed value or an error never carries over to another sheet.
export const contributions = [
  PersonHeaders.fill({ order: 20.5, Component: () => <th>Plans</th> }),
  PersonCells.fill({ order: 20.5, Component: ({ sheetId, personId }) => <PersonPlans key={`${sheetId}-${personId}`} sheetId={sheetId} personId={personId} /> }),
  SheetPanel.fill({ order: 6.8, Component: ({ sheetId }) => <GeneratePlans key={`plans-${sheetId}`} sheetId={sheetId} /> }),
];
