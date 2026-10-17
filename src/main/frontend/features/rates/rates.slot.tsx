import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { total, useClaims } from '../claims/claims';
import { GRADES, usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { NA, PERCENT, dueCents, ratesKey, setRate, show, useGradeRate, usePersonRate } from './rates';
import type { RateInput } from './rates';

const EMPTY: RateInput = { grade: 'A', rate: '' };

// The sheet's rates: a form that sets a grade's rate (replacing any it had), and each grade's
// current rate (n/a when it has none).
function Rates() {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const rateOf = useGradeRate();
  const [form, setForm] = useState<RateInput>(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: (input: RateInput) => setRate(sheet!, user, input),
    onSuccess: async () => {
      setForm((f) => ({ ...f, rate: '' }));
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheet!) });
    },
    onError: () => setError('The rate could not be set: it must be a percent of 0.00 or more, with up to two decimals.'),
  });

  if (!sheet) return null;

  const change = (patch: Partial<RateInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!PERCENT.test(form.rate.trim())) {
      setError('The rate must be a percent of 0.00 or more, with up to two decimals.');
      return;
    }
    save.mutate({ grade: form.grade, rate: form.rate.trim() });
  };

  return (
    <section data-testid="rates">
      <h2>Rates</h2>
      <form data-testid="rate-form" onSubmit={submit}>
        <select data-testid="rate-form-grade" value={form.grade} onChange={(e) => change({ grade: e.target.value })}>
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
        <input
          data-testid="rate-form-pct"
          placeholder="Percent"
          inputMode="decimal"
          value={form.rate}
          onChange={(e) => change({ rate: e.target.value })}
        />
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {error && <span data-testid="rate-form-error">{error}</span>}
      </form>
      <table data-testid="rate-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate (%)</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const rate = rateOf(g);
            return (
              <tr key={g} data-testid={rate !== undefined ? `rate-row-${g}-start` : undefined}>
                <td>{g}</td>
                <td data-testid={`rate-current-${g}`}>
                  {rate !== undefined ? <span data-testid="rate-pct">{rate}</span> : NA}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

function DueColumn() {
  return <th>Due</th>;
}

/** A person's due-total in cents (their claimed × their grade's rate / 100), or undefined for n/a. */
function useDueTotal() {
  const claims = useClaims().data ?? [];
  const rateOf = usePersonRate();
  return (personId: number) => {
    const rate = rateOf(personId);
    if (rate === undefined) return undefined;
    return dueCents(total(claims.filter((c) => c.personId === personId)), rate);
  };
}

// A person's due-total: n/a when their grade has no rate.
function PersonDueTotal({ personId }: { personId: number }) {
  const due = useDueTotal()(personId);
  return <td data-testid="person-due-total">{due === undefined ? NA : show(due)}</td>;
}

// The sheet's due-total: everyone's due-total added up, skipping n/a.
function DueTotal() {
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const dueOf = useDueTotal();
  if (!sheet) return null;
  const sum = people.reduce((n, p) => n + (dueOf(p.id) ?? 0n), 0n);
  return (
    <section data-testid="rollup-due">
      Due total <strong data-testid="rollup-due-total">{show(sum)}</strong>
    </section>
  );
}

export const contributions = [
  HomeMain.fill({ order: 5, Component: Rates }),
  PersonColumns.fill({ order: 20, Component: DueColumn }),
  PersonCells.fill({ order: 20, Component: PersonDueTotal }),
  HomeMain.fill({ order: 21, Component: DueTotal }),
];
