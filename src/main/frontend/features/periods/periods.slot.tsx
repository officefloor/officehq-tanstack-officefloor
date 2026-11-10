import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useUser } from '../session/session';
import { claimDue, rateOn, sumDue, useRates } from '../rates/rates';
import { periodsKey, usePeriods } from './periods';
import type { Period } from './periods';

type Fields = { name: string; from: string; to: string };
const EMPTY: Fields = { name: '', from: '', to: '' };

// The sheet's periods: a form that adds one (a from-date left blank is the sheet's today), and each
// period with its due-total: the due of the claims that count whose date falls in it.
function Periods({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const periods = usePeriods(sheetId);
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const save = useMutation({
    mutationFn: (body: Fields) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, body, user),
    onSuccess: () => {
      setFields(EMPTY);
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: () => setError('That period could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const body = { name: fields.name.trim(), from: fields.from.trim(), to: fields.to.trim() };
    if (body.name === '') {
      setError('A name is required.');
      return;
    }
    if (body.to === '') {
      setError('A to-date is required.');
      return;
    }
    once((done) => save.mutate(body, { onSettled: done }));
  };

  const change = (next: Partial<Fields>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
  };

  const dueTotal = (p: Period) => {
    const dues = people.flatMap((person) =>
      claims
        .filter((c) => c.personId === person.id && counts(c) && p.from <= c.date && c.date <= p.to)
        .map((c) => claimDue(c, rateOn(rates, person.grade, c.date))),
    );
    return sumDue(dues);
  };

  return (
    <section data-testid="periods">
      <h2>Periods</h2>
      <form onSubmit={submit} data-testid="period-form">
        <label>
          Name{' '}
          <input data-testid="period-form-name" value={fields.name} onChange={(e) => change({ name: e.target.value })} />
        </label>{' '}
        <label>
          From{' '}
          <input
            type="date"
            data-testid="period-form-from"
            value={fields.from}
            onChange={(e) => change({ from: e.target.value })}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            type="date"
            data-testid="period-form-to"
            value={fields.to}
            onChange={(e) => change({ to: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {error && (
          <p role="alert" data-testid="period-form-error">
            {error}
          </p>
        )}
      </form>
      {periods.isError && (
        <p role="alert" data-testid="periods-error">
          The periods could not be read.
        </p>
      )}
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
          {(periods.data ?? []).map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
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

export const contribution = SheetBody.fill({ order: -5, Component: Periods });
