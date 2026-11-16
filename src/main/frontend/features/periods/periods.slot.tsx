import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { counts, useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { claimDue, rateFor, sum, useRates } from '../rates/rates';
import { periodsKey, usePeriods, type Period } from './periods';

type Values = { name: string; from: string; to: string };
const EMPTY: Values = { name: '', from: '', to: '' };

/** The sheet's periods: the form that adds one, and each period with its due-total — the figures of the claims
 *  that count whose date falls in it, added up (skipping n/a). */
function Periods({ sheetId, user }: { sheetId: string; user: string }) {
  const periods = usePeriods(sheetId);
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const queryClient = useQueryClient();
  const [values, setValues] = useState<Values>(EMPTY);

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: (v: Values) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, { ...v, by: user }),
    onSuccess: () => {
      setValues(EMPTY);
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onSettled: submit.end,
  });
  const change = (v: Partial<Values>) => {
    setValues((was) => ({ ...was, ...v }));
    save.reset();
  };

  const dueTotal = (p: Period) => {
    const grades = new Map((people.data ?? []).map((person) => [person.id, person.grade]));
    const inPeriod = (claims.data ?? []).filter((c) => counts(c) && p.from <= c.date && c.date <= p.to);
    return sum(
      inPeriod.map((c) => {
        const grade = grades.get(c.personId);
        return claimDue(c, grade === undefined ? undefined : rateFor(rates.data, grade, c.date));
      }),
    );
  };

  return (
    <section data-testid="periods">
      <h3>Periods</h3>
      <form
        data-testid="period-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          save.reset();
          save.mutate(values);
        }}
      >
        <input data-testid="period-form-name" placeholder="Name" value={values.name} onChange={(e) => change({ name: e.target.value })} />{' '}
        <label>
          From <input type="date" data-testid="period-form-from" value={values.from} onChange={(e) => change({ from: e.target.value })} />
        </label>{' '}
        <label>
          To <input type="date" data-testid="period-form-to" value={values.to} onChange={(e) => change({ to: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {save.isError && (
          <span data-testid="period-form-error">
            A period needs a name not already on the sheet, and dates from on or before to that overlap no other period.
          </span>
        )}
      </form>
      {periods.isError && <p data-testid="periods-load-error">Could not load periods.</p>}
      <table data-testid="periods-table">
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{dueTotal(p)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 7, Component: Periods });
