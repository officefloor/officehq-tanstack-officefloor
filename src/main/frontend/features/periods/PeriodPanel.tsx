import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { formatCents } from '../rates/due';
import { usePeriodDueTotal } from './usePeriodDueTotal';
import { periodsKey, usePeriods, type Period } from './usePeriods';

// The sheet's periods, each with its dates and due-total (the dues as shown now, late charges included, of the claims
// whose date falls in it that counted by the status they had at the end of its last day, leaving out a held person's OPEN claims; a claim whose due is n/a adds nothing), and the form that adds one (a name not
// already on the sheet, a from-date on or before the to-date, not overlapping another period). A
// closed period shows the due-total fixed when it was closed, whatever the rates do later; a steward
// can reopen it, making its due-total live again.
export function PeriodPanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const periods = usePeriods(sheet);
  const dueTotalOf = usePeriodDueTotal(sheet);
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { name: string; from: string; to: string }) =>
      postJson<Period[]>(`/api/sheets/${encodeURIComponent(sheet)}/periods`, { ...body, user }),
    onSuccess: (saved) => {
      setName('');
      setFrom('');
      setTo('');
      queryClient.setQueryData(periodsKey(sheet), saved);
    },
  });
  // Close a period, or reopen a closed one (a steward only).
  const act = useMutation({
    mutationFn: ({ name, action }: { name: string; action: 'close' | 'reopen' }) =>
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
              <td data-testid="period-closed">{p.closed ? 'yes' : 'no'}</td>
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
                      ? ' Could not reopen the period: only a steward can reopen it, and only while it is closed.'
                      : ' Could not close the period.'}
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
            return add.mutateAsync({ name: name.trim(), from: from.trim(), to: to.trim() });
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
        <button type="submit" data-testid="period-form-submit">Add period</button>
        {add.isError && <span data-testid="period-form-error" role="alert"> Could not add the period: it needs a name not already on the sheet and a from-date on or before its to-date (YYYY-MM-DD), not overlapping another period.</span>}
      </form>
    </>
  );
}
