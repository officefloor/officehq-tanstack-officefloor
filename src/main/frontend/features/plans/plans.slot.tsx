import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { claimsKey } from '../claims/claims';
import { peopleKey, type Person } from '../people/people';
import { plansKey, usePlans, type Plan } from './plans';

function PlansHead() {
  return <th>Plans</th>;
}

type PlanForm = { amount: string; start: string; every: string; day: string; end: string; sheet: string };
const EMPTY: PlanForm = { amount: '', start: '', every: '', day: '', end: '', sheet: '' };

/** A person's plans (amount, start, every N days or monthly on a day of the month, end, how many claims each has
 *  made so far, and its next five dates), in the order added, and the form that adds one (fill every N days or the day of the month, 1 to 31,
 *  not both, and optionally one of the sheet's descendants to make its claims on, for the person of the same name there).
 *  The server refuses a bad amount, interval or day, an end before the start, or a sheet that is not a descendant: the row's
 *  plan error shows, until the form is changed or submitted again. */
function PersonPlans({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const plans = usePlans(sheetId);
  const queryClient = useQueryClient();
  const [form, setForm] = useState<PlanForm>(EMPTY);

  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (f: PlanForm) =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/plans`, { ...f, by: user }),
    onSuccess: () => {
      setForm(EMPTY);
      return queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
    },
    onSettled: submit.end,
  });

  const change = (key: keyof PlanForm) => (e: ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [key]: e.target.value });
    add.reset();
  };
  const mine = (plans.data ?? []).filter((p) => p.personId === person.id);

  return (
    <td>
      <ul>
        {mine.map((p) => (
          <PlanRow key={p.id} sheetId={sheetId} user={user} plan={p} />
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          add.reset();
          add.mutate(form);
        }}
      >
        <input data-testid="plan-form-amount" placeholder="Amount" size={7} value={form.amount} onChange={change('amount')} />{' '}
        <input data-testid="plan-form-start" type="date" value={form.start} onChange={change('start')} />{' '}
        <input data-testid="plan-form-every" placeholder="Every N days" size={4} value={form.every} onChange={change('every')} />{' '}
        <input data-testid="plan-form-day" placeholder="Or day of month" size={4} value={form.day} onChange={change('day')} />{' '}
        <input data-testid="plan-form-sheet" placeholder="Descendant sheet" size={10} value={form.sheet} onChange={change('sheet')} />{' '}
        <input data-testid="plan-form-end" type="date" value={form.end} onChange={change('end')} />{' '}
        <button type="submit" data-testid="plan-form-submit" disabled={add.isPending}>
          Add plan
        </button>
        {add.isError && <span data-testid="plan-error"> Could not add this plan.</span>}
      </form>
    </td>
  );
}

/** A plan (amount, interval, start, end, how many claims it has made, its next dates, whether it is paused and its
 *  changes of amount, oldest first), the buttons that pause and resume it — a date on or after the day it is paused
 *  and before the day it is resumed is skipped — and the entry that changes its amount for the claims generated from
 *  then on and for those it made that are still DRAFT. The server refuses pausing a paused plan, resuming a running one or a bad amount: the plan's error shows,
 *  until the next press or the amount is changed. */
function PlanRow({ sheetId, user, plan: p }: { sheetId: string; user: string; plan: Plan }) {
  const queryClient = useQueryClient();
  const submit = useSubmitOnce();
  const [amount, setAmount] = useState('');
  const act = useMutation({
    mutationFn: (action: 'pause' | 'resume') =>
      postJson<void>(`/api/sheets/${encodeURIComponent(sheetId)}/plans/${p.id}/${action}`, { by: user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: plansKey(sheetId) }),
    onSettled: submit.end,
  });
  const save = useMutation({
    mutationFn: (a: string) =>
      postJson<void>(`/api/sheets/${encodeURIComponent(sheetId)}/plans/${p.id}/amount`, { amount: a, by: user }),
    onSuccess: () => {
      setAmount('');
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      return queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
    },
    onSettled: submit.end,
  });
  const press = (action: 'pause' | 'resume') => () => {
    if (!submit.begin()) return;
    act.reset();
    save.reset();
    act.mutate(action);
  };
  const pressSave = () => {
    if (!submit.begin()) return;
    act.reset();
    save.reset();
    save.mutate(amount);
  };

  return (
    <li data-testid={`plan-row-${p.id}`}>
      {p.amount} {p.day != null ? `monthly on day ${p.day}` : `every ${p.every} days`} from {p.start} to {p.end}
      {p.sheet != null && ` on sheet ${p.sheet}`}; made{' '}
      <span data-testid="plan-generated">{p.generated}</span>; next{' '}
      <span data-testid="plan-next-dates">{p.nextDates}</span>; paused{' '}
      <span data-testid="plan-paused">{p.paused ? 'yes' : 'no'}</span>{' '}
      <button type="button" data-testid="plan-pause" disabled={act.isPending} onClick={press('pause')}>
        Pause
      </button>{' '}
      <button type="button" data-testid="plan-resume" disabled={act.isPending} onClick={press('resume')}>
        Resume
      </button>
{' '}
      <input
        data-testid="plan-amount"
        placeholder="New amount"
        size={7}
        value={amount}
        onChange={(e) => {
          setAmount(e.target.value);
          act.reset();
          save.reset();
        }}
      />{' '}
      <button type="button" data-testid="plan-amount-save" disabled={save.isPending} onClick={pressSave}>
        Save amount
      </button>
      {(act.isError || save.isError) && <span data-testid="plan-error"> Could not change this plan.</span>}
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

type Generated = { made: number };

/** The generate button: for every plan on the sheet, in the order added, a DRAFT claim of the plan's amount for each
 *  plan date (the start, then every N days; or its day in every month from the start's month, on or after the
 *  start) up to its end and the sheet's today that it has made none for yet (on its descendant sheet, for the person of the
 *  same name there, when it names one), by the claim form's rules (a date they refuse is skipped, and tried again next time). Shows how many were made; the count
 *  is cleared while a new press runs. */
function GeneratePlans({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const submit = useSubmitOnce();
  const generate = useMutation({
    mutationFn: () => postJson<Generated>(`/api/sheets/${encodeURIComponent(sheetId)}/generate-plans`, { by: user }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <p>
      <button
        type="button"
        data-testid="plan-generate"
        disabled={generate.isPending}
        onClick={() => {
          if (!submit.begin()) return;
          generate.reset();
          generate.mutate();
        }}
      >
        Generate plan claims
      </button>
      {generate.isSuccess && (
        <>
          {' '}
          Made <span data-testid="plan-generated-count">{generate.data.made}</span>
        </>
      )}
      {generate.isError && <span data-testid="plan-generate-error"> Could not generate the plans' claims.</span>}
    </p>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 65, Component: PlansHead }),
  PersonRow.fill({ order: 65, Component: PersonPlans }),
  SheetPage.fill({ order: 24.5, Component: GeneratePlans }),
];
