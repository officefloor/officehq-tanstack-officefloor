import { type ChangeEvent, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { counts, live, useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { claimDue, dueTotal, useRates } from '../rates/rates';
import { useSettings } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { lateOf } from '../overdue/overdue';
import { type Period, contains, periodsKey, usePeriods } from './periods';

type Fields = { name: string; from: string; to: string };
const EMPTY: Fields = { name: '', from: '', to: '' };

// The sheet's periods, each with its due-total (the figures of the claims that count whose date falls
// in it, skipping n/a; fixed when the period was closed), its closed yes/no and the button that
// closes it, and the form that adds one.
function Periods() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const periods = usePeriods(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const settings = useSettings(sheet).data;
  const floor = settings?.floor;
  const today = useSheetToday(sheet);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: (f: Fields) => postJson<Period>(`/api/sheets/${encodeURIComponent(sheet)}/periods`, { ...f, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet) });
      setFields(EMPTY);
    },
  });
  const submit = useSubmitOnce(save);

  // The server refuses closing a period that is already closed; the error shows on that period.
  const close = useMutation({
    mutationFn: (name: string) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheet)}/periods/${encodeURIComponent(name)}/close`, { user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
  });
  const closePeriod = (name: string) => {
    close.reset();
    close.mutate(name);
  };

  const set = (key: keyof Fields) => (e: ChangeEvent<HTMLInputElement>) => {
    setFields({ ...fields, [key]: e.target.value });
    save.reset();
  };

  const gradesOf = new Map((people.data ?? []).map((p) => [p.id, p.grades]));
  const periodDue = (period: Period) =>
    dueTotal(
      live(claims.data ?? [], people.data)
        .filter((c) => counts(c) && contains(period, c.date) && gradesOf.has(c.personId))
        .map((c) => claimDue(c, rates.data, gradesOf.get(c.personId)!, floor, lateOf(settings, today))),
    );

  return (
    <section data-testid="periods">
      <h2>Periods</h2>
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due total</th>
            <th>Closed</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{p.closed && p.total !== null ? p.total : periodDue(p)}</td>
              <td data-testid="period-closed">{p.closed ? 'yes' : 'no'}</td>
              <td>
                <button type="button" data-testid="period-close" disabled={close.isPending} onClick={() => closePeriod(p.name)}>
                  Close
                </button>
                {close.isError && close.variables === p.name && (
                  <span data-testid="period-error" role="alert">
                    {' '}
                    {close.error instanceof HttpError && close.error.reason ? `${close.error.reason}.` : 'The period was refused.'}
                  </span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <form
        data-testid="period-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(fields);
        }}
      >
        <input data-testid="period-form-name" placeholder="Name" value={fields.name} onChange={set('name')} />
        <input data-testid="period-form-from" placeholder="From (yyyy-mm-dd)" value={fields.from} onChange={set('from')} />
        <input data-testid="period-form-to" placeholder="To (yyyy-mm-dd)" value={fields.to} onChange={set('to')} />
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {save.isError && (
          <span data-testid="period-form-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The period was refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 45, Component: Periods });
