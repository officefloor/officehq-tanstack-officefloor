import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { GRADES } from '../people/usePeople';
import { useSheetToday } from '../sheet/useSheet';
import { PCT, rateOn, ratesKey, useRates, type Rate } from './useRates';

/**
 * The rate in force on the sheet's today for each grade, every rate set (by grade, then from-date),
 * and one form to set a grade's rate, from a date or (left blank) from the beginning of time.
 */
function RatesPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const [invalid, setInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (r: { grade: string; pct: string; from: string }) =>
      postJson<Rate>(`/api/sheets/${encodeURIComponent(sheet)}/rates`, { ...r, user }),
    onSuccess: async () => {
      setPct('');
      setFrom('');
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
          submitOnce(() => save.mutateAsync({ grade, pct: pct.trim(), from: from.trim() }));
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
        <label>
          From{' '}
          <input
            data-testid="rate-form-from"
            placeholder="yyyy-mm-dd"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
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
      <table data-testid="rate-current">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate % today</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>{rateOn(rates, g, today) ?? 'n/a'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table data-testid="rate-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>From</th>
            <th>Rate %</th>
          </tr>
        </thead>
        <tbody>
          {rates.map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from ?? 'start'}</td>
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

export const contribution = SheetPanels.fill({ order: 5, Component: RatesPanel });
