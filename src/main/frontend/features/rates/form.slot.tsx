import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { GRADES } from '../people/people';
import { rateOn, ratesPath, ratesQueryKey, useRates, type Rate } from './rates';

// The sheet's rates: each grade's rate in force on the sheet's today (n/a for a grade with none), every
// rate with its from-date (a grade's rate from the start shows n/a when it has none), and the form that
// sets one — replacing the grade's rate with the same from-date.
function Rates() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const rates = useRates();
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { grade: string; pct: string; from: string }) =>
      putJson<Rate>(ratesPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesQueryKey(sheet ?? '') });
      setPct('');
      setFrom('');
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
            <th>From</th>
            <th>Rate (%)</th>
            <th>In force today (%)</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.flatMap((g) => {
            const mine = (rates.data ?? []).filter((r) => r.grade === g);
            const start = mine.find((r) => r.from === null);
            const dated = mine.filter((r) => r.from !== null);
            return [
              <tr key={`${g}-start`} data-testid={`rate-row-${g}-start`}>
                <td>{g}</td>
                <td>start</td>
                <td data-testid="rate-pct">{start?.pct ?? 'n/a'}</td>
                <td rowSpan={dated.length + 1} data-testid={`rate-current-${g}`}>
                  {rateOn(rates.data, g, today) ?? 'n/a'}
                </td>
              </tr>,
              ...dated.map((r) => (
                <tr key={`${g}-${r.from}`} data-testid={`rate-row-${g}-${r.from}`}>
                  <td>{g}</td>
                  <td data-testid="rate-from">{r.from}</td>
                  <td data-testid="rate-pct">{r.pct}</td>
                </tr>
              )),
            ];
          })}
        </tbody>
      </table>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          save.reset();
          save.mutate({ grade, pct, from });
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
        <input
          data-testid="rate-form-from"
          placeholder="From (YYYY-MM-DD, blank: start)"
          value={from}
          onChange={(e) => { setFrom(e.target.value); save.reset(); }}
        />
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && (
          <span data-testid="rate-form-error">The rate must be a percent, 0.00 or more, with up to two decimals, and the from-date a calendar date or blank.</span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 5, Component: Rates });
