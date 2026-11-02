import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { GRADES } from '../people/usePeople';
import { useSheetToday } from '../today/useSheetToday';
import { useDueTotals } from './useDueTotals';
import { rateFor, ratesKey, sumDues, useRates, type Rate } from './useRates';

/** The sheet's rates for each grade: a form that sets one from an optional from-date (replacing the
 *  grade's rate from that date; blank is from the beginning of time), the rates set, and the rate in
 *  force on the sheet's today for every grade ('n/a' when none is). */
function Rates({ sheetId }: { sheetId: string }) {
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId);
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { grade: string; pct: string; from: string }) =>
      postJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`, f),
    onSuccess: async () => {
      setPct('');
      setFrom('');
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
  });

  return (
    <section data-testid="rates">
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ grade, pct, from });
          });
        }}
      >
        <label>
          Grade{' '}
          <select
            data-testid="rate-form-grade"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              save.reset();
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
            inputMode="decimal"
            value={pct}
            onChange={(e) => {
              setPct(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          From{' '}
          <input
            type="text"
            placeholder="yyyy-mm-dd"
            data-testid="rate-form-from"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && <p data-testid="rate-form-error">{save.error.message}</p>}
      </form>
      <table data-testid="rates-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate %</th>
            <th>From</th>
          </tr>
        </thead>
        <tbody>
          {(rates.data ?? []).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td data-testid="rate-grade">{r.grade}</td>
              <td data-testid="rate-pct">{r.pct}</td>
              <td data-testid="rate-from">{r.from ?? ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Current rates:{' '}
        {GRADES.map((g) => (
          <span key={g}>
            {g} <strong data-testid={`rate-current-${g}`}>{rateFor(rates.data, g, today.data?.today) ?? 'n/a'}</strong>{' '}
          </span>
        ))}
      </p>
      {rates.isError && <p data-testid="rates-error">{rates.error.message}</p>}
    </section>
  );
}

function PersonDueTotal({ sheetId, personId }: { sheetId: string; personId: number }) {
  const totals = useDueTotals(sheetId);
  return <td data-testid="person-due-total">{totals.get(personId) ?? ''}</td>;
}

/** The sheet's due-total: everyone's due-total added up, skipping 'n/a'. */
function DueTotal({ sheetId }: { sheetId: string }) {
  const totals = useDueTotals(sheetId);
  return (
    <p>
      Due total: <strong data-testid="rollup-due-total">{sumDues([...totals.values()])}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 5, Component: ({ sheetId }) => <Rates sheetId={sheetId} /> }),
  PersonColumnHead.fill({ order: 20, Component: () => <th>Due</th> }),
  PersonCells.fill({ order: 20, Component: PersonDueTotal }),
  SheetBody.fill({ order: 30, Component: ({ sheetId }) => <DueTotal sheetId={sheetId} /> }),
];
