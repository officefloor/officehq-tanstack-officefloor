import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { GRADES } from '../people/usePeople';
import { PCT, rateByGrade, ratesKey, useRates, type Rate } from './useRates';

/** The sheet's rate for each grade, and one form to set a grade's rate. */
function RatesPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: rates = [] } = useRates(sheet);
  const current = rateByGrade(rates);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [invalid, setInvalid] = useState(false);

  const save = useMutation({
    mutationFn: (r: { grade: string; pct: string }) =>
      postJson<Rate>(`/api/sheets/${encodeURIComponent(sheet)}/rates`, { ...r, user }),
    onSuccess: async () => {
      setPct('');
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheet) });
    },
  });

  // An error goes as soon as its form is changed or submitted again.
  const clearError = () => {
    save.reset();
    setInvalid(false);
  };

  return (
    <section data-testid="rates-panel">
      <h2>Rates</h2>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!PCT.test(pct.trim())) {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            save.reset();
            setInvalid(true);
            return;
          }
          setInvalid(false);
          save.mutate({ grade, pct: pct.trim() });
        }}
      >
        <label>
          Grade{' '}
          <select
            data-testid="rate-form-grade"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              clearError();
            }}
          >
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          Rate %{' '}
          <input
            data-testid="rate-form-pct"
            placeholder="0.00"
            value={pct}
            onChange={(e) => {
              setPct(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {(invalid || save.isError) && (
          <span data-testid="rate-form-error" role="alert">
            {' '}
            {invalid
              ? 'The rate must be a percent, 0.00 or more, with up to two decimals.'
              : 'Could not set this rate.'}
          </span>
        )}
      </form>
      <table data-testid="rate-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate %</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g} data-testid={current.has(g) ? `rate-row-${g}-start` : undefined}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>
                {current.has(g) ? <span data-testid="rate-pct">{current.get(g)}</span> : 'n/a'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 5, Component: RatesPanel });
