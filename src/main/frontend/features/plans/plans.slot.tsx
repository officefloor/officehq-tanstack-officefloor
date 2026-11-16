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

type PlanForm = { amount: string; start: string; every: string; day: string; end: string };
const EMPTY: PlanForm = { amount: '', start: '', every: '', day: '', end: '' };

/** A person's plans (amount, start, every N days or monthly on a day of the month, end, and how many claims each has
 *  made so far), in the order added, and the form that adds one (fill every N days or the day of the month, 1 to 31,
 *  not both). The server refuses a bad amount, interval or day, or an end before the start: the row's
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
          <li key={p.id} data-testid={`plan-row-${p.id}`}>
            {p.amount} {p.day != null ? `monthly on day ${p.day}` : `every ${p.every} days`} from {p.start} to {p.end}; made{' '}
            <span data-testid="plan-generated">{p.generated}</span>
          </li>
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
        <input data-testid="plan-form-end" type="date" value={form.end} onChange={change('end')} />{' '}
        <button type="submit" data-testid="plan-form-submit" disabled={add.isPending}>
          Add plan
        </button>
        {add.isError && <span data-testid="plan-error"> Could not add this plan.</span>}
      </form>
    </td>
  );
}

type Generated = { made: number };

/** The generate button: for every plan on the sheet, in the order added, a DRAFT claim of the plan's amount for each
 *  plan date (the start, then every N days; or its day in every month from the start's month, on or after the
 *  start) up to its end and the sheet's today that it has made none for yet, by the
 *  claim form's rules (a date they refuse is skipped, and tried again next time). Shows how many were made; the count
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
