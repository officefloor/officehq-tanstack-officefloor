import { type ChangeEvent, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useRates } from '../rates/rates';
import { floorOf, useSettings } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { lateOf } from '../overdue/overdue';
import { type Period, periodDue, periodsKey, usePeriods } from './periods';

type Fields = { name: string; from: string; to: string };
const EMPTY: Fields = { name: '', from: '', to: '' };

type Range = { from: string; to: string };
type GenerateResult = { generated: number };

// The sheet's periods, each with its due-total (the figures of the claims whose date falls in it and that
// count by their status at its end, skipping n/a; fixed when the period was closed), its closed yes/no and the buttons that
// close it and reopen it (as the sheet's permission matrix allows), and the form that adds one.
function Periods() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const periods = usePeriods(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const settings = useSettings(sheet).data;
  const floor = floorOf(settings);
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

  // Generates one period per calendar month from the from-date's month to the to-date's; the server
  // leaves out a month whose name is taken or whose days overlap a period, and the count made shows
  // once it answers (gone while it runs).
  const [range, setRange] = useState<Range>({ from: '', to: '' });
  const generate = useMutation({
    mutationFn: (r: Range) =>
      postJson<GenerateResult>(`/api/sheets/${encodeURIComponent(sheet)}/periods/generate`, { ...r, user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
  });
  const submitGenerate = useSubmitOnce(generate);
  const setBound = (key: keyof Range) => (e: ChangeEvent<HTMLInputElement>) => {
    setRange({ ...range, [key]: e.target.value });
    generate.reset();
  };

  // The server refuses closing a period that is already closed, and reopening one that is not closed
  // or by a role the sheet's permission matrix does not allow; the error shows on that period.
  type Action = { name: string; action: 'close' | 'reopen' };
  const act = useMutation({
    mutationFn: ({ name, action }: Action) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheet)}/periods/${encodeURIComponent(name)}/${action}`, { user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
  });
  const periodAction = (name: string, action: Action['action']) => {
    act.reset();
    act.mutate({ name, action });
  };

  const set = (key: keyof Fields) => (e: ChangeEvent<HTMLInputElement>) => {
    setFields({ ...fields, [key]: e.target.value });
    save.reset();
  };


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
              <td data-testid="period-due-total">{periodDue(p, claims.data, people.data, rates.data, floor, lateOf(settings, today))}</td>
              <td data-testid="period-closed">{p.closed ? 'yes' : 'no'}</td>
              <td>
                <button type="button" data-testid="period-close" disabled={act.isPending} onClick={() => periodAction(p.name, 'close')}>
                  Close
                </button>{' '}
                <button type="button" data-testid="period-reopen" disabled={act.isPending} onClick={() => periodAction(p.name, 'reopen')}>
                  Reopen
                </button>
                {act.isError && act.variables?.name === p.name && (
                  <span data-testid="period-error" role="alert">
                    {' '}
                    {act.error instanceof HttpError && act.error.reason ? `${act.error.reason}.` : 'The period was refused.'}
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
      <form
        data-testid="period-generate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitGenerate(range);
        }}
      >
        <input data-testid="period-generate-from" placeholder="From (yyyy-mm-dd)" value={range.from} onChange={setBound('from')} />
        <input data-testid="period-generate-to" placeholder="To (yyyy-mm-dd)" value={range.to} onChange={setBound('to')} />
        <button type="submit" data-testid="period-generate" disabled={generate.isPending}>
          Generate monthly periods
        </button>
        {generate.isSuccess && (
          <>
            {' '}
            Made <strong data-testid="period-generated">{generate.data.generated}</strong>
          </>
        )}
        {generate.isError && (
          <span data-testid="period-generate-error" role="alert">
            {' '}
            {generate.error instanceof HttpError && generate.error.reason ? `${generate.error.reason}.` : 'The generate was refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 45, Component: Periods });
