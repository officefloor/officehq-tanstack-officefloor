import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { GRADES } from '../people/usePeople';
import { NA } from './due';
import { ratesKey, useRates, type Rate } from './useRates';

// The sheet's rate for each grade (n/a where none is set), and the form that sets one.
export function RatePanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const rates = useRates(sheet);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { grade: string; rate: string }) =>
      postJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheet)}/rates`, { ...body, user }),
    onSuccess: (saved) => {
      setPct('');
      queryClient.setQueryData(ratesKey(sheet), saved);
    },
  });
  return (
    <>
      <table data-testid="rates">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate %</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g} data-testid={rates.has(g) ? `rate-row-${g}-start` : undefined}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>
                {rates.has(g) ? <span data-testid="rate-pct">{rates.get(g)}</span> : NA}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          save.reset();
          save.mutate({ grade, rate: pct.trim() });
        }}
      >
        <select
          data-testid="rate-form-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            save.reset();
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>{' '}
        <input
          data-testid="rate-form-pct"
          value={pct}
          placeholder="Rate %"
          onChange={(e) => {
            setPct(e.target.value);
            save.reset();
          }}
        />{' '}
        <button type="submit" data-testid="rate-form-submit">Set rate</button>
        {save.isError && <span data-testid="rate-form-error" role="alert"> Could not set the rate: it must be a percent of 0.00 or more with at most two decimals.</span>}
      </form>
    </>
  );
}
