import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { gradeOn, usePeople } from '../people/people';
import { rateOf, twoDecimals, useRates } from '../rates/rates';
import { useFloorCents } from '../settings/settings';
import { claimDueCents, counted, useClaims, useLate } from '../claims/claims';
import { closePeriodPath, periodOf, periodsKey, periodsPath, usePeriods } from './periods';
import type { Period } from './periods';

// One period with its due-total (fixed once closed) and closed flag, and the button that closes it. The server
// refuses closing a period already closed; the row shows that error until its next action.
function PeriodRow({ sheet, user, period, dueCents }: { sheet: string; user: string; period: Period; dueCents: bigint }) {
  const queryClient = useQueryClient();
  const close = useMutation({
    mutationFn: () => postJson<Period[]>(byUser(closePeriodPath(sheet, period.name), user), {}),
    onSuccess: (updated) => queryClient.setQueryData(periodsKey(sheet), updated),
  });
  const act = useSubmitOnce(close);

  return (
    <tr data-testid={`period-row-${period.name}`}>
      <td data-testid="period-name">{period.name}</td>
      <td data-testid="period-from">{period.from}</td>
      <td data-testid="period-to">{period.to}</td>
      <td data-testid="period-due-total">{period.closed && period.closedTotal !== null ? period.closedTotal : twoDecimals(dueCents)}</td>
      <td data-testid="period-closed">{period.closed ? 'yes' : 'no'}</td>
      <td>
        <button type="button" data-testid="period-close" disabled={close.isPending} onClick={() => act(undefined)}>
          Close
        </button>
        {close.isError && (
          <span data-testid="period-error" role="alert">
            {' '}This period cannot be closed: it is already closed.
          </span>
        )}
      </td>
    </tr>
  );
}

type Draft = { name: string; from: string; to: string };
const EMPTY: Draft = { name: '', from: '', to: '' };

// The sheet's periods, each with its due-total: the due of the counted (OPEN and SETTLED, but not a held person's OPEN) claims whose date
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

  const byId = new Map(people.map((p) => [p.id, p]));
  const totals = new Map<string, bigint>();
  for (const c of counted(all, people)) {
    const period = periodOf(periods, c.date);
    const person = byId.get(c.person);
    if (period === undefined || person === undefined) continue;
    const grade = gradeOn(person, c.date);
    const due = claimDueCents(c, rateOf(rates, grade, c.date), floor, late) ?? 0n;
    totals.set(period.name, (totals.get(period.name) ?? 0n) + due);
  }

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
