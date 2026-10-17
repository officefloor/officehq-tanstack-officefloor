import { useState } from 'react';
import type { FormEvent } from 'react';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { counts, useClaims } from '../claims/claims';
import { claimDueCents, show, usePersonRate } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { addPeriod, periodsKey, usePeriods } from './periods';
import type { Period, PeriodInput } from './periods';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

const EMPTY: PeriodInput = { name: '', from: '', to: '' };

/** A period's due-total in cents: the due of the claims that count whose date falls in it (n/a as 0). */
function usePeriodDue() {
  const claims = useClaims().data ?? [];
  const rateOf = usePersonRate();
  return (period: Period) =>
    claims
      .filter((c) => counts(c) && period.from <= c.date && c.date <= period.to)
      .reduce((n, c) => n + (claimDueCents(c, rateOf(c.personId, c.date)) ?? 0n), 0n);
}

// The sheet's periods: a form that adds one (a name unique on the sheet, from on or before to, not
// overlapping another), and each period with its dates and its due-total.
function Periods() {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const periods = usePeriods().data ?? [];
  const dueOf = usePeriodDue();
  const [form, setForm] = useState<PeriodInput>(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (input: PeriodInput) => addPeriod(sheet!, user, input),
    onSuccess: async () => {
      setForm(EMPTY);
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet!) });
    },
    onError: async () => {
      setError('The period could not be added: its name must be new on the sheet and its dates must not overlap another period.');
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet!) });
    },
  });

  if (!sheet) return null;

  const change = (patch: Partial<PeriodInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const name = form.name.trim();
    const from = form.from.trim();
    const to = form.to.trim();
    if (name === '') {
      setError('The period must have a name.');
      return;
    }
    if (!DATE.test(from) || !DATE.test(to)) {
      setError('The from and to dates must be dates (yyyy-mm-dd).');
      return;
    }
    if (to < from) {
      setError('The to-date must be on or after the from-date.');
      return;
    }
    if (periods.some((p) => p.name === name)) {
      setError(`A period named ${name} is already on the sheet.`);
      return;
    }
    const clash = periods.find((p) => p.from <= to && from <= p.to);
    if (clash) {
      setError(`The period overlaps ${clash.name}.`);
      return;
    }
    once(() => save.mutateAsync({ name, from, to }));
  };

  return (
    <section data-testid="periods">
      <h2>Periods</h2>
      <form data-testid="period-form" onSubmit={submit}>
        <input
          data-testid="period-form-name"
          placeholder="Name"
          value={form.name}
          onChange={(e) => change({ name: e.target.value })}
        />
        <input
          data-testid="period-form-from"
          type="date"
          value={form.from}
          onChange={(e) => change({ from: e.target.value })}
        />
        <input
          data-testid="period-form-to"
          type="date"
          value={form.to}
          onChange={(e) => change({ to: e.target.value })}
        />
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {error && <span data-testid="period-form-error">{error}</span>}
      </form>
      <table data-testid="period-list">
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{show(dueOf(p))}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contributions = [HomeMain.fill({ order: 6, Component: Periods })];
