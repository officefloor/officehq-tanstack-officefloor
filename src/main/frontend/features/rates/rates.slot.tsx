import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { claimed, useClaims } from '../claims/useClaims';
import { GRADES, usePeople } from '../people/usePeople';
import { due, rateFor, ratesKey, sumDues, useRates, type Rate } from './useRates';

/** The sheet's rate for each grade: a form that sets one (replacing the grade's rate), the rates
 *  set, and the rate in force for every grade ('n/a' when it has none). */
function Rates({ sheetId }: { sheetId: string }) {
  const rates = useRates(sheetId);
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');

  const save = useMutation({
    mutationFn: (f: { grade: string; pct: string }) =>
      postJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`, f),
    onSuccess: async () => {
      setPct('');
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
  });

  return (
    <section data-testid="rates">
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          save.reset();
          save.mutate({ grade, pct });
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
          </tr>
        </thead>
        <tbody>
          {(rates.data ?? []).map((r) => (
            <tr key={r.grade} data-testid={`rate-row-${r.grade}-start`}>
              <td data-testid="rate-grade">{r.grade}</td>
              <td data-testid="rate-pct">{r.pct}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Current rates:{' '}
        {GRADES.map((g) => (
          <span key={g}>
            {g} <strong data-testid={`rate-current-${g}`}>{rateFor(rates.data, g) ?? 'n/a'}</strong>{' '}
          </span>
        ))}
      </p>
      {rates.isError && <p data-testid="rates-error">{rates.error.message}</p>}
    </section>
  );
}

/** Each person's due-total, by person id: their claimed times their grade's rate, divided by 100,
 *  rounded half up; 'n/a' when their grade has no rate. */
function useDueTotals(sheetId: string): Map<number, string> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const totals = new Map<number, string>();
  for (const p of people.data ?? []) {
    const mine = (claims.data ?? []).filter((c) => c.personId === p.id);
    totals.set(p.id, due(claimed(mine), rateFor(rates.data, p.grade)));
  }
  return totals;
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
