import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { NA, type Rate, rateOf, ratesKey, useRates } from './rates';
import { useFigure } from '../locale/locale';
import { useGradeCodes } from '../grades/grades';

// The rate in force for each grade on the sheet's today (its parent sheets' when it has none), every own rate with the dates it applies from and
// through, and the form that sets one (a blank from-date applies from the beginning of time, a blank
// to-date has no end).
function Rates() {
  const fig = useFigure();
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const grades = useGradeCodes(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: (r: { grade: string; pct: string; from: string; to: string }) =>
      postJson<Rate>(`/api/sheets/${encodeURIComponent(sheet)}/rates`, { ...r, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheet) });
      setPct('');
      setFrom('');
      setTo('');
    },
  });
  const submit = useSubmitOnce(save);

  return (
    <section data-testid="rates">
      <h2>Rates</h2>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate % today</th>
          </tr>
        </thead>
        <tbody>
          {grades.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>{fig(rateOf(rates.data, g, today) ?? NA)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>From</th>
            <th>To</th>
            <th>Rate %</th>
            <th>Versions</th>
          </tr>
        </thead>
        <tbody>
          {(rates.data ?? []).filter((r) => (r.level ?? 0) === 0).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from ?? ''}</td>
              <td data-testid="rate-to">{r.to ?? ''}</td>
              <td data-testid="rate-pct">{fig(r.pct)}</td>
              <td data-testid="rate-version">{r.version ?? 1}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ grade, pct, from, to });
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
          {grades.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
        <input
          data-testid="rate-form-pct"
          placeholder="Rate %"
          inputMode="decimal"
          value={pct}
          onChange={(e) => {
            setPct(e.target.value);
            save.reset();
          }}
        />
        <input
          data-testid="rate-form-from"
          placeholder="From (yyyy-mm-dd)"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            save.reset();
          }}
        />
        <input
          data-testid="rate-form-to"
          placeholder="To (yyyy-mm-dd)"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            save.reset();
          }}
        />
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && (
          <span data-testid="rate-form-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The rate was refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 40, Component: Rates });
