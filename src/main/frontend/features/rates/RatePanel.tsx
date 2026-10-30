import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { GRADES } from '../people/usePeople';
import { useSheetToday } from '../sheet/useSheetToday';
import { NA } from './due';
import { ImportRates } from './ImportRates';
import { RatePreview } from './RatePreview';
import { rateOn, ratesKey, useRates, type Rate } from './useRates';

// The rate in force on the sheet's today for each grade (n/a where none applies), every rate set with
// the dates it applies from and through, and the form that sets one (a blank from-date: from the
// beginning of time; a blank to-date: no end; one already there from the same date is kept as an older
// version and the row shows its version count), and a preview of a candidate rate for a grade.
export function RatePanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const rates = useRates(sheet);
  const [grade, setGrade] = useState<string>('A');
  const today = useSheetToday(sheet);
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: { grade: string; rate: string; from: string; to: string }) =>
      postJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheet)}/rates`, { ...body, user }),
    onSuccess: (saved) => {
      setPct('');
      setFrom('');
      setTo('');
      queryClient.setQueryData(ratesKey(sheet), saved);
    },
  });
  return (
    <>
      <table data-testid="rates">
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
              <td data-testid={`rate-current-${g}`}>{rateOn(rates, g, today) ?? NA}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table data-testid="rate-list">
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
          {rates.filter((r) => r.level === undefined).map((r) => (
            <tr key={`${r.grade}-${r.from}`} data-testid={`rate-row-${r.grade}-${r.from || 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from || 'start'}</td>
              <td data-testid="rate-to">{r.to}</td>
              <td data-testid="rate-pct">{r.rate}</td>
              <td data-testid="rate-version">{r.version}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ grade, rate: pct.trim(), from: from.trim(), to: to.trim() });
          });
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
        <input
          data-testid="rate-form-from"
          value={from}
          placeholder="From (YYYY-MM-DD, blank: always)"
          onChange={(e) => {
            setFrom(e.target.value);
            save.reset();
          }}
        />{' '}
        <input
          data-testid="rate-form-to"
          value={to}
          placeholder="To (YYYY-MM-DD, blank: no end)"
          onChange={(e) => {
            setTo(e.target.value);
            save.reset();
          }}
        />{' '}
        <button type="submit" data-testid="rate-form-submit">Set rate</button>
        {save.isError && <span data-testid="rate-form-error" role="alert"> Could not set the rate: it must be a percent of 0.00 or more with at most two decimals, from a valid date (YYYY-MM-DD) or none, to a valid date not before it or none.</span>}
      </form>
      <ImportRates />
      <RatePreview />
    </>
  );
}
