import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peoplePath, type Person } from '../people/people';
import { plansQueryKey, usePlans, type Plan } from './plans';

// A person's plans — each an amount, a start, every N days or monthly on a day of the month, and an end, with how many
// claims it has made so far, its next five dates after the sheet's today and whether it is paused — and the form
// that adds another. A bad amount, interval or day, both an interval and a day, or an end before the start, is
// refused.
function PlansHeader() {
  return <th>Plans</th>;
}

const blank = { amount: '', start: '', every: '', day: '', end: '' };

// One plan, with its pause and resume buttons: pausing on the sheet's today skips its dates from then until it is
// resumed. Pausing a paused plan, or resuming a running one, is refused.
function PlanRow({ person, plan: p }: { person: Person; plan: Plan }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();
  const toggle = useMutation({
    mutationFn: (action: 'pause' | 'resume') =>
      putJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheet ?? '')}/plans/${p.id}/${action}`, {}, user),
    onSuccess: (saved) => {
      queryClient.setQueryData(plansQueryKey(sheet ?? ''), saved);
    },
  });
  const submit = useSubmitOnce(toggle);
  const rowAction = useRowAction(`person-${person.id}`);
  const take = (action: 'pause' | 'resume') => { rowAction.take(); submit(action); };
  return (
    <li data-testid={`plan-row-${p.id}`}>
      <span data-testid="plan-amount">{p.amount}</span> from <span data-testid="plan-start">{p.start}</span>{' '}
      {p.day != null ? (
        <>monthly on day <span data-testid="plan-day">{p.day}</span></>
      ) : (
        <>every <span data-testid="plan-every">{p.every}</span> days</>
      )}{' '}
      to <span data-testid="plan-end">{p.end}</span>;
      made <span data-testid="plan-generated">{p.generated}</span>;
      next <span data-testid="plan-next-dates">{p.nextDates.join(', ')}</span>;
      paused <span data-testid="plan-paused">{p.paused ? 'yes' : 'no'}</span>{' '}
      <button type="button" data-testid="plan-pause" disabled={toggle.isPending} onClick={() => take('pause')}>
        Pause
      </button>
      <button type="button" data-testid="plan-resume" disabled={toggle.isPending} onClick={() => take('resume')}>
        Resume
      </button>
      {toggle.isError && rowAction.isLatest && (
        <span data-testid="plan-error">
          {toggle.variables === 'pause' ? 'The plan is already paused.' : 'The plan is not paused.'}
        </span>
      )}
    </li>
  );
}

function PersonPlans({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const plans = usePlans();
  const [form, setForm] = useState(blank);
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: typeof blank) =>
      postJson<Plan[]>(`${peoplePath(sheet ?? '')}/${person.id}/plans`, body, user),
    onSuccess: (saved) => {
      setForm(blank);
      queryClient.setQueryData(plansQueryKey(sheet ?? ''), saved);
    },
  });
  const submit = useSubmitOnce(add);
  const rowAction = useRowAction(`person-${person.id}`);
  const field = (key: keyof typeof blank) => ({
    value: form[key],
    onChange: (e: ChangeEvent<HTMLInputElement>) => { setForm({ ...form, [key]: e.target.value }); add.reset(); },
  });
  const mine = (plans.data ?? []).filter((p) => p.personId === person.id);
  return (
    <td>
      <ul>
        {mine.map((p) => <PlanRow key={p.id} person={person} plan={p} />)}
      </ul>
      <input data-testid="plan-form-amount" placeholder="Amount" {...field('amount')} />
      <input data-testid="plan-form-start" placeholder="Start YYYY-MM-DD" {...field('start')} />
      <input data-testid="plan-form-every" placeholder="Every N days" {...field('every')} />
      <input data-testid="plan-form-day" placeholder="Or day of month 1-31" {...field('day')} />
      <input data-testid="plan-form-end" placeholder="End YYYY-MM-DD" {...field('end')} />
      <button
        type="button"
        data-testid="plan-form-submit"
        disabled={add.isPending}
        onClick={() => { rowAction.take(); submit(form); }}
      >
        Add plan
      </button>
      {add.isError && rowAction.isLatest && (
        <span data-testid="plan-error">
          A plan needs an amount, 0.00 or more with up to two decimals, a start and an end date, the end not before the
          start, and either every N days, a whole number of at least 1, or a day of the month from 1 to 31.
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 19, Component: PlansHeader }),
  PersonCells.fill({ order: 19, Component: PersonPlans }),
];
