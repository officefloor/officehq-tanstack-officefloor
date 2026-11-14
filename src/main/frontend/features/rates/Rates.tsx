import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { GRADES } from '../people/api';
import { useSheetToday } from '../sheet/api';
import { listRates, rateByGrade, ratesKey, setRate, type RateInput } from './api';

const EMPTY: RateInput = { grade: 'A', pct: '', from: '' };

/** The sheet's rates: a form that sets one (from a from-date, or from the beginning of time),
 *  each grade's rate in force on the sheet's today, and every rate by grade and from-date. */
export function Rates({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const byGrade = rateByGrade(rates.data, useSheetToday(sheetId, user));
  const [form, setForm] = useState<RateInput>(EMPTY);

  const save = useMutation({
    mutationFn: (input: RateInput) => setRate(sheetId, user, input),
    onSuccess: () => {
      setForm((f) => ({ ...f, pct: '', from: '' }));
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
        <label>
          From{' '}
          <input type="date" data-testid="rate-form-from" value={form.from}
            onChange={(e) => change({ from: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && (
          <span data-testid="rate-form-error">A rate needs a percent of 0.00 or more with up to two decimals, and a from-date that is a date or blank.</span>
        )}
      </form>
      <table>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>
                {byGrade.has(g) ? <span data-testid="rate-pct">{byGrade.get(g)}</span> : 'n/a'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <table>
        <tbody>
          {(rates.data ?? []).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td>{r.from === null ? 'From the start' : `From ${r.from}`}</td>
              <td>
                <span data-testid="rate-pct">{r.pct}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
