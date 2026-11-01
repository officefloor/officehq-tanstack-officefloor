import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { twoDecimals, useRates } from '../rates/rates';
import { useFloorCents } from '../settings/settings';
import { useClaims, useLate } from '../claims/claims';
import {
  closePeriodPath,
  generatePeriodsPath,
  periodDueCents,
  shownDueTotal,
  reopenPeriodPath,
  softClosePeriodPath,
  periodsKey,
  periodsPath,
  usePeriods,
} from './periods';
import type { Generated, Period } from './periods';

// One period with its due-total (fixed once closed or soft-closed) and closed flag (yes, soft or no), and the buttons
// that close, soft-close and reopen it. The server refuses closing a period already closed, soft-closing one already
// closed or soft-closed, and reopening one neither; and any of them by a role the permission matrix does not allow;
// the row shows that error until its next action.
function PeriodRow({ sheet, user, period, dueCents }: { sheet: string; user: string; period: Period; dueCents: bigint }) {
  const queryClient = useQueryClient();
  const close = useMutation({
    mutationFn: () => postJson<Period[]>(byUser(closePeriodPath(sheet, period.name), user), {}),
    onSuccess: (updated) => queryClient.setQueryData(periodsKey(sheet), updated),
  });
  const reopen = useMutation({
    mutationFn: () => postJson<Period[]>(byUser(reopenPeriodPath(sheet, period.name), user), {}),
    onSuccess: (updated) => queryClient.setQueryData(periodsKey(sheet), updated),
  });
  const softClose = useMutation({
    mutationFn: () => postJson<Period[]>(byUser(softClosePeriodPath(sheet, period.name), user), {}),
    onSuccess: (updated) => queryClient.setQueryData(periodsKey(sheet), updated),
  });
  const closeOnce = useSubmitOnce(close);
  const softCloseOnce = useSubmitOnce(softClose);
  const pending = close.isPending || softClose.isPending || reopen.isPending;
  const reopenOnce = useSubmitOnce(reopen);

  return (
    <tr data-testid={`period-row-${period.name}`}>
      <td data-testid="period-name">{period.name}</td>
      <td data-testid="period-from">{period.from}</td>
      <td data-testid="period-to">{period.to}</td>
      <td data-testid="period-due-total">{shownDueTotal(period, dueCents)}</td>
      <td data-testid="period-closed">{period.closed ? 'yes' : period.soft ? 'soft' : 'no'}</td>
      <td>
        <button
          type="button"
          data-testid="period-close"
          disabled={pending}
          onClick={() => {
            softClose.reset();
            reopen.reset();
            closeOnce(undefined);
          }}
        >
          Close
        </button>{' '}
        <button
          type="button"
          data-testid="period-soft-close"
          disabled={pending}
          onClick={() => {
            close.reset();
            reopen.reset();
            softCloseOnce(undefined);
          }}
        >
          Soft close
        </button>{' '}
        <button
          type="button"
          data-testid="period-reopen"
          disabled={pending}
          onClick={() => {
            close.reset();
            softClose.reset();
            reopenOnce(undefined);
          }}
        >
          Reopen
        </button>
        {close.isError && (
          <span data-testid="period-error" role="alert">
            {' '}This period cannot be closed: it is already closed, or your role may not close it.
          </span>
        )}
        {softClose.isError && (
          <span data-testid="period-error" role="alert">
            {' '}This period cannot be soft-closed: it is already closed or soft-closed, or your role may not close it.
          </span>
        )}
        {reopen.isError && (
          <span data-testid="period-error" role="alert">
            {' '}This period cannot be reopened: it is not closed or soft-closed, or your role may not reopen it.
          </span>
        )}
      </td>
    </tr>
  );
}

type Draft = { name: string; from: string; to: string };
const EMPTY: Draft = { name: '', from: '', to: '' };

type Range = { from: string; to: string };
const NO_RANGE: Range = { from: '', to: '' };

// Generates one period per calendar month from the from-date's month to the to-date's, named yyyy-mm; the server leaves
// out a month whose name is already a period or that overlaps one, and refuses a to-date before the from-date. Shows how
// many were made.
function GeneratePeriods({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const [range, setRange] = useState<Range>(NO_RANGE);
  const generate = useMutation({
    mutationFn: (body: Range) => postJson<Generated>(byUser(generatePeriodsPath(sheet), user), body),
    onSuccess: (result) => {
      queryClient.setQueryData(periodsKey(sheet), result.periods);
      setRange(NO_RANGE);
    },
  });
  const send = useSubmitOnce(generate);

  const change = (next: Partial<Range>) => {
    setRange((r) => ({ ...r, ...next }));
    generate.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(range);
  };

  return (
    <form onSubmit={submit}>
      <label>
        Generate monthly from{' '}
        <input
          data-testid="period-generate-from"
          placeholder="yyyy-mm-dd (blank: today)"
          value={range.from}
          onChange={(e) => change({ from: e.target.value })}
        />
      </label>{' '}
      <label>
        to{' '}
        <input
          data-testid="period-generate-to"
          placeholder="yyyy-mm-dd"
          value={range.to}
          onChange={(e) => change({ to: e.target.value })}
        />
      </label>{' '}
      <button type="submit" data-testid="period-generate" disabled={generate.isPending}>
        Generate periods
      </button>
      {generate.isSuccess && (
        <>
          {' '}Made <strong data-testid="period-generated">{generate.data.made}</strong>
        </>
      )}
      {generate.isError && (
        <span data-testid="period-generate-error" role="alert">
          {' '}Nothing was made: generating needs valid from and to dates, the to on or after the from.
        </span>
      )}
    </form>
  );
}

// The sheet's periods, each with its due-total: the due now of the claims that counted at the end of its last day (see countedOn) whose date
// falls in it (a claim with no rate adds nothing), fixed when the period is closed. The form adds one; the server refuses a name already on
// the sheet, a bad date, a to-date before the from-date, or dates that overlap another period.
function Periods({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: periods = [] } = usePeriods(sheet);
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);
  const [draft, setDraft] = useState<Draft>(EMPTY);

  const save = useMutation({
    mutationFn: (body: Draft) => postJson<Period[]>(byUser(periodsPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(periodsKey(sheet), updated);
      setDraft(EMPTY);
    },
  });
  const send = useSubmitOnce(save);

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(draft);
  };

  const totals = periodDueCents(periods, all, people, rates, floor, late);

  return (
    <section>
      <h2>Periods</h2>
      <form onSubmit={submit}>
        <label>
          Name{' '}
          <input data-testid="period-form-name" value={draft.name} onChange={(e) => change({ name: e.target.value })} />
        </label>{' '}
        <label>
          From{' '}
          <input
            data-testid="period-form-from"
            placeholder="yyyy-mm-dd (blank: today)"
            value={draft.from}
            onChange={(e) => change({ from: e.target.value })}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            data-testid="period-form-to"
            placeholder="yyyy-mm-dd"
            value={draft.to}
            onChange={(e) => change({ to: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {save.isError && (
          <span data-testid="period-form-error" role="alert">
            {' '}A period needs a name not already on the sheet and valid from and to dates, the to on or after the from, that overlap no other period.
          </span>
        )}
      </form>
      <GeneratePeriods sheet={sheet} user={user} />
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due total</th>
            <th>Closed</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <PeriodRow key={p.name} sheet={sheet} user={user} period={p} dueCents={totals.get(p.name) ?? 0n} />
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 25, Component: Periods });
