import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peoplePath, type Person } from '../people/people';
import { plansQueryKey, usePlans, type Plan } from './plans';

// A person's plans — each an amount, a start, every N days and an end, with how many claims it has made so far — and
// the form that adds another. A bad amount or interval, or an end before the start, is refused.
function PlansHeader() {
  return <th>Plans</th>;
}

const blank = { amount: '', start: '', every: '', end: '' };

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
        {mine.map((p) => (
          <li key={p.id} data-testid={`plan-row-${p.id}`}>
            <span data-testid="plan-amount">{p.amount}</span> from <span data-testid="plan-start">{p.start}</span>{' '}
            every <span data-testid="plan-every">{p.every}</span> days to <span data-testid="plan-end">{p.end}</span>;
            made <span data-testid="plan-generated">{p.generated}</span>
          </li>
        ))}
      </ul>
      <input data-testid="plan-form-amount" placeholder="Amount" {...field('amount')} />
      <input data-testid="plan-form-start" placeholder="Start YYYY-MM-DD" {...field('start')} />
      <input data-testid="plan-form-every" placeholder="Every N days" {...field('every')} />
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
          start, and every N days, a whole number of at least 1.
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 19, Component: PlansHeader }),
  PersonCells.fill({ order: 19, Component: PersonPlans }),
];
