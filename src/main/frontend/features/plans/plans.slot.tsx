import { useState, type ChangeEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/people.slot';

/** A person's plan: an amount, a start date, every N days, an end date and how many claims it has made so far. */
export type Plan = { id: number; personId: number; amount: string; start: string; every: number; end: string; generated: number };

/** The key every feature reads the sheet's plans under. */
export const plansKey = (sheetId: string) => ['sheet', sheetId, 'plans'] as const;

/** The sheet's plans, in the order added. */
export function usePlans(sheetId: string) {
  return useQuery({
    queryKey: plansKey(sheetId),
    queryFn: () => getJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheetId)}/plans`),
  });
}

type PlanBody = { amount: string; start: string; every: string; end: string };

function PlansHead() {
  return <th>Plans</th>;
}

// The person's plans, each with how many claims it has made so far, and the form that adds one: an amount (by the
// claim form's rule), a start date (blank: the sheet's today), every N days (a whole number, at least 1) and an end
// date not before the start. A refusal shows on the form until it is changed or submitted again.
function PlansCell({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const plans = (usePlans(sheetId).data ?? []).filter((p) => p.personId === personId);
  const [amount, setAmount] = useState('');
  const [start, setStart] = useState('');
  const [every, setEvery] = useState('');
  const [end, setEnd] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: PlanBody) =>
      // `by` is the signed-in user the change is recorded against.
      postJson<Plan>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/plans`, { ...body, by: user ?? '' }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
      setAmount('');
      setStart('');
      setEvery('');
      setEnd('');
    },
  });
  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    add.reset();
  };

  const submit = useSubmitOnce(add);
  return (
    <td>
      <ul>
        {plans.map((p) => (
          <li key={p.id} data-testid={`plan-row-${p.id}`}>
            {Number(p.amount).toFixed(2)} from {p.start} every {p.every} days to {p.end}, made{' '}
            <span data-testid="plan-generated">{p.generated}</span>
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit({ amount, start, every, end });
        }}
      >
        <input data-testid="plan-form-amount" placeholder="Amount" inputMode="decimal" value={amount} onChange={field(setAmount)} />{' '}
        <input type="date" data-testid="plan-form-start" value={start} onChange={field(setStart)} />{' '}
        <input data-testid="plan-form-every" placeholder="Every N days" inputMode="numeric" value={every} onChange={field(setEvery)} />{' '}
        <input type="date" data-testid="plan-form-end" value={end} onChange={field(setEnd)} />{' '}
        <button type="submit" data-testid="plan-form-submit" disabled={add.isPending}>
          Add plan
        </button>
        {add.isError && (
          <span data-testid="plan-error" role="alert">
            {' '}
            {add.error.message}
          </span>
        )}
      </form>
    </td>
  );
}

type GenerateResult = { made: number };

// Generate: for every plan on the sheet, in the order added, a DRAFT claim for each plan date (the start, then every
// N days) up to its end and the sheet's today that has none yet, by the claim form's rules (a date they refuse is
// skipped and tried again next time). Shows how many were made; the count is gone while it runs.
function GeneratePlans({ sheetId }: { sheetId: string; user: string }) {
  const [made, setMade] = useState<number | null>(null);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const generate = useMutation({
    mutationFn: async () => {
      setMade(null);
      const result = await postJson<GenerateResult>(`/api/sheets/${encodeURIComponent(sheetId)}/plans/generate`, {
        by: user ?? '',
      });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: plansKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
      ]);
      setMade(result.made);
      return result;
    },
  });

  const submit = useSubmitOnce(generate);
  return (
    <section data-testid="plans-panel">
      <button type="button" data-testid="plan-generate" disabled={generate.isPending} onClick={() => submit(undefined)}>
        Generate plan claims
      </button>{' '}
      {made !== null && !generate.isError && (
        <span>
          Made <strong data-testid="plan-generated-count">{made}</strong>
        </span>
      )}
      {generate.isError && (
        <p data-testid="plan-generate-error" role="alert">
          {generate.error.message}
        </p>
      )}
    </section>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 12, Component: PlansHead }),
  PersonCells.fill({ order: 12, Component: PlansCell }),
  SheetBody.fill({ order: 17.5, Component: GeneratePlans }),
];
