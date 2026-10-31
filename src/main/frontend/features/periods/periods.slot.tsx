import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimDueCents, live, useClaims } from '../claims/useClaims';
import { gradeOn, usePeople } from '../people/usePeople';
import { rateOn, totalDue, useRates } from '../rates/useRates';
import { useFloor, useLate } from '../settings/useSettings';
import { DATE, periodOf, periodsKey, usePeriods, type Period } from './usePeriods';

/**
 * The sheet's periods, each with its due-total — the figures of everyone's counted (OPEN or
 * SETTLED) claims dated in it, not a held person's OPEN ones, n/a ones skipped — and one form to add a period: a name unique on
 * the sheet and a from and to date, inclusive, overlapping no other period. A period can be closed:
 * its due-total is then the one it had when it was closed, whatever rates do later.
 */
function PeriodsPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: periods = [] } = usePeriods(sheet);
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [invalid, setInvalid] = useState<string | null>(null);
  // The period whose last close was refused: its row shows the error.
  const [refused, setRefused] = useState<string | null>(null);

  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (p: { name: string; from: string; to: string }) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheet)}/periods`, { ...p, user }),
    onSuccess: async () => {
      setName('');
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet) });
    },
  });

  const close = useMutation({
    mutationFn: (name: string) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheet)}/periods/${encodeURIComponent(name)}/close`, { user }),
    onMutate: () => setRefused(null),
    onError: (_e, name) => setRefused(name),
    onSettled: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
  });

  // An error goes as soon as its form is changed or submitted again.
  const clearError = () => {
    save.reset();
    setInvalid(null);
  };

  // Refused here, before anything is sent, when the form alone shows it is bad.
  const check = (): string | null => {
    const n = name.trim();
    if (n === '') return 'A period needs a name.';
    if (!DATE.test(from.trim()) || !DATE.test(to.trim())) return 'The dates must be yyyy-mm-dd.';
    if (from.trim() > to.trim()) return 'The from-date must be on or before the to-date.';
    if (periods.some((p) => p.name === n)) return 'There is already a period of that name.';
    const clash = periods.find((p) => p.from <= to.trim() && from.trim() <= p.to);
    if (clash) return `The period overlaps ${clash.name}.`;
    return null;
  };

  const personOf = new Map(people.map((p) => [p.id, p]));
  const held = new Set(people.filter((p) => p.held).map((p) => p.id));
  const dueIn = (period: Period) =>
    period.closed && period.closedTotal !== null
      ? period.closedTotal
      : totalDue(
          live(all, (id) => held.has(id))
            .filter((c) => periodOf([period], c.date) !== undefined && personOf.has(c.personId))
            .map((c) => claimDueCents(c, floor, rateOn(rates, gradeOn(personOf.get(c.personId)!, c.date), c.date), late)),
        );

  return (
    <section data-testid="periods-panel">
      <h2>Periods</h2>
      <form
        data-testid="period-form"
        onSubmit={(e) => {
          e.preventDefault();
          const problem = check();
          if (problem !== null) {
            save.reset();
            setInvalid(problem);
            return;
          }
          setInvalid(null);
          submitOnce(() => save.mutateAsync({ name: name.trim(), from: from.trim(), to: to.trim() }));
        }}
      >
        <label>
          Name{' '}
          <input
            data-testid="period-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <label>
          From{' '}
          <input
            data-testid="period-form-from"
            placeholder="yyyy-mm-dd"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            data-testid="period-form-to"
            placeholder="yyyy-mm-dd"
            value={to}
            onChange={(e) => {
              setTo(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {(invalid !== null || save.isError) && (
          <span data-testid="period-form-error" role="alert">
            {' '}
            {invalid ?? 'Could not add this period.'}
          </span>
        )}
      </form>
      <table data-testid="period-list">
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due</th>
            <th>Closed</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{dueIn(p)}</td>
              <td data-testid="period-closed">{p.closed ? 'yes' : 'no'}</td>
              <td>
                <button
                  type="button"
                  data-testid="period-close"
                  disabled={close.isPending}
                  onClick={() => close.mutate(p.name)}
                >
                  Close
                </button>
                {refused === p.name && (
                  <span data-testid="period-error" role="alert">
                    {' '}
                    Could not close this period.
                  </span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 6, Component: PeriodsPanel });
