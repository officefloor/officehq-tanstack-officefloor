import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useUser } from '../session/useUser';
import { useSheetToday } from '../sheet/useSheetToday';
import { GRADES } from '../people/peopleQuery';
import type { Grade } from '../people/peopleQuery';
import { claimsQuery } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { useFloor, useLate } from '../settings/settingsQuery';
import { rateFor, ratePreview, ratesKey, ratesQuery } from './ratesQuery';
import type { Rate } from './ratesQuery';

const PERCENT = /^\d{1,7}(\.\d{1,2})?$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** The sheet's rates — per grade, the one in force on the sheet's today, and every rate with its
 *  from-date and to-date — and the form a grade's rate is set through. */
function Rates({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const rates = useQuery(ratesQuery(sheetId));
  const [grade, setGrade] = useState<Grade>('A');
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const today = useSheetToday(sheetId);
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: { grade: Grade; rate: string; from: string; to: string }) =>
      putJson<Rate>(
        `/api/sheets/${encodeURIComponent(sheetId)}/rates/${body.grade}`,
        { rate: body.rate, from: body.from, to: body.to },
        user,
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
      setPct('');
      setFrom('');
      setTo('');
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
    const f = from.trim();
    if (f !== '' && !DATE.test(f)) {
      setError('The from-date must be a date (yyyy-mm-dd).');
      return;
    }
    const t = to.trim();
    if (t !== '' && !DATE.test(t)) {
      setError('The to-date must be a date (yyyy-mm-dd).');
      return;
    }
    if (t !== '' && f !== '' && t < f) {
      setError('The to-date must not be before the from-date.');
      return;
    }
    once(() => save.mutateAsync({ grade, rate: p, from: f, to: t }));
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
        <label>
          From{' '}
          <input
            data-testid="rate-form-from"
            type="date"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            data-testid="rate-form-to"
            type="date"
            value={to}
            onChange={(e) => {
              setTo(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {error && <p data-testid="rate-form-error">{error}</p>}
      </form>
      <RatePreview sheetId={sheetId} />
      {rates.isError && <p data-testid="rates-load-error">Could not load the rates.</p>}
      <table data-testid="rates-current">
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>{rateFor(list, g, today) ?? 'n/a'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table data-testid="rates-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate %</th>
            <th>From</th>
            <th>To</th>
          </tr>
        </thead>
        <tbody>
          {list.filter((r) => r.level === 0).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-pct">{r.rate}</td>
              <td data-testid="rate-from">{r.from ?? 'start'}</td>
              <td data-testid="rate-to">{r.to ?? ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

/** A preview of a candidate rate for a grade on every date: how many OPEN and PENDING claims of people on the grade
 *  would show a different figure, and by how much in all. Nothing is saved. */
function RatePreview({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const rates = useQuery(ratesQuery(sheetId)).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const [grade, setGrade] = useState<Grade>('A');
  const [pct, setPct] = useState('');
  const [result, setResult] = useState<{ count: number; diff: string } | null>(null);
  const [error, setError] = useState('');

  const changed = () => {
    setResult(null);
    setError('');
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    changed();
    const p = pct.trim();
    if (!PERCENT.test(p)) {
      setError('The rate must be a percent of 0.00 or more, with up to two decimals.');
      return;
    }
    setResult(ratePreview(grade, p, people, claims, rates, floor, late));
  };

  return (
    <form data-testid="rate-preview-form" onSubmit={submit}>
      <label>
        Preview grade{' '}
        <select
          data-testid="rate-preview-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value as Grade);
            changed();
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
          data-testid="rate-preview-pct"
          placeholder="0.00"
          value={pct}
          onChange={(e) => {
            setPct(e.target.value);
            changed();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="rate-preview">
        Preview
      </button>
      {result && (
        <span>
          {' '}
          Claims changed <strong data-testid="rate-preview-count">{result.count}</strong>
          {' '}· Difference <strong data-testid="rate-preview-diff">{result.diff}</strong>
        </span>
      )}
      {error && <p data-testid="rate-preview-error">{error}</p>}
    </form>
  );
}

// Keyed by sheet, so a typed rate or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 5,
  Component: ({ sheetId }) => <Rates key={sheetId} sheetId={sheetId} />,
});
