import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { addPlan, listPlans, plansKey, type PlanInput } from './api';

const EMPTY: PlanInput = { amount: '', start: '', every: '', end: '' };

/** A person's plans (each its amount, start, every N days, end and how many claims it has made so far)
 *  and a form that adds one; a refused add shows an error on the row and adds nothing. */
export function PersonPlans({ sheetId, user, personId }: { sheetId: string; user: string; personId: number }) {
  const queryClient = useQueryClient();
  const plans = useQuery({ queryKey: plansKey(sheetId), queryFn: () => listPlans(sheetId) });
  const [form, setForm] = useState<PlanInput>(EMPTY);
  const submitOnce = useSubmitOnce();

  const add = useMutation({
    mutationFn: (input: PlanInput) => addPlan(sheetId, user, personId, input),
    onSuccess: () => {
      setForm(EMPTY);
      return queryClient.invalidateQueries({ queryKey: plansKey(sheetId) });
    },
  });

  const change = (patch: Partial<PlanInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    add.reset();
  };

  return (
    <div>
      Plans
      <table>
        <tbody>
          {(plans.data ?? []).filter((pl) => pl.personId === personId).map((pl) => (
            <tr key={pl.id} data-testid={`plan-row-${pl.id}`}>
              <td data-testid="plan-amount-shown">{Number(pl.amount).toFixed(2)}</td>
              <td data-testid="plan-start">{pl.start}</td>
              <td>every <span data-testid="plan-every">{pl.every}</span> days</td>
              <td>to <span data-testid="plan-end">{pl.end}</span></td>
              <td>made <span data-testid="plan-generated">{pl.generated}</span></td>
            </tr>
          ))}
        </tbody>
      </table>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = form;
          submitOnce(() => {
            add.reset();
            return add.mutateAsync(input);
          });
        }}
      >
        <input data-testid="plan-form-amount" placeholder="Amount" value={form.amount}
          onChange={(e) => change({ amount: e.target.value })} />{' '}
        <input type="date" data-testid="plan-form-start" value={form.start}
          onChange={(e) => change({ start: e.target.value })} />{' '}
        <input data-testid="plan-form-every" placeholder="Every N days" value={form.every}
          onChange={(e) => change({ every: e.target.value })} />{' '}
        <input type="date" data-testid="plan-form-end" value={form.end}
          onChange={(e) => change({ end: e.target.value })} />{' '}
        <button type="submit" data-testid="plan-form-submit" disabled={add.isPending}>
          Add plan
        </button>
        {add.isError && (
          <span data-testid="plan-error">A plan needs an amount of 0.00 or more with up to two decimals, a start, every a whole number of days (at least 1) and an end not before the start.</span>
        )}
      </form>
    </div>
  );
}
