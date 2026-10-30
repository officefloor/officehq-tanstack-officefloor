import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { countsLive, useClaims } from '../claims/useClaims';
import { usePeople } from '../people/usePeople';
import { claimDue, formatCents, toCents } from '../rates/due';
import { rateOn, useRates } from '../rates/useRates';
import { periodOf, periodsKey, usePeriods, type Period } from './usePeriods';

// The sheet's periods, each with its dates and due-total (the dues of the claims that count whose
// date falls in it, leaving out a held person's OPEN claims; a claim whose due is n/a adds nothing), and the form that adds one (a name not
// already on the sheet, a from-date on or before the to-date, not overlapping another period). A
// closed period shows the due-total fixed when it was closed, whatever the rates do later.
export function PeriodPanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const periods = usePeriods(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
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
  const close = useMutation({
    mutationFn: (name: string) =>
      postJson<Period[]>(
        `/api/sheets/${encodeURIComponent(sheet)}/periods/${encodeURIComponent(name)}/close`,
        { user },
      ),
    onSuccess: (saved) => queryClient.setQueryData(periodsKey(sheet), saved),
  });
  const gradeOf = new Map(people.map((p) => [p.id, p.grade]));
  const heldOf = new Map(people.map((p) => [p.id, p.held]));
  const dueTotalOf = (period: Period) =>
    period.closed ? toCents(period.closedTotal) : claims
      .filter((c) => gradeOf.has(c.personId) && countsLive(c, heldOf.get(c.personId)!) && periodOf(periods, c.date) === period)
      .reduce((sum, c) => sum + (claimDue(c, rateOn(rates, gradeOf.get(c.personId)!, c.date)) ?? 0n), 0n);
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
                      close.reset();
                      close.mutate(p.name);
                    }}
                  >
                    Close
                  </button>
                )}
                {close.isError && close.variables === p.name && (
                  <span data-testid="period-error" role="alert"> Could not close the period.</span>
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
