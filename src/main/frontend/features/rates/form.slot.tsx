import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES } from '../people/people';
import { rateOf, ratesPath, ratesQueryKey, useRates, type Rate } from './rates';

// The sheet's rate for each grade (n/a for a grade with none), and the form that sets one.
function Rates() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const rates = useRates();
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [pct, setPct] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { grade: string; pct: string }) => putJson<Rate>(ratesPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesQueryKey(sheet ?? '') });
      setPct('');
    },
  });

  if (!user || !sheet) return null;
  return (
    <section data-testid="rates">
      <h2>Rates</h2>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate (%)</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const shown = rateOf(rates.data, g) ?? 'n/a';
            return (
              <tr key={g} data-testid={`rate-row-${g}-start`}>
                <td>{g}</td>
                <td data-testid="rate-pct">
                  <span data-testid={`rate-current-${g}`}>{shown}</span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          save.reset();
          save.mutate({ grade, pct });
        }}
      >
        <select
          data-testid="rate-form-grade"
          value={grade}
          onChange={(e) => { setGrade(e.target.value); save.reset(); }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>
        <input
          data-testid="rate-form-pct"
          placeholder="Percent"
          inputMode="decimal"
          value={pct}
          onChange={(e) => { setPct(e.target.value); save.reset(); }}
        />
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && (
          <span data-testid="rate-form-error">The rate must be a percent, 0.00 or more, with up to two decimals.</span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 5, Component: Rates });
