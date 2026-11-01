import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { rateOf, twoDecimals, useRates } from '../rates/rates';
import { claimDueCents, counted, useClaims } from '../claims/claims';
import { periodOf, periodsKey, periodsPath, usePeriods } from './periods';
import type { Period } from './periods';

type Draft = { name: string; from: string; to: string };
const EMPTY: Draft = { name: '', from: '', to: '' };

// The sheet's periods, each with its due-total: the due of the counted (OPEN and SETTLED) claims whose date
// falls in it (a claim with no rate adds nothing). The form adds one; the server refuses a name already on
// the sheet, a bad date, a to-date before the from-date, or dates that overlap another period.
function Periods({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: periods = [] } = usePeriods(sheet);
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
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

  const grades = new Map(people.map((p) => [p.id, p.grade]));
  const totals = new Map<string, bigint>();
  for (const c of counted(all)) {
    const period = periodOf(periods, c.date);
    const grade = grades.get(c.person);
    if (period === undefined || grade === undefined) continue;
    const due = claimDueCents(c, rateOf(rates, grade, c.date)) ?? 0n;
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
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{twoDecimals(totals.get(p.name) ?? 0n)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 25, Component: Periods });
