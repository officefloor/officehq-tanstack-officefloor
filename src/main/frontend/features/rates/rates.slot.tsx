import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { total, useClaims } from '../claims/claims';
import { GRADES, usePeople, type Person } from '../people/people';
import { due, rateFor, ratesKey, sum, useRates, type Rate } from './rates';

/** The sheet's rate for each grade: the form that sets one, and each grade's current rate. */
function Rates({ sheetId, user }: { sheetId: string; user: string }) {
  const rates = useRates(sheetId);
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [pct, setPct] = useState('');

  const save = useMutation({
    mutationFn: (v: { grade: string; rate: string }) =>
      putJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates/${encodeURIComponent(v.grade)}`, {
        rate: v.rate,
        by: user,
      }),
    onSuccess: () => {
      setPct('');
      void queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
  });

  return (
    <section data-testid="rates">
      <h3>Rates</h3>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          save.reset();
          save.mutate({ grade, rate: pct });
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
        {save.isError && (
          <span data-testid="rate-form-error">The rate must be a percent 0.00 or more, with up to two decimals.</span>
        )}
      </form>
      {rates.isError && <p data-testid="rates-load-error">Could not load rates.</p>}
      <table data-testid="rates-table">
        <tbody>
          {GRADES.map((g) => {
            const rate = rateFor(rates.data, g);
            return (
              <tr key={g} data-testid={rate === undefined ? undefined : `rate-row-${g}-start`}>
                <td>{g}</td>
                <td data-testid={`rate-current-${g}`}>{rate === undefined ? 'n/a' : <span data-testid="rate-pct">{rate}</span>}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

function DueHead() {
  return <th>Due total</th>;
}

/** A person's due-total: their claimed at their grade's rate. */
function PersonDueTotal({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const claimed = total(claims.data?.filter((c) => c.personId === person.id) ?? []);
  return <td data-testid="person-due-total">{due(claimed, rateFor(rates.data, person.grade))}</td>;
}

/** The sheet's due-total: everyone's due-total added up, skipping n/a. */
function DueTotal({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const dues = (people.data ?? []).map((p) =>
    due(total(claims.data?.filter((c) => c.personId === p.id) ?? []), rateFor(rates.data, p.grade)),
  );
  return (
    <p>
      Due total <span data-testid="rollup-due-total">{sum(dues)}</span>
    </p>
  );
}

export const contributions = [
  SheetPage.fill({ order: 5, Component: Rates }),
  PeopleHead.fill({ order: 20, Component: DueHead }),
  PersonRow.fill({ order: 20, Component: PersonDueTotal }),
  SheetPage.fill({ order: 30, Component: DueTotal }),
];
