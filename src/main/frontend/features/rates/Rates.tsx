import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { GRADES } from '../people/api';
import { listRates, rateByGrade, ratesKey, setRate, type RateInput } from './api';

const EMPTY: RateInput = { grade: 'A', pct: '' };

/** The sheet's rate for each grade: a form that sets one, and each grade's current rate. */
export function Rates({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const byGrade = rateByGrade(rates.data);
  const [form, setForm] = useState<RateInput>(EMPTY);

  const save = useMutation({
    mutationFn: (input: RateInput) => setRate(sheetId, user, input),
    onSuccess: () => {
      setForm((f) => ({ ...f, pct: '' }));
      return queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
  });

  const change = (patch: Partial<RateInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  return (
    <section>
      <h3>Rates</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          save.reset();
          save.mutate(form);
        }}
      >
        <label>
          Grade{' '}
          <select data-testid="rate-form-grade" value={form.grade} onChange={(e) => change({ grade: e.target.value })}>
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          Percent{' '}
          <input data-testid="rate-form-pct" value={form.pct} onChange={(e) => change({ pct: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && (
          <span data-testid="rate-form-error">A rate needs a percent of 0.00 or more with up to two decimals.</span>
        )}
      </form>
      <table>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g} data-testid={byGrade.has(g) ? `rate-row-${g}-start` : undefined}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>
                {byGrade.has(g) ? <span data-testid="rate-pct">{byGrade.get(g)}</span> : 'n/a'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
