import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { GRADES } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useSheetToday } from '../session/sheetToday';
import { NA, PERCENT, rateFor, ratesKey, useRates } from './rates';
import type { Rate, RateInput } from './rates';
import { useOneAtATime } from '../../query/useOneAtATime';
import { RatePreview } from './RatePreview';

const EMPTY: RateInput = { grade: 'A', rate: '', from: '', to: '' };

/** yyyy-MM-dd. */
const DATE = /^\d{4}-\d{2}-\d{2}$/;

// Each grade's rate in force on the sheet's today (n/a when none is), every rate the sheet has with
// its from- and to-date, and the form that sets one. The rate in force may be inherited from a parent sheet;
// the list shows only the sheet's own.
export function Rates() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const rates = useRates().data ?? [];
  const today = useSheetToday();
  const [form, setForm] = useState<RateInput>(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const save = useMutation({
    mutationFn: (input: RateInput) =>
      putJson<Rate>(`/api/sheets/${encodeURIComponent(sheet)}/rates?user=${encodeURIComponent(user)}`, input),
    onSuccess: async () => {
      setForm((f) => ({ ...f, rate: '', from: '', to: '' }));
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
    const from = form.from.trim();
    if (from !== '' && !DATE.test(from)) {
      setError('Type a from-date as yyyy-mm-dd, or leave it blank for from the start.');
      return;
    }
    const to = form.to.trim();
    if (to !== '' && !DATE.test(to)) {
      setError('Type a to-date as yyyy-mm-dd, or leave it blank for no end.');
      return;
    }
    if (from !== '' && to !== '' && to < from) {
      setError('The to-date must not be before the from-date.');
      return;
    }
    once(() => save.mutateAsync({ grade: form.grade, rate: form.rate.trim(), from, to }));
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
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>{rateFor(rates, g, today) ?? NA}</td>
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
          </tr>
        </thead>
        <tbody>
          {rates.filter((r) => r.sheet === sheet).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from ?? 'start'}</td>
              <td data-testid="rate-to">{r.to ?? ''}</td>
              <td data-testid="rate-pct">{r.rate}</td>
            </tr>
          ))}
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
        <input
          data-testid="rate-form-from"
          placeholder="From yyyy-mm-dd (blank: from the start)"
          value={form.from}
          onChange={(e) => change({ from: e.target.value })}
        />
        <input
          data-testid="rate-form-to"
          placeholder="To yyyy-mm-dd (blank: no end)"
          value={form.to}
          onChange={(e) => change({ to: e.target.value })}
        />
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {error && <span data-testid="rate-form-error">{error}</span>}
      </form>
      <RatePreview />
    </section>
  );
}
