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

// A person's plans, each with how many claims it has made, and a form adding one: an amount, a
// start (blank: the sheet's today), every N days (a whole number, at least 1) or a day of the month (1 to 31), and an end not
// before the start. A refused plan shows the error on the row and adds nothing.
function PersonPlans({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const plans = usePlans(sheetId);
  const [amount, setAmount] = useState('');
  const [start, setStart] = useState('');
  const [every, setEvery] = useState('');
  const [day, setDay] = useState('');
  const [end, setEnd] = useState('');
  const [error, setError] = useRowError(person.id, 'plan');
  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: { amount: string; start: string; every: string; day: string; end: string }) =>
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
      return queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
    },
    onError: () => setError('That plan could not be added.'),
  });

  const submit = () => {
    setError('');
    once((done) =>
      add.mutate(
        { amount: amount.trim(), start: start.trim(), every: every.trim(), day: day.trim(), end: end.trim() },
        { onSettled: done },
      ),
    );
  };

  const changed = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
  };

  return (
    <td>
      {(plans.data ?? [])
        .filter((p) => p.personId === person.id)
        .map((p) => (
          <div key={p.id} data-testid={`plan-row-${p.id}`}>
            <span data-testid="plan-amount-shown">{p.amount}</span> from {p.start} {p.day !== null ? `monthly on day ${p.day}` : `every ${p.every} days`} to {p.end},
            made <span data-testid="plan-generated">{p.generated}</span>
          </div>
        ))}
      <input data-testid="plan-form-amount" aria-label="Plan amount" value={amount} onChange={changed(setAmount)} />{' '}
      <input type="date" data-testid="plan-form-start" aria-label="Plan start" value={start} onChange={changed(setStart)} />{' '}
      <input data-testid="plan-form-every" aria-label="Plan every N days" value={every} onChange={changed(setEvery)} />{' '}
      <input data-testid="plan-form-day" aria-label="Plan day of the month" value={day} onChange={changed(setDay)} />{' '}
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
