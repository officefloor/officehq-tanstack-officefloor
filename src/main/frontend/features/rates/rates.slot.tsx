import { useState } from 'react';
import type { FormEvent } from 'react';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useClaimDue, useClaims, useCounts } from '../claims/claims';
import { GRADES, gradeOn, usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { NA, PERCENT, ratesKey, setRate, show, useGradeRate, usePersonRate, useRates } from './rates';
import type { RateInput } from './rates';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

const EMPTY: RateInput = { grade: 'A', rate: '', from: '' };

// The sheet's rates: a form that sets a grade's rate from a from-date (none: from the beginning of
// time), replacing any rate it had for that from-date; each grade's rate in force on the sheet's today
// (n/a when none applies); and every rate set, by grade then from-date.
function Rates() {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const rateOf = useGradeRate();
  const rates = useRates().data ?? [];
  const [form, setForm] = useState<RateInput>(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (input: RateInput) => setRate(sheet!, user, input),
    onSuccess: async () => {
      setForm((f) => ({ ...f, rate: '', from: '' }));
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheet!) });
    },
    onError: () =>
      setError('The rate could not be set: it must be a percent of 0.00 or more, with up to two decimals, and a date if from is given.'),
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
    const from = form.from.trim();
    if (from !== '' && !DATE.test(from)) {
      setError('The from-date must be a date (yyyy-mm-dd).');
      return;
    }
    once(() => save.mutateAsync({ grade: form.grade, rate: form.rate.trim(), from }));
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
        <input
          data-testid="rate-form-from"
          type="date"
          placeholder="From (yyyy-mm-dd)"
          value={form.from}
          onChange={(e) => change({ from: e.target.value })}
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
            <th>Rate today (%)</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>{rateOf(g) ?? NA}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table data-testid="rate-history">
        <thead>
          <tr>
            <th>Grade</th>
            <th>From</th>
            <th>Rate (%)</th>
          </tr>
        </thead>
        <tbody>
          {rates.map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from ?? 'start'}</td>
              <td data-testid="rate-pct">{r.rate}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function DueColumn() {
  return <th>Due</th>;
}

/** A person's due-total in cents: the already-rounded figures of their claims that count added up
 *  (0 when none counts). Undefined for n/a: their grade has no rate today and no claim keeps a figure
 *  (a settled claim's fixed due, or one dated under another grade than today's that has a due). */
export function useDueTotal() {
  const claims = useClaims().data ?? [];
  const rateOf = usePersonRate();
  const counts = useCounts();
  const claimDue = useClaimDue();
  const people = usePeople().data ?? [];
  const today = useSheetToday();
  return (personId: number) => {
    const person = people.find((p) => p.id === personId);
    const gradeToday = gradeOn(person, today);
    const figures = claims
      .filter((c) => c.personId === personId && counts(c))
      .map((c) => ({ claim: c, due: claimDue(c, rateOf(personId, c.date)) }));
    const kept = figures.some((f) => f.due !== undefined
      && (f.claim.status === 'SETTLED' || gradeOn(person, f.claim.date) !== gradeToday));
    if (rateOf(personId) === undefined && !kept) return undefined;
    return figures.reduce((n, f) => n + (f.due ?? 0n), 0n);
  };
}

// A person's due-total: n/a when their grade has no rate today, unless a claim keeps a figure.
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
