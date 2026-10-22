import { useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { counts, useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { claimDue, rateOn, sumDue, useRates } from '../rates/rates';
import { periodOf, periodsKey, usePeriods } from './periods';
import type { Period } from './periods';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** The sheet's periods, each with its due-total — the dues (as shown on their rows) of the claims that count
 *  whose date falls in it — and the form that adds one. */
function Periods({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const periods = usePeriods(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const people = usePeople(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (body: { name: string; from: string; to: string }) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, { user, ...body }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
      setName('');
      setFrom('');
      setTo('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : 'Could not add this period.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('The period needs a name.');
      return;
    }
    if (!DATE.test(from.trim()) || !DATE.test(to.trim())) {
      setError('The from and to dates must be dates, yyyy-mm-dd.');
      return;
    }
    if (from.trim() > to.trim()) {
      setError('The from-date must be on or before the to-date.');
      return;
    }
    if (!once.start()) return;
    add.mutate({ name: name.trim(), from: from.trim(), to: to.trim() });
  };

  const change = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    setError('');
  };

  const dueTotal = (period: Period) =>
    sumDue(
      claims
        .filter((c) => counts(c) && periodOf([period], c.date))
        .map((c) => {
          const grade = people.find((p) => p.id === c.personId)?.grade ?? '';
          return claimDue(c, rateOn(rates, grade, c.date));
        }),
    );

  return (
    <section data-testid="periods">
      <h2>Periods</h2>
      <form data-testid="period-form" onSubmit={submit}>
        <label>
          Name <input data-testid="period-form-name" value={name} onChange={change(setName)} />
        </label>{' '}
        <label>
          From <input data-testid="period-form-from" placeholder="yyyy-mm-dd" value={from} onChange={change(setFrom)} />
        </label>{' '}
        <label>
          To <input data-testid="period-form-to" placeholder="yyyy-mm-dd" value={to} onChange={change(setTo)} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={add.isPending}>
          Add period
        </button>
        {error && (
          <p data-testid="period-form-error" role="alert">
            {error}
          </p>
        )}
      </form>
      <table>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td>
                Due total <span data-testid="period-due-total">{dueTotal(p)}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6, Component: Periods });
