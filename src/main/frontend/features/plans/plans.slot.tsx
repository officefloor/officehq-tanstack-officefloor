import { useEffect, useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { peoplePath } from '../people/people';
import type { Person } from '../people/people';
import { claimsKey } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { plansKey, plansPath, usePlans } from './plans';
import type { Plan } from './plans';

type PlanForm = { amount: string; start: string; every: string; day: string; end: string };

const EMPTY: PlanForm = { amount: '', start: '', every: '', day: '', end: '' };

function PlansHeader() {
  return <th>Plans</th>;
}

// A person's plans, each with how many claims it has made so far and its next five dates after the sheet's today, and the form adding one: an amount (0.00 or more,
// up to two decimals), a start date (blank: the sheet's today), every N days (a whole number, at least 1) or monthly
// on a day of the month (1 to 31, filled instead of every), and an end date not before the start. The server refuses anything else.
function PersonPlans({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: plans = [] } = usePlans(sheet);
  const [form, setForm] = useState<PlanForm>(EMPTY);
  // Counts the add form's submissions, so a plan's old pause/resume error goes when the person's next plan is sent.
  const [submits, setSubmits] = useState(0);
  const mine = plans.filter((p) => p.person === person.id);

  const add = useMutation({
    mutationFn: (body: PlanForm) => postJson<Plan[]>(byUser(`${peoplePath(sheet)}/${person.id}/plans`, user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(plansKey(sheet), updated);
      setForm(EMPTY);
    },
  });
  const send = useSubmitOnce(add);

  const field = (key: keyof PlanForm) => ({
    value: form[key],
    onChange: (e: { target: { value: string } }) => {
      setForm({ ...form, [key]: e.target.value });
      add.reset();
    },
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setSubmits((n) => n + 1);
    send(form);
  };

  return (
    <td>
      <ul>
        {mine.map((p) => (
          <PlanRow key={p.id} sheet={sheet} user={user} plan={p} submits={submits} />
        ))}
      </ul>
      <form onSubmit={submit}>
        <input data-testid="plan-form-amount" placeholder="Amount" size={7} {...field('amount')} />{' '}
        <input data-testid="plan-form-start" type="date" {...field('start')} />{' '}
        <input data-testid="plan-form-every" placeholder="Every N days" size={4} {...field('every')} />{' '}
        <input data-testid="plan-form-day" placeholder="Or day of month" size={4} {...field('day')} />{' '}
        <input data-testid="plan-form-end" type="date" {...field('end')} />{' '}
        <button type="submit" data-testid="plan-form-submit" disabled={add.isPending}>
          Add plan
        </button>
        {add.isError && (
          <span data-testid="plan-error" role="alert">
            {' '}A plan needs an amount of 0.00 or more with up to two decimals, every 1 or more whole days or
            a day of the month from 1 to 31, and an end
            not before its start.
          </span>
        )}
      </form>
    </td>
  );
}

// One plan, with its Pause and Resume buttons: a pause skips (never postpones) every plan date from the sheet's
// today until the day it is resumed. Pausing a paused plan, or resuming a running one, is refused.
function PlanRow({ sheet, user, plan: p, submits }: { sheet: string; user: string; plan: Plan; submits: number }) {
  const queryClient = useQueryClient();
  const toggle = useMutation({
    mutationFn: (action: 'pause' | 'resume') =>
      postJson<Plan[]>(byUser(`${plansPath(sheet)}/${p.id}/${action}`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(plansKey(sheet), updated),
  });
  const send = useSubmitOnce(toggle);
  const { reset } = toggle;
  useEffect(() => reset(), [submits, reset]);

  return (
    <li data-testid={`plan-row-${p.id}`}>
      {p.amount} from {p.start} {p.day != null ? `monthly on day ${p.day}` : `every ${p.every} days`} to {p.end};
      made{' '}
      <span data-testid="plan-generated">{p.generated}</span>; next{' '}
      <span data-testid="plan-next-dates">{p.nextDates.join(', ')}</span>; paused{' '}
      <span data-testid="plan-paused">{p.paused ? 'yes' : 'no'}</span>{' '}
      <button type="button" data-testid="plan-pause" disabled={toggle.isPending} onClick={() => send('pause')}>
        Pause
      </button>{' '}
      <button type="button" data-testid="plan-resume" disabled={toggle.isPending} onClick={() => send('resume')}>
        Resume
      </button>
      {toggle.isError && (
        <span data-testid="plan-error" role="alert">
          {' '}{p.paused ? 'The plan is already paused.' : 'The plan is not paused.'}
        </span>
      )}
    </li>
  );
}

type Generated = { made: number; claims: Claim[]; plans: Plan[] };

// Makes, for every plan on the sheet in the order added, a DRAFT claim for each plan date up to its end and the
// sheet's today that has none yet (a date the claim form refuses is tried again next time). Shows how many were made.
function GeneratePlans({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const generate = useMutation({
    mutationFn: () => postJson<Generated>(byUser(`${plansPath(sheet)}/generate`, user), {}),
    onSuccess: (result) => {
      queryClient.setQueryData(claimsKey(sheet), result.claims);
      queryClient.setQueryData(plansKey(sheet), result.plans);
    },
  });
  const send = useSubmitOnce(generate);

  return (
    <p>
      <button type="button" data-testid="plan-generate" disabled={generate.isPending} onClick={() => send(undefined)}>
        Generate planned claims
      </button>
      {generate.isSuccess && (
        <>
          {' '}Made <strong data-testid="plan-generated-count">{generate.data.made}</strong>
        </>
      )}
      {generate.isError && (
        <span data-testid="plan-generate-error" role="alert">
          {' '}The planned claims could not be made.
        </span>
      )}
    </p>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 55, Component: PlansHeader }),
  PersonColumns.fill({ order: 55, Component: PersonPlans }),
  SheetPanels.fill({ order: 23.5, Component: GeneratePlans }),
];
