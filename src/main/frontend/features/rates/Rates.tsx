import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { GRADES } from '../people/api';
import { useSheetToday } from '../sheet/api';
import { ImportRates } from './ImportRates';
import { listRates, previewRate, rateByGrade, ratesKey, setRate, type RateInput } from './api';

const EMPTY: RateInput = { grade: 'A', pct: '', from: '', to: '' };

/** The sheet's rates: a form that sets one (from a from-date, or from the beginning of time,
 *  through a to-date or without end),
 *  each grade's rate in force on the sheet's today, and every rate by grade and from-date. */
export function Rates({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const byGrade = rateByGrade(rates.data, useSheetToday(sheetId, user));
  const [form, setForm] = useState<RateInput>(EMPTY);

  const save = useMutation({
    mutationFn: (input: RateInput) => setRate(sheetId, user, input),
    onSuccess: () => {
      setForm((f) => ({ ...f, pct: '', from: '', to: '' }));
      return queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();

  const [candidate, setCandidate] = useState({ grade: 'A', pct: '' });
  const preview = useMutation({ mutationFn: (c: typeof candidate) => previewRate(sheetId, c.grade, c.pct) });
  const changeCandidate = (patch: Partial<typeof candidate>) => {
    setCandidate((c) => ({ ...c, ...patch }));
    preview.reset();
  };

  const change = (patch: Partial<RateInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  return (
    <section>
      <h3>Rates</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(form);
          });
        }}
      >
        <label>
          Grade{' '}
          <select data-testid="rate-form-grade" value={form.grade} onChange={(e) => change({ grade: e.target.value })}>
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          Percent{' '}
          <input data-testid="rate-form-pct" value={form.pct} onChange={(e) => change({ pct: e.target.value })} />
        </label>{' '}
        <label>
          From{' '}
          <input type="date" data-testid="rate-form-from" value={form.from}
            onChange={(e) => change({ from: e.target.value })} />
        </label>{' '}
        <label>
          To{' '}
          <input type="date" data-testid="rate-form-to" value={form.to}
            onChange={(e) => change({ to: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && (
          <span data-testid="rate-form-error">A rate needs a percent of 0.00 or more with up to two decimals, and a from-date and to-date that are dates or blank, the to-date not before the from-date.</span>
        )}
      </form>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          preview.reset();
          preview.mutate(candidate);
        }}
      >
        <label>
          Preview grade{' '}
          <select data-testid="rate-preview-grade" value={candidate.grade}
            onChange={(e) => changeCandidate({ grade: e.target.value })}>
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          Candidate percent{' '}
          <input data-testid="rate-preview-pct" value={candidate.pct}
            onChange={(e) => changeCandidate({ pct: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="rate-preview" disabled={preview.isPending}>
          Preview
        </button>
        {preview.data && (
          <span>
            {' '}<span data-testid="rate-preview-count">{preview.data.count}</span> claims would change by{' '}
            <span data-testid="rate-preview-diff">{preview.data.diff}</span>
          </span>
        )}
        {preview.isError && (
          <span data-testid="rate-preview-error">A preview needs a percent of 0.00 or more with up to two decimals.</span>
        )}
      </form>
      <ImportRates sheetId={sheetId} user={user} />
      <table>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>
                {byGrade.has(g) ? <span data-testid="rate-pct">{byGrade.get(g)}</span> : 'n/a'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <table>
        <tbody>
          {(rates.data ?? []).filter((r) => r.level === 0).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td>{r.from === null ? 'From the start' : `From ${r.from}`}</td>
              <td>
                {r.to !== null && 'to '}<span data-testid="rate-to">{r.to ?? ''}</span>
              </td>
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
