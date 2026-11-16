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
 *  that count, each with its late charge (a person's OPEN or PENDING claims dated within a hold of theirs, not bound by a statement, do not) whose due-by falls in it, added up (skipping n/a); a closed period
 *  shows the due-total fixed when it was closed, a soft-closed one that fixed when it was soft-closed. */
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
      <PeriodGenerate sheetId={sheetId} user={user} />
      {periods.isError && <p data-testid="periods-load-error">Could not load periods.</p>}
      <table data-testid="periods-table">
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{periodDue(p, claims.data, people.data, rates.data, floor, late)}</td>
              <td data-testid="period-closed">{p.closed ? 'yes' : p.soft ? 'soft' : 'no'}</td>
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

type Range = { from: string; to: string };

/** Generates one period per calendar month from the from-date's month to the to-date's month, each from the first to
 *  the last day of its month and named yyyy-mm; a month whose name is already a period, or whose days overlap one, is
 *  left out. Shows how many were made; the count is cleared while a new press runs. A to-date before the from-date
 *  is refused. */
function PeriodGenerate({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [range, setRange] = useState<Range>({ from: '', to: '' });
  const generate = useMutation({
    mutationFn: (r: Range) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/generate`, { ...r, by: user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });
  const change = (r: Partial<Range>) => {
    setRange((was) => ({ ...was, ...r }));
    generate.reset();
  };
  return (
    <p>
      Generate monthly periods{' '}
      <label>
        From <input type="date" data-testid="period-generate-from" value={range.from} onChange={(e) => change({ from: e.target.value })} />
      </label>{' '}
      <label>
        To <input type="date" data-testid="period-generate-to" value={range.to} onChange={(e) => change({ to: e.target.value })} />
      </label>{' '}
      <button
        type="button"
        data-testid="period-generate"
        disabled={generate.isPending}
        onClick={() => {
          generate.reset();
          generate.mutate(range);
        }}
      >
        Generate
      </button>
      {generate.isSuccess && (
        <>
          {' '}
          Made <span data-testid="period-generated">{generate.data.made}</span>
        </>
      )}
      {generate.isError && <span data-testid="period-generate-error"> The to-date may not be before the from-date.</span>}
    </p>
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

/** Closes a period: its due-total is fixed as it is now. Soft-closes an open one: its due-total is fixed as it is now, but
 *  claims dated inside it can still change (both by the roles the close permission allows). Reopens a closed or soft-closed one (the roles the reopen permission allows; a steward while blank): its due-total is
 *  live again. A refused action shows the period's error; the next action on the period clears it. */
function PeriodClose({ sheetId, user, period }: { sheetId: string; user: string; period: Period }) {
  const queryClient = useQueryClient();
  const act = (action: 'close' | 'soft-close' | 'reopen') =>
    postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/${period.id}/${action}`, { by: user });
  const onSuccess = () => {
    void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
  };
  const close = useMutation({ mutationFn: () => act('close'), onSuccess });
  const softClose = useMutation({ mutationFn: () => act('soft-close'), onSuccess });
  const reopen = useMutation({ mutationFn: () => act('reopen'), onSuccess });
  const pending = close.isPending || softClose.isPending || reopen.isPending;
  const resetAll = () => {
    close.reset();
    softClose.reset();
    reopen.reset();
  };
  return (
    <>
      <button
        type="button"
        data-testid="period-close"
        disabled={pending}
        onClick={() => {
          resetAll();
          close.mutate();
        }}
      >
        Close
      </button>{' '}
      <button
        type="button"
        data-testid="period-soft-close"
        disabled={pending}
        onClick={() => {
          resetAll();
          softClose.mutate();
        }}
      >
        Soft close
      </button>{' '}
      <button
        type="button"
        data-testid="period-reopen"
        disabled={pending}
        onClick={() => {
          resetAll();
          reopen.mutate();
        }}
      >
        Reopen
      </button>
      {close.isError && <span data-testid="period-error"> This period is already closed, or your role may not close a period.</span>}
      {softClose.isError && <span data-testid="period-error"> Only an open period can be soft-closed, by a role the sheet lets close.</span>}
      {reopen.isError && <span data-testid="period-error"> Only a role the sheet lets reopen (a steward unless set) can reopen a period, and only a closed or soft-closed one.</span>}
    </>
  );
}
