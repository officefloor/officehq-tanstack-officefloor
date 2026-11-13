import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useRates } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useDueRules } from '../settings/settings';
import { periodDue, periodsKey, usePeriods } from './periods';
import type { Period, PeriodInput } from './periods';
import { useOneAtATime } from '../../query/useOneAtATime';

const EMPTY: PeriodInput = { name: '', from: '', to: '' };

/** yyyy-MM-dd. */
const DATE = /^\d{4}-\d{2}-\d{2}$/;

// The sheet's periods, each with its due-total, and the form that adds one.
export function Periods() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const periods = usePeriods().data ?? [];
  const claims = useClaims().data ?? [];
  const people = usePeople().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const [form, setForm] = useState<PeriodInput>(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (input: PeriodInput) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheet)}/periods?user=${encodeURIComponent(user)}`, input),
    onSuccess: async () => {
      setForm(EMPTY);
      setError('');
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet) });
    },
    onError: () => setError('That period could not be added: its name must be new and its dates must not overlap another period.'),
  });

  // The generate form: one period per calendar month from the from-date's month to the to-date's month.
  const [gen, setGen] = useState({ from: '', to: '' });
  const [genError, setGenError] = useState('');
  const [made, setMade] = useState<number | undefined>(undefined);
  const generate = useMutation({
    mutationFn: (input: { from: string; to: string }) =>
      postJson<{ made: number }>(
        `/api/sheets/${encodeURIComponent(sheet)}/generate-periods?user=${encodeURIComponent(user)}`,
        input,
      ),
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet) });
      setMade(result.made);
    },
    onError: () => setGenError('No periods were generated: the to-date must not be before the from-date.'),
  });

  // The period whose close or reopen was refused, and why; cleared by the next action on a period.
  const [rowError, setRowError] = useState<{ name: string; message: string } | null>(null);
  const act = useMutation({
    mutationFn: ({ name, action }: { name: string; action: 'close' | 'soft-close' | 'reopen' }) =>
      postJson<Period>(
        `/api/sheets/${encodeURIComponent(sheet)}/periods/${encodeURIComponent(name)}/${action}?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet) });
    },
    onError: (_e, { name, action }) =>
      setRowError({
        name,
        message:
          action === 'close'
            ? 'That period could not be closed: it is already closed, or your role is not allowed to close a period.'
            : action === 'soft-close'
              ? 'That period could not be soft-closed: it is already closed or soft-closed, or your role is not allowed to close a period.'
              : 'That period could not be reopened: it is not closed, or your role is not allowed to reopen a period.',
      }),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const input = { name: form.name.trim(), from: form.from.trim(), to: form.to.trim() };
    if (input.name === '') {
      setError('Type a name for the period.');
      return;
    }
    if (!DATE.test(input.from) || !DATE.test(input.to)) {
      setError('Type the from and to dates as yyyy-mm-dd.');
      return;
    }
    if (input.to < input.from) {
      setError('The to-date must not be before the from-date.');
      return;
    }
    once(() => add.mutateAsync(input));
  };

  const submitGenerate = (e: FormEvent) => {
    e.preventDefault();
    setGenError('');
    setMade(undefined);
    const input = { from: gen.from.trim(), to: gen.to.trim() };
    if ((input.from && !DATE.test(input.from)) || (input.to && !DATE.test(input.to))) {
      setGenError('Type the from and to dates as yyyy-mm-dd.');
      return;
    }
    if (input.from && input.to && input.to < input.from) {
      setGenError('The to-date must not be before the from-date.');
      return;
    }
    once(() => generate.mutateAsync(input));
  };

  const changeGenerate = (patch: Partial<{ from: string; to: string }>) => {
    setGen((g) => ({ ...g, ...patch }));
    setGenError('');
  };

  const change = (patch: Partial<PeriodInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  return (
    <section data-testid="periods">
      <table data-testid="period-list">
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due</th>
            <th>Closed</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{periodDue(p, claims, people, rates, rules)}</td>
              <td data-testid="period-closed">{p.closed ? 'yes' : p.soft ? 'soft' : 'no'}</td>
              <td>
                <button
                  type="button"
                  data-testid="period-close"
                  disabled={act.isPending}
                  onClick={() => {
                    setRowError(null);
                    once(() => act.mutateAsync({ name: p.name, action: 'close' }));
                  }}
                >
                  Close
                </button>
                <button
                  type="button"
                  data-testid="period-soft-close"
                  disabled={act.isPending}
                  onClick={() => {
                    setRowError(null);
                    once(() => act.mutateAsync({ name: p.name, action: 'soft-close' }));
                  }}
                >
                  Soft close
                </button>
                <button
                  type="button"
                  data-testid="period-reopen"
                  disabled={act.isPending}
                  onClick={() => {
                    setRowError(null);
                    once(() => act.mutateAsync({ name: p.name, action: 'reopen' }));
                  }}
                >
                  Reopen
                </button>
                {rowError?.name === p.name && <span data-testid="period-error">{rowError.message}</span>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <form onSubmit={submit} data-testid="period-form">
        <input
          data-testid="period-form-name"
          placeholder="Period name"
          value={form.name}
          onChange={(e) => change({ name: e.target.value })}
        />
        <input
          data-testid="period-form-from"
          placeholder="From yyyy-mm-dd"
          value={form.from}
          onChange={(e) => change({ from: e.target.value })}
        />
        <input
          data-testid="period-form-to"
          placeholder="To yyyy-mm-dd"
          value={form.to}
          onChange={(e) => change({ to: e.target.value })}
        />
        <button type="submit" data-testid="period-form-submit" disabled={add.isPending}>
          Add period
        </button>
        {error && <span data-testid="period-form-error">{error}</span>}
      </form>
      <form onSubmit={submitGenerate} data-testid="period-generate-form">
        <input
          data-testid="period-generate-from"
          placeholder="From yyyy-mm-dd"
          value={gen.from}
          onChange={(e) => changeGenerate({ from: e.target.value })}
        />
        <input
          data-testid="period-generate-to"
          placeholder="To yyyy-mm-dd"
          value={gen.to}
          onChange={(e) => changeGenerate({ to: e.target.value })}
        />
        <button type="submit" data-testid="period-generate" disabled={generate.isPending}>
          Generate monthly periods
        </button>
        {made !== undefined && (
          <>
            {' '}
            Made <span data-testid="period-generated">{made}</span>
          </>
        )}
        {genError && <span data-testid="period-generate-error">{genError}</span>}
      </form>
    </section>
  );
}
