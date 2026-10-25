import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { GRADES } from '../people/people';
import { rateOn, rateWithdrawPath, ratesPath, ratesPreviewPath, ratesQueryKey, useRates, type Rate } from './rates';
import { useGradeCodes } from '../grades/grades';

// The sheet's rates: each grade's rate in force on the sheet's today (its parent's, up to three levels up, when it
// has none in force; n/a for a grade with none), every
// rate with its from-date and to-date and its version count (a grade's rate from the start shows n/a when it has none), and the form that
// sets one — a new version of the grade's rate with the same from-date, when it has one. A steward withdraws a rate's
// newest version from its row (the one before it applies again; with none the rate is gone); a refusal shows the
// rate form's error.
function Rates() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const gradeCodes = useGradeCodes();
  const rates = useRates();
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { grade: string; pct: string; from: string; to: string }) =>
      putJson<Rate>(ratesPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesQueryKey(sheet ?? '') });
      setPct('');
      setFrom('');
      setTo('');
    },
  });
  const submit = useSubmitOnce(save);
  const withdraw = useMutation({
    mutationFn: (body: { grade: string; from: string }) =>
      postJson<Rate[]>(rateWithdrawPath(sheet ?? ''), body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ratesQueryKey(sheet ?? '') }),
  });
  const withdrawOnce = useSubmitOnce(withdraw);
  const takeWithdraw = (g: string, from: string | null) => {
    save.reset();
    withdrawOnce({ grade: g, from: from ?? '' });
  };
  const [previewGrade, setPreviewGrade] = useState<string>(GRADES[0]);
  const [previewPct, setPreviewPct] = useState('');
  // Nothing is saved: the store works out what the candidate rate would change and the screen shows it.
  const preview = useMutation({
    mutationFn: (q: { grade: string; pct: string }) =>
      getJson<{ count: number; diff: string }>(
        `${ratesPreviewPath(sheet ?? '')}?grade=${encodeURIComponent(q.grade)}&pct=${encodeURIComponent(q.pct)}`),
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
            <th>To</th>
            <th>Rate (%)</th>
            <th>Versions</th>
            <th></th>
            <th>In force today (%)</th>
          </tr>
        </thead>
        <tbody>
          {gradeCodes.flatMap((g) => {
            const mine = (rates.data ?? []).filter((r) => r.sheet === sheet && r.grade === g);
            const start = mine.find((r) => r.from === null);
            const dated = mine.filter((r) => r.from !== null);
            return [
              <tr key={`${g}-start`} data-testid={`rate-row-${g}-start`}>
                <td>{g}</td>
                <td>start</td>
                <td data-testid="rate-to">{start?.to ?? ''}</td>
                <td data-testid="rate-pct">{start?.pct ?? 'n/a'}</td>
                <td data-testid="rate-version">{start?.version ?? ''}</td>
                <td>
                  <button type="button" data-testid="rate-withdraw" disabled={withdraw.isPending}
                    onClick={() => takeWithdraw(g, null)}>
                    Withdraw
                  </button>
                </td>
                <td rowSpan={dated.length + 1} data-testid={`rate-current-${g}`}>
                  {rateOn(rates.data, g, today) ?? 'n/a'}
                </td>
              </tr>,
              ...dated.map((r) => (
                <tr key={`${g}-${r.from}`} data-testid={`rate-row-${g}-${r.from}`}>
                  <td>{g}</td>
                  <td data-testid="rate-from">{r.from}</td>
                  <td data-testid="rate-to">{r.to ?? ''}</td>
                  <td data-testid="rate-pct">{r.pct}</td>
                  <td data-testid="rate-version">{r.version}</td>
                  <td>
                    <button type="button" data-testid="rate-withdraw" disabled={withdraw.isPending}
                      onClick={() => takeWithdraw(g, r.from)}>
                      Withdraw
                    </button>
                  </td>
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
          withdraw.reset();
          submit({ grade, pct, from, to });
        }}
      >
        <select
          data-testid="rate-form-grade"
          value={grade}
          onChange={(e) => { setGrade(e.target.value); save.reset(); withdraw.reset(); }}
        >
          {gradeCodes.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>
        <input
          data-testid="rate-form-pct"
          placeholder="Percent"
          inputMode="decimal"
          value={pct}
          onChange={(e) => { setPct(e.target.value); save.reset(); withdraw.reset(); }}
        />
        <input
          data-testid="rate-form-from"
          placeholder="From (YYYY-MM-DD, blank: start)"
          value={from}
          onChange={(e) => { setFrom(e.target.value); save.reset(); withdraw.reset(); }}
        />
        <input
          data-testid="rate-form-to"
          placeholder="To (YYYY-MM-DD, blank: no end)"
          value={to}
          onChange={(e) => { setTo(e.target.value); save.reset(); withdraw.reset(); }}
        />
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {withdraw.isError && (
          <span data-testid="rate-form-error">Only a steward can withdraw a rate, and only one that is set.</span>
        )}
        {save.isError && (
          <span data-testid="rate-form-error">The rate must be a percent, 0.00 or more, with up to two decimals, the from-date a calendar date or blank, and the to-date a calendar date on or after the from-date, or blank.</span>
        )}
      </form>
      <form
        data-testid="rate-preview-form"
        onSubmit={(e) => {
          e.preventDefault();
          preview.reset();
          preview.mutate({ grade: previewGrade, pct: previewPct });
        }}
      >
        <select
          data-testid="rate-preview-grade"
          value={previewGrade}
          onChange={(e) => { setPreviewGrade(e.target.value); preview.reset(); }}
        >
          {gradeCodes.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>
        <input
          data-testid="rate-preview-pct"
          placeholder="Candidate percent"
          inputMode="decimal"
          value={previewPct}
          onChange={(e) => { setPreviewPct(e.target.value); preview.reset(); }}
        />
        <button type="submit" data-testid="rate-preview" disabled={preview.isPending}>
          Preview
        </button>
        {preview.isSuccess && (
          <span>
            {' '}<span data-testid="rate-preview-count">{preview.data.count}</span> claims would change by{' '}
            <span data-testid="rate-preview-diff">{preview.data.diff}</span>
          </span>
        )}
        {preview.isError && (
          <span data-testid="rate-preview-error">The candidate rate must be a percent, 0.00 or more, with up to two decimals.</span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 5, Component: Rates });
