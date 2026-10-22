import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { PersonRow } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheet';
import { sumAmounts, useClaims } from '../claims/claims';
import { GRADES, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { dueOn, ratesKey, sumDue, useRateFor, useRates } from './rates';
import type { Rate } from './rates';

const PCT = /^\d{1,3}(\.\d{1,2})?$/;

/** The sheet's rate for each grade, and the form that sets one. */
function Rates({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const rates = useRates(sheetId);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [error, setError] = useState('');

  const save = useMutation({
    mutationFn: (body: { grade: string; rate: string }) =>
      putJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates/${body.grade}`, { user, rate: body.rate }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
      setPct('');
    },
    onError: () => setError('Could not set this rate: it must be a percent, 0.00 to 999.99, with up to two decimals.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!PCT.test(pct.trim())) {
      setError('The rate must be a percent, 0.00 to 999.99, with up to two decimals.');
      return;
    }
    save.mutate({ grade, rate: pct.trim() });
  };

  return (
    <section data-testid="rates">
      <h2>Rates</h2>
      <form data-testid="rate-form" onSubmit={submit}>
        <label>
          Grade{' '}
          <select
            data-testid="rate-form-grade"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
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
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {error && (
          <p data-testid="rate-form-error" role="alert">
            {error}
          </p>
        )}
      </form>
      <table>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g} data-testid={`rate-row-${g}-start`}>
              <td>{g}</td>
              <td data-testid="rate-pct">
                <span data-testid={`rate-current-${g}`}>
                  {rates.data?.find((r) => r.grade === g)?.rate ?? 'n/a'}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

/** A person's due-total: their claimed times their grade's rate, divided by 100, rounded half up. */
function PersonDueTotal({ sheetId, person }: { sheetId: string; user: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rate = useRateFor(sheetId, person.grade);
  return <td data-testid="person-due-total">{dueOn(sumAmounts(claims), rate)}</td>;
}

/** The sheet's due-total: every person's due-total added up, skipping n/a. */
function DueTotal({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const dues = people.map((p) =>
    dueOn(
      sumAmounts(claims.filter((c) => c.personId === p.id)),
      rates.find((r) => r.grade === p.grade)?.rate,
    ),
  );
  return (
    <p>
      Due total <strong data-testid="rollup-due-total">{sumDue(dues)}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 5, Component: Rates }),
  SheetBody.fill({ order: 21, Component: DueTotal }),
  PersonRow.fill({ order: 20, Component: PersonDueTotal }),
];
