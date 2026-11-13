import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { GRADES } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { NA, PERCENT, rateFor, ratesKey, useRates } from './rates';
import type { Rate, RateInput } from './rates';

const EMPTY: RateInput = { grade: 'A', rate: '' };

// Each grade's current rate (n/a when it has none) and the form that sets one.
export function Rates() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const rates = useRates().data ?? [];
  const [form, setForm] = useState<RateInput>(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: (input: RateInput) =>
      putJson<Rate>(`/api/sheets/${encodeURIComponent(sheet)}/rates?user=${encodeURIComponent(user)}`, input),
    onSuccess: async () => {
      setForm((f) => ({ ...f, rate: '' }));
      setError('');
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheet) });
    },
    onError: () => setError('That rate could not be set.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!PERCENT.test(form.rate.trim())) {
      setError('Type a percent of 0.00 or more, with at most two decimals.');
      return;
    }
    save.mutate({ grade: form.grade, rate: form.rate.trim() });
  };

  const change = (patch: Partial<RateInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  return (
    <section data-testid="rates">
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate %</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const rate = rateFor(rates, g);
            return (
              <tr key={g} data-testid={rate === undefined ? undefined : `rate-row-${g}-start`}>
                <td>{g}</td>
                <td data-testid={`rate-current-${g}`}>
                  {rate === undefined ? NA : <span data-testid="rate-pct">{rate}</span>}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <form onSubmit={submit} data-testid="rate-form">
        <select data-testid="rate-form-grade" value={form.grade} onChange={(e) => change({ grade: e.target.value })}>
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
        <input
          data-testid="rate-form-pct"
          placeholder="Rate %"
          value={form.rate}
          onChange={(e) => change({ rate: e.target.value })}
        />
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {error && <span data-testid="rate-form-error">{error}</span>}
      </form>
    </section>
  );
}
