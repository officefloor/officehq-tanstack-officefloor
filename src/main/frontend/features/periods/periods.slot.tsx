import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useClaims } from '../claims/claims';
import { usePeople, type Person } from '../people/people';
import { useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { periodDue, periodsKey, personPeriodDue, usePeriods, type Period } from './periods';

type Values = { name: string; from: string; to: string };
const EMPTY: Values = { name: '', from: '', to: '' };

/** The sheet's periods: the form that adds one, and each period with its due-total — the figures of the claims
 *  that count, each with its late charge (a person's OPEN or PENDING claims dated within a hold of theirs, not bound by a statement, do not) whose date falls in it, added up (skipping n/a); a closed period
 *  shows the due-total fixed when it was closed. */
function Periods({ sheetId, user }: { sheetId: string; user: string }) {
  const periods = usePeriods(sheetId);
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const queryClient = useQueryClient();
  const [values, setValues] = useState<Values>(EMPTY);

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: (v: Values) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, { ...v, by: user }),
    onSuccess: () => {
      setValues(EMPTY);
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onSettled: submit.end,
  });
  const change = (v: Partial<Values>) => {
    setValues((was) => ({ ...was, ...v }));
    save.reset();
  };

  return (
    <section data-testid="periods">
      <h3>Periods</h3>
      <form
        data-testid="period-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          save.reset();
          save.mutate(values);
        }}
      >
        <input data-testid="period-form-name" placeholder="Name" value={values.name} onChange={(e) => change({ name: e.target.value })} />{' '}
        <label>
          From <input type="date" data-testid="period-form-from" value={values.from} onChange={(e) => change({ from: e.target.value })} />
        </label>{' '}
        <label>
          To <input type="date" data-testid="period-form-to" value={values.to} onChange={(e) => change({ to: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {save.isError && (
          <span data-testid="period-form-error">
            A period needs a name not already on the sheet, and dates from on or before to that overlap no other period.
          </span>
        )}
      </form>
      {periods.isError && <p data-testid="periods-load-error">Could not load periods.</p>}
      <table data-testid="periods-table">
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{periodDue(p, claims.data, people.data, rates.data, floor, late)}</td>
              <td data-testid="period-closed">{p.closed ? 'yes' : 'no'}</td>
              <td>
                <PeriodClose sheetId={sheetId} user={user} period={p} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

/** One header cell per period of the sheet. */
function PeriodsHead({ sheetId }: { sheetId: string }) {
  const periods = usePeriods(sheetId);
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <th key={p.id}>{p.name} due</th>
      ))}
    </>
  );
}

/** A person's due-total for each period of the sheet (see personPeriodDue). */
function PersonPeriods({ sheetId, person }: { sheetId: string; person: Person }) {
  const periods = usePeriods(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <td key={p.id} data-testid={`person-period-${p.name}-due-total`}>
          {personPeriodDue(p, person, claims.data, rates.data, floor, late)}
        </td>
      ))}
    </>
  );
}

export const contributions = [
  SheetPage.fill({ order: 7, Component: Periods }),
  PeopleHead.fill({ order: 22, Component: PeriodsHead }),
  PersonRow.fill({ order: 22, Component: PersonPeriods }),
];

/** Closes a period: its due-total is fixed as it is now. Reopens a closed one (a steward only): its due-total is
 *  live again. A refused action shows the period's error; the next action on the period clears it. */
function PeriodClose({ sheetId, user, period }: { sheetId: string; user: string; period: Period }) {
  const queryClient = useQueryClient();
  const act = (action: 'close' | 'reopen') =>
    postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/${period.id}/${action}`, { by: user });
  const onSuccess = () => {
    void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
  };
  const close = useMutation({ mutationFn: () => act('close'), onSuccess });
  const reopen = useMutation({ mutationFn: () => act('reopen'), onSuccess });
  const pending = close.isPending || reopen.isPending;
  return (
    <>
      <button
        type="button"
        data-testid="period-close"
        disabled={pending}
        onClick={() => {
          close.reset();
          reopen.reset();
          close.mutate();
        }}
      >
        Close
      </button>{' '}
      <button
        type="button"
        data-testid="period-reopen"
        disabled={pending}
        onClick={() => {
          close.reset();
          reopen.reset();
          reopen.mutate();
        }}
      >
        Reopen
      </button>
      {close.isError && <span data-testid="period-error"> This period is already closed.</span>}
      {reopen.isError && <span data-testid="period-error"> Only a steward can reopen a period, and only a closed one.</span>}
    </>
  );
}
