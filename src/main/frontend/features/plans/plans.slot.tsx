import { type ChangeEvent, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction } from '../people/people';
import { claimsKey } from '../claims/claims';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Plan, plansKey, usePlans } from './plans';
import { useFigure } from '../locale/locale';

type Fields = { amount: string; start: string; every: string; day: string; end: string; sheet: string };
const EMPTY: Fields = { amount: '', start: '', every: '', day: '', end: '', sheet: '' };

function PlansHead() {
  return <th>Plans</th>;
}

// A plan, with how many claims it has made so far, its next five dates, whether it is paused and the buttons that pause
// and resume it (on the sheet's today), the entry that changes its amount (from the sheet's today on) and its amount
// changes, oldest first. A refused pause, resume or amount change shows its error on the plan until the amount is edited
// or the next action on the person's row is taken.
function PlanRow({ plan: p, latest }: { plan: Plan; latest: number }) {
  const fig = useFigure();
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  const pause = useMutation({
    mutationKey: personActionKey(sheet, p.personId),
    mutationFn: (on: boolean) =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheet)}/plans/${p.id}/pause`, { on, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: plansKey(sheet) });
    },
  });
  const submit = useSubmitOnce(pause);

  const [amount, setAmount] = useState('');
  const change = useMutation({
    mutationKey: personActionKey(sheet, p.personId),
    mutationFn: (a: string) =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheet)}/plans/${p.id}/amount`, { amount: a, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: plansKey(sheet) });
      setAmount('');
    },
  });
  const submitChange = useSubmitOnce(change);

  // Both share the person's action key, so at most one of them is the latest action.
  const refused = [pause, change].find((m) => m.isError && m.submittedAt >= latest);

  return (
    <li data-testid={`plan-row-${p.id}`}>
      <span data-testid="plan-amount-shown">{fig(p.amount)}</span> from {p.start}{' '}
      {p.day !== null ? `monthly on day ${p.day}` : `every ${p.every} days`} to {p.end}
      {' — made '}
      <span data-testid="plan-generated">{p.generated}</span>
      {' — next '}
      <span data-testid="plan-next-dates">{p.nextDates}</span>
      {' — paused '}
      <span data-testid="plan-paused">{p.paused ? 'yes' : 'no'}</span>{' '}
      <button type="button" data-testid="plan-pause" disabled={pause.isPending} onClick={() => submit(true)}>
        Pause
      </button>
      <button type="button" data-testid="plan-resume" disabled={pause.isPending} onClick={() => submit(false)}>
        Resume
      </button>
      <input
        data-testid="plan-amount"
        placeholder="New amount"
        inputMode="decimal"
        value={amount}
        onChange={(e) => {
          setAmount(e.target.value);
          change.reset();
        }}
      />
      <button type="button" data-testid="plan-amount-save" disabled={change.isPending} onClick={() => submitChange(amount)}>
        Save amount
      </button>
      {refused && (
        <span data-testid="plan-error" role="alert">
          {' '}
          {refused.error instanceof HttpError && refused.error.reason ? `${refused.error.reason}.` : 'The plan change was refused.'}
        </span>
      )}
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

// The person's plans, each with how many claims it has made so far and its next five dates, and the form that adds one. A bad
// amount, interval or day of the month, an end before the start, or a sheet that is not a descendant, is refused with an error on the row until the form is
// changed or the next action on the row is taken.
function PersonPlans({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const plans = usePlans(sheet);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (f: Fields) =>
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/plans`, { ...f, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: plansKey(sheet) });
      setFields(EMPTY);
    },
  });
  const submit = useSubmitOnce(add);
  const latest = useLatestPersonAction(sheet, person.id);
  const set = (key: keyof Fields) => (e: ChangeEvent<HTMLInputElement>) => {
    setFields({ ...fields, [key]: e.target.value });
    add.reset();
  };

  return (
    <td>
      <ul>
        {(plans.data ?? [])
          .filter((p) => p.personId === person.id)
          .map((p) => (
            <PlanRow key={p.id} plan={p} latest={latest} />
          ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit(fields);
        }}
      >
        <input data-testid="plan-form-amount" placeholder="Amount" inputMode="decimal" value={fields.amount} onChange={set('amount')} />
        <input data-testid="plan-form-start" placeholder="Start (yyyy-mm-dd)" value={fields.start} onChange={set('start')} />
        <input data-testid="plan-form-every" placeholder="Every N days" inputMode="numeric" value={fields.every} onChange={set('every')} />
        <input data-testid="plan-form-day" placeholder="Or day of month (1-31)" inputMode="numeric" value={fields.day} onChange={set('day')} />
        <input data-testid="plan-form-end" placeholder="End (yyyy-mm-dd)" value={fields.end} onChange={set('end')} />
        <input data-testid="plan-form-sheet" placeholder="On descendant sheet (optional)" value={fields.sheet} onChange={set('sheet')} />
        <button type="submit" data-testid="plan-form-submit" disabled={add.isPending}>
          Add plan
        </button>
        {add.isError && add.submittedAt >= latest && (
          <span data-testid="plan-error" role="alert">
            {' '}
            {add.error instanceof HttpError && add.error.reason ? `${add.error.reason}.` : 'The plan was refused.'}
          </span>
        )}
      </form>
    </td>
  );
}

type GenerateResult = { generated: number };

// Makes the plans' DRAFT claims due up to the sheet's today; the count made shows once the server answers
// (gone while it runs).
function GeneratePlans() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  const generate = useMutation({
    mutationFn: () => postJson<GenerateResult>(`/api/sheets/${encodeURIComponent(sheet)}/plans/generate`, { user }),
    // Not awaited: the count made shows as soon as the server answers, no later than the rows it added.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      void queryClient.invalidateQueries({ queryKey: plansKey(sheet) });
    },
  });
  const submit = useSubmitOnce(generate);

  return (
    <section>
      <h2>Plans</h2>
      <button type="button" data-testid="plan-generate" disabled={generate.isPending} onClick={() => submit(undefined)}>
        Generate plan claims
      </button>
      {generate.isSuccess && (
        <>
          {' '}
          Made <strong data-testid="plan-generated-count">{generate.data.generated}</strong>
        </>
      )}
      {generate.isError && (
        <span data-testid="plan-generate-error" role="alert">
          {' '}
          {generate.error instanceof HttpError && generate.error.reason ? `${generate.error.reason}.` : 'The generate was refused.'}
        </span>
      )}
    </section>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.97, Component: PlansHead }),
  PersonColumns.fill({ order: 19.97, Component: PersonPlans }),
  HomeMain.fill({ order: 16.5, Component: GeneratePlans }),
];
