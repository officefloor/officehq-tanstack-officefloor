import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { formatCents } from '../rates/due';
import { usePeriodDueTotal } from './usePeriodDueTotal';
import { useSettings } from '../settings/useSettings';
import { fiscalYearOf, periodsKey, usePeriods, type Period } from './usePeriods';

// The sheet's periods, each with its dates and due-total (the dues as shown now, late charges included, of the claims
// whose date falls in it that counted by the status they had at the end of its last day, leaving out a held person's OPEN claims; a claim whose due is n/a adds nothing), and the form that adds one (a name not
// already on the sheet, a from-date on or before the to-date, not overlapping another period, and an optional cap: a whole number of at
// least 1, the most OPEN or PENDING claims a person may have in it, blank for none). A
// closed period shows the due-total fixed when it was closed, whatever the rates do later; a steward
// can reopen it, making its due-total live again. A soft-closed period (shown closed: soft) has its figures fixed the
// same way, but claims dated in it can still be added and changed (one added then is a late entry); it can be closed
// (fixing the figures afresh) or reopened.
export function PeriodPanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const periods = usePeriods(sheet);
  const dueTotalOf = usePeriodDueTotal(sheet);
  // Each period shows the fiscal year its from-date falls in.
  const fiscalStart = Number(useSettings(sheet)?.fiscalStartMonth ?? '1') || 1;
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [cap, setCap] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { name: string; from: string; to: string; cap: string }) =>
      postJson<Period[]>(`/api/sheets/${encodeURIComponent(sheet)}/periods`, { ...body, user }),
    onSuccess: (saved) => {
      setName('');
      setFrom('');
      setTo('');
      setCap('');
      queryClient.setQueryData(periodsKey(sheet), saved);
    },
  });
  // Generate one period per calendar month from the from-date's month to the to-date's month (blank is
  // the sheet's today), each named yyyy-mm; a month already named or overlapping a period is left out.
  // Shows how many were made; pressing again removes the old count until the new one is in.
  const [genFrom, setGenFrom] = useState('');
  const [genTo, setGenTo] = useState('');
  const generate = useMutation({
    mutationFn: (body: { from: string; to: string }) =>
      postJson<{ generated: number; periods: Period[] }>(
        `/api/sheets/${encodeURIComponent(sheet)}/periods/generate`,
        { ...body, user },
      ),
    onSuccess: (saved) => queryClient.setQueryData(periodsKey(sheet), saved.periods),
  });
  // Generate a fiscal year's twelve monthly periods (from the sheet's fiscal start month of the year), as the
  // generator above does; its count shows in the same place.
  const [fyYear, setFyYear] = useState('');
  const generateFy = useMutation({
    mutationFn: (body: { year: string }) =>
      postJson<{ generated: number; periods: Period[] }>(
        `/api/sheets/${encodeURIComponent(sheet)}/periods/generate-fy`,
        { ...body, user },
      ),
    onSuccess: (saved) => queryClient.setQueryData(periodsKey(sheet), saved.periods),
  });
  const generated = generateFy.submittedAt > generate.submittedAt ? generateFy : generate;
  // Close a period (open or soft-closed), soft-close an open one, or reopen a closed or soft-closed one (each by the roles the sheet's permission allows; by default
  // close by clerk and steward, reopen by a steward only).
  const act = useMutation({
    mutationFn: ({ name, action }: { name: string; action: 'close' | 'soft-close' | 'reopen' }) =>
      postJson<Period[]>(
        `/api/sheets/${encodeURIComponent(sheet)}/periods/${encodeURIComponent(name)}/${action}`,
        { user },
      ),
    onSuccess: (saved) => queryClient.setQueryData(periodsKey(sheet), saved),
  });
  return (
    <>
      <table data-testid="periods">
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due</th>
            <th>Closed</th>
            <th>FY</th>
            <th>Cap</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{formatCents(dueTotalOf(p))}</td>
              <td data-testid="period-closed">{p.closed ? 'yes' : p.softClosed ? 'soft' : 'no'}</td>
              <td data-testid="period-fy">{fiscalYearOf(p.from, fiscalStart)}</td>
              <td data-testid="period-cap">{p.cap}</td>
              <td>
                {!p.closed && (
                  <button
                    type="button"
                    data-testid="period-close"
                    onClick={() => {
                      act.reset();
                      act.mutate({ name: p.name, action: 'close' });
                    }}
                  >
                    Close
                  </button>
                )}{' '}
                <button
                  type="button"
                  data-testid="period-soft-close"
                  onClick={() => {
                    act.reset();
                    act.mutate({ name: p.name, action: 'soft-close' });
                  }}
                >
                  Soft close
                </button>{' '}
                <button
                  type="button"
                  data-testid="period-reopen"
                  onClick={() => {
                    act.reset();
                    act.mutate({ name: p.name, action: 'reopen' });
                  }}
                >
                  Reopen
                </button>
                {act.isError && act.variables?.name === p.name && (
                  <span data-testid="period-error" role="alert">
                    {act.variables.action === 'reopen'
                      ? ' Could not reopen the period: only a role the sheet allows to reopen (by default a steward) can reopen it, and only while it is closed or soft-closed.'
                      : act.variables.action === 'soft-close'
                      ? ' Could not soft-close the period: only a role the sheet allows to close can soft-close it, and only while it is open.'
                      : ' Could not close the period: only a role the sheet allows to close can close it, and only while it is open or soft-closed.'}
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
          submitOnce(() => {
            add.reset();
            return add.mutateAsync({ name: name.trim(), from: from.trim(), to: to.trim(), cap: cap.trim() });
          });
        }}
      >
        <input
          data-testid="period-form-name"
          value={name}
          placeholder="Period name"
          onChange={(e) => {
            setName(e.target.value);
            add.reset();
          }}
        />{' '}
        <input
          data-testid="period-form-from"
          value={from}
          placeholder="From (YYYY-MM-DD)"
          onChange={(e) => {
            setFrom(e.target.value);
            add.reset();
          }}
        />{' '}
        <input
          data-testid="period-form-to"
          value={to}
          placeholder="To (YYYY-MM-DD)"
          onChange={(e) => {
            setTo(e.target.value);
            add.reset();
          }}
        />{' '}
        <input
          data-testid="period-form-cap"
          value={cap}
          placeholder="Cap per person (blank for none)"
          onChange={(e) => {
            setCap(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="period-form-submit">Add period</button>
        {add.isError && <span data-testid="period-form-error" role="alert"> Could not add the period: it needs a name not already on the sheet and a from-date on or before its to-date (YYYY-MM-DD), not overlapping another period, and a cap that is blank or a whole number of at least 1.</span>}
      </form>
      <form
        data-testid="period-generate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            generate.reset();
            generateFy.reset();
            return generate.mutateAsync({ from: genFrom.trim(), to: genTo.trim() });
          });
        }}
      >
        <input
          data-testid="period-generate-from"
          value={genFrom}
          placeholder="From (YYYY-MM-DD)"
          onChange={(e) => {
            setGenFrom(e.target.value);
            generate.reset();
          }}
        />{' '}
        <input
          data-testid="period-generate-to"
          value={genTo}
          placeholder="To (YYYY-MM-DD)"
          onChange={(e) => {
            setGenTo(e.target.value);
            generate.reset();
          }}
        />{' '}
        <button type="submit" data-testid="period-generate">Generate monthly periods</button>
        {generated.isSuccess && (
          <>
            {' '}
            Made <span data-testid="period-generated">{generated.data.generated}</span>
          </>
        )}
        {generated.isError && (
          <span data-testid="period-generate-error" role="alert">
            {' '}
            {generated === generateFy
              ? 'Could not generate the fiscal year: it must be a year (YYYY).'
              : 'Could not generate periods: the to-date must be on or after the from-date (YYYY-MM-DD).'}
          </span>
        )}
      </form>
      <form
        data-testid="period-generate-fy-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            generate.reset();
            generateFy.reset();
            return generateFy.mutateAsync({ year: fyYear.trim() });
          });
        }}
      >
        <input
          data-testid="period-generate-fy-year"
          value={fyYear}
          placeholder="Fiscal year (YYYY)"
          onChange={(e) => {
            setFyYear(e.target.value);
            generateFy.reset();
          }}
        />{' '}
        <button type="submit" data-testid="period-generate-fy">Generate fiscal year</button>
      </form>
    </>
  );
}
