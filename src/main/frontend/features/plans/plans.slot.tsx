import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';
import { plansKey, usePlans } from './plans';
import type { Plan } from './plans';

function PlansHead() {
  return <th>Plans</th>;
}

// A person's plans, each with how many claims it has made and its next five dates, and a form adding one: an amount, a
// start (blank: the sheet's today), every N days (a whole number, at least 1) or a day of the month (1 to 31), and an end not
// before the start, and optionally a descendant sheet whose person of the same name gets its claims. A refused plan shows the error on the row and adds nothing.
function PersonPlans({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const plans = usePlans(sheetId);
  const [amount, setAmount] = useState('');
  const [start, setStart] = useState('');
  const [every, setEvery] = useState('');
  const [day, setDay] = useState('');
  const [end, setEnd] = useState('');
  const [target, setTarget] = useState('');
  const [error, setError] = useRowError(person.id, 'plan');
  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: { amount: string; start: string; every: string; day: string; end: string; sheet: string }) =>
      postJson<Plan>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/plans`,
        body,
        user,
      ),
    onSuccess: () => {
      setAmount('');
      setStart('');
      setEvery('');
      setDay('');
      setEnd('');
      setTarget('');
      return queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
    },
    onError: () => setError('That plan could not be added.'),
  });

  // Pauses or resumes a plan on the sheet's today; pausing a paused plan or resuming a running one is refused.
  const pause = useMutation({
    mutationFn: ({ planId, on }: { planId: string; on: boolean }) =>
      postJson<Plan>(
        `/api/sheets/${encodeURIComponent(sheetId)}/plans/${encodeURIComponent(planId)}/${on ? 'pause' : 'resume'}`,
        {},
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: plansKey(sheetId) }),
    onError: (_e, { planId, on }) =>
      setPauseError({ planId, message: on ? 'That plan could not be paused.' : 'That plan could not be resumed.' }),
  });

  const [pauseError, setPauseError] = useState<{ planId: string; message: string } | null>(null);

  // Changes a plan's amount on the sheet's today: claims generated from then on take it, and the claims it
  // generated that are still DRAFT are edited to it (so the whole sheet is re-read); claims in any other
  // status keep theirs. An amount the claim form would refuse shows the error on the plan.
  const [newAmounts, setNewAmounts] = useState<Record<string, string>>({});
  const changeAmount = useMutation({
    mutationFn: ({ planId, amount }: { planId: string; amount: string }) =>
      postJson<Plan>(
        `/api/sheets/${encodeURIComponent(sheetId)}/plans/${encodeURIComponent(planId)}/amount`,
        { amount },
        user,
      ),
    onSuccess: (_r, { planId }) => {
      setNewAmounts((m) => ({ ...m, [planId]: '' }));
      return queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: (_e, { planId }) => setPauseError({ planId, message: 'That amount could not be saved.' }),
  });

  const pressChangeAmount = (planId: string) => {
    setError('');
    setPauseError(null);
    const amount = (newAmounts[planId] ?? '').trim();
    once((done) => changeAmount.mutate({ planId, amount }, { onSettled: done }));
  };

  const pressPause = (planId: string, on: boolean) => {
    setError('');
    setPauseError(null);
    once((done) => pause.mutate({ planId, on }, { onSettled: done }));
  };

  const submit = () => {
    setError('');
    setPauseError(null);
    once((done) =>
      add.mutate(
        { amount: amount.trim(), start: start.trim(), every: every.trim(), day: day.trim(), end: end.trim(), sheet: target.trim() },
        { onSettled: done },
      ),
    );
  };

  const changed = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
    setPauseError(null);
  };

  return (
    <td>
      {(plans.data ?? [])
        .filter((p) => p.personId === person.id)
        .map((p) => (
          <div key={p.id} data-testid={`plan-row-${p.id}`}>
            <span data-testid="plan-amount-shown">{p.amount}</span> from {p.start} {p.day !== null ? `monthly on day ${p.day}` : `every ${p.every} days`} to {p.end}{p.sheet ? ` on ${p.sheet}` : ''},
            made <span data-testid="plan-generated">{p.generated}</span>,
            next <span data-testid="plan-next-dates">{p.nextDates}</span>,
            paused <span data-testid="plan-paused">{p.paused ? 'yes' : 'no'}</span>{' '}
            <button type="button" data-testid="plan-pause" onClick={() => pressPause(p.id, true)} disabled={pause.isPending}>
              Pause
            </button>{' '}
            <button type="button" data-testid="plan-resume" onClick={() => pressPause(p.id, false)} disabled={pause.isPending}>
              Resume
            </button>{' '}
            <input
              data-testid="plan-amount"
              aria-label="New plan amount"
              value={newAmounts[p.id] ?? ''}
              onChange={(e) => {
                const value = e.target.value;
                setNewAmounts((m) => ({ ...m, [p.id]: value }));
                setError('');
                setPauseError(null);
              }}
            />{' '}
            <button
              type="button"
              data-testid="plan-amount-save"
              onClick={() => pressChangeAmount(p.id)}
              disabled={changeAmount.isPending}
            >
              Save amount
            </button>
            <ul>
              {p.changes.map((c, i) => (
                <li key={i} data-testid="plan-change-line">
                  {c}
                </li>
              ))}
            </ul>
            {pauseError?.planId === p.id && (
              <p role="alert" data-testid="plan-error">
                {pauseError.message}
              </p>
            )}
          </div>
        ))}
      <input data-testid="plan-form-amount" aria-label="Plan amount" value={amount} onChange={changed(setAmount)} />{' '}
      <input type="date" data-testid="plan-form-start" aria-label="Plan start" value={start} onChange={changed(setStart)} />{' '}
      <input data-testid="plan-form-every" aria-label="Plan every N days" value={every} onChange={changed(setEvery)} />{' '}
      <input data-testid="plan-form-day" aria-label="Plan day of the month" value={day} onChange={changed(setDay)} />{' '}
      <input data-testid="plan-form-sheet" aria-label="Plan descendant sheet" value={target} onChange={changed(setTarget)} />{' '}
      <input type="date" data-testid="plan-form-end" aria-label="Plan end" value={end} onChange={changed(setEnd)} />{' '}
      <button type="button" data-testid="plan-form-submit" onClick={submit} disabled={add.isPending}>
        Add plan
      </button>
      {error && (
        <p role="alert" data-testid="plan-error">
          {error}
        </p>
      )}
    </td>
  );
}

// Makes the plans' claims: for every plan in the order added, a DRAFT claim for each plan date up to
// its end and the sheet's today that has none yet (a date the claim form refuses is tried again next
// time). Shows how many were made; the count is cleared while it runs.
function GeneratePlans({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [made, setMade] = useState<number | null>(null);
  const [error, setError] = useState('');
  const once = useOnce();
  const generate = useMutation({
    mutationFn: () =>
      postJson<{ generated: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/plans/generate`, {}, user),
    onSuccess: (r) => {
      setMade(r.generated);
      void queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('The plans could not be generated.'),
  });

  const press = () => {
    setMade(null);
    setError('');
    once((done) => generate.mutate(undefined, { onSettled: done }));
  };

  return (
    <section data-testid="plan-generate-bar">
      <button type="button" data-testid="plan-generate" onClick={press} disabled={generate.isPending}>
        Generate plan claims
      </button>
      {made !== null && (
        <>
          {' '}Made <span data-testid="plan-generated-count">{made}</span>
        </>
      )}
      {error && (
        <p role="alert" data-testid="plan-generate-error">
          {error}
        </p>
      )}
    </section>
  );
}

export const contributions = [
  PersonHead.fill({ order: 56, Component: PlansHead }),
  PersonRow.fill({ order: 56, Component: PersonPlans }),
  SheetBody.fill({ order: 1.6, Component: GeneratePlans }),
];
