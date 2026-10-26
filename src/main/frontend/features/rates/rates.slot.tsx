import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { GRADES } from '../people/peopleQuery';
import type { Grade } from '../people/peopleQuery';
import { rateFor, ratesKey, ratesQuery } from './ratesQuery';
import type { Rate } from './ratesQuery';

const PERCENT = /^\d{1,7}(\.\d{1,2})?$/;

/** The sheet's rate for each grade, and the form a grade's rate is set through. */
function Rates({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const rates = useQuery(ratesQuery(sheetId));
  const [grade, setGrade] = useState<Grade>('A');
  const [pct, setPct] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const save = useMutation({
    mutationFn: (body: { grade: Grade; rate: string }) =>
      putJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates/${body.grade}`, { rate: body.rate }, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
      setPct('');
    },
    onError: () => setError('The rate could not be set.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const p = pct.trim();
    if (!PERCENT.test(p)) {
      setError('The rate must be a percent of 0.00 or more, with up to two decimals.');
      return;
    }
    save.mutate({ grade, rate: p });
  };

  const list = rates.data ?? [];
  return (
    <section data-testid="rates">
      <h3>Rates</h3>
      <form data-testid="rate-form" onSubmit={submit}>
        <label>
          Grade{' '}
          <select
            data-testid="rate-form-grade"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value as Grade);
              setError('');
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
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {error && <p data-testid="rate-form-error">{error}</p>}
      </form>
      {rates.isError && <p data-testid="rates-load-error">Could not load the rates.</p>}
      <table>
        <tbody>
          {GRADES.map((g) => {
            const rate = rateFor(list, g);
            return (
              <tr key={g} data-testid={rate === undefined ? undefined : `rate-row-${g}-start`}>
                <td>{g}</td>
                <td data-testid={`rate-current-${g}`}>
                  <span data-testid="rate-pct">{rate ?? 'n/a'}</span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

// Keyed by sheet, so a typed rate or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 5,
  Component: ({ sheetId }) => <Rates key={sheetId} sheetId={sheetId} />,
});
