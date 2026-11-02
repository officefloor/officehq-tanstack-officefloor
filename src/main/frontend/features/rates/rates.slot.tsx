import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { GRADES } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import type { Grade } from '../people/people';
import { rateOf, ratePreviewPath, ratesKey, ratesPath, useRates } from './rates';
import type { Rate, RatePreview } from './rates';
import { Fig } from '../locale/Fig';

type Draft = { grade: Grade; pct: string; from: string; to: string };
const EMPTY: Draft = { grade: 'A', pct: '', from: '', to: '' };

// The sheet's rates: each grade's rate in force on the sheet's today (n/a when none), a calendar of each grade's rate
// on the first day of each month of today's calendar year (n/a when none), every rate with its
// from-date (a blank from-date applies from the start) and to-date (a blank one has no end), and a form that sets one, replacing the rate already
// there for that grade and from-date as a new version (each row shows its version count; the newest is in force).
function Rates({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  // The rate calendar: each month (yyyy-MM) of the sheet's today's calendar year, read on its first day.
  const year = today?.slice(0, 4);
  const months = year ? Array.from({ length: 12 }, (_, i) => `${year}-${String(i + 1).padStart(2, '0')}`) : [];

  const save = useMutation({
    mutationFn: (body: Draft) => postJson<Rate[]>(byUser(ratesPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(ratesKey(sheet), updated);
      setDraft((d) => ({ ...d, pct: '', from: '', to: '' }));
    },
  });
  const send = useSubmitOnce(save);

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a percent that is not 0.00 or more with up to two decimals, a bad from- or to-date, or a to-date before the from-date.
    send(draft);
  };

  // The preview of a candidate rate: read-only, it saves nothing and audits nothing.
  const [previewGrade, setPreviewGrade] = useState<Grade>('A');
  const [previewPct, setPreviewPct] = useState('');
  const preview = useMutation({
    mutationFn: ({ grade, pct }: { grade: Grade; pct: string }) =>
      getJson<RatePreview>(ratePreviewPath(sheet, grade, pct)),
  });
  const runPreview = () => {
    preview.reset();
    preview.mutate({ grade: previewGrade, pct: previewPct });
  };

  return (
    <section>
      <h2>Rates</h2>
      <form onSubmit={submit}>
        <label>
          Grade{' '}
          <select
            data-testid="rate-form-grade"
            value={draft.grade}
            onChange={(e) => change({ grade: e.target.value as Grade })}
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
          <input data-testid="rate-form-pct" value={draft.pct} onChange={(e) => change({ pct: e.target.value })} />
        </label>{' '}
        <label>
          From{' '}
          <input
            data-testid="rate-form-from"
            placeholder="yyyy-mm-dd (blank: from the start)"
            value={draft.from}
            onChange={(e) => change({ from: e.target.value })}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            data-testid="rate-form-to"
            placeholder="yyyy-mm-dd (blank: no end)"
            value={draft.to}
            onChange={(e) => change({ to: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && (
          <span data-testid="rate-form-error" role="alert">
            {' '}A rate needs a grade of A, B or C and a percent of 0.00 or more with up to two decimals, a from-date and a to-date that are each a valid date or blank, and a to-date not before the from-date.
          </span>
        )}
      </form>
      <div>
        <label>
          Preview grade{' '}
          <select
            data-testid="rate-preview-grade"
            value={previewGrade}
            onChange={(e) => {
              setPreviewGrade(e.target.value as Grade);
              preview.reset();
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
          Candidate rate %{' '}
          <input
            data-testid="rate-preview-pct"
            value={previewPct}
            onChange={(e) => {
              setPreviewPct(e.target.value);
              preview.reset();
            }}
          />
        </label>{' '}
        <button type="button" data-testid="rate-preview" disabled={preview.isPending} onClick={runPreview}>
          Preview
        </button>
        {preview.data && (
          <span>
            {' '}Claims changed: <span data-testid="rate-preview-count">{preview.data.count}</span>, difference:{' '}
            <span data-testid="rate-preview-diff"><Fig>{preview.data.diff}</Fig></span>
          </span>
        )}
        {preview.isError && (
          <span data-testid="rate-preview-error" role="alert">
            {' '}A preview needs a grade of A, B or C and a percent of 0.00 or more with up to two decimals.
          </span>
        )}
      </div>
      <table>
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
              <td data-testid={`rate-current-${g}`}><Fig>{rateOf(rates, g, today) ?? 'n/a'}</Fig></td>
            </tr>
          ))}
        </tbody>
      </table>
      {months.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Grade</th>
              {months.map((m) => (
                <th key={m}>{m}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {GRADES.map((g) => (
              <tr key={g}>
                <td>{g}</td>
                {months.map((m) => (
                  <td key={m} data-testid={`rate-month-${g}-${m}`}><Fig>{rateOf(rates, g, `${m}-01`) ?? 'n/a'}</Fig></td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      )}
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
          {rates.filter((r) => (r.level ?? 0) === 0).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from ?? 'start'}</td>
              <td data-testid="rate-to">{r.to ?? ''}</td>
              <td>
                <span data-testid="rate-pct"><Fig>{r.pct}</Fig></span>
              </td>
              <td data-testid="rate-version">{r.version ?? 1}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 5, Component: Rates });
