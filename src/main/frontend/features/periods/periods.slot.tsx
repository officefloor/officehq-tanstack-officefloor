import { useState, type ChangeEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, useClaims } from '../claims/useClaims';
import { usePeople } from '../people/people.slot';
import { useUser } from '../session/useSession';
import { claimDueCents, showHundredths, useRates } from '../rates/useRates';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** A period of the sheet: its name and its from and to dates (yyyy-mm-dd), both inclusive. */
export type Period = { id: number; name: string; from: string; to: string };

/** The key every feature reads the sheet's periods under. */
export const periodsKey = (sheetId: string) => ['sheet', sheetId, 'periods'] as const;

/** The sheet's periods, by from-date; no two overlap. */
export function usePeriods(sheetId: string) {
  return useQuery({
    queryKey: periodsKey(sheetId),
    queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`),
  });
}

/** The period whose dates contain the date (ISO dates compare as text), or none. */
export function periodOn(periods: readonly Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}

// The period form: adds a period with a name unique on the sheet and a from and to date, not
// overlapping another period. Every period is listed under it with its due-total: the due figures of
// the counted claims whose date falls in it, added up.
function PeriodsPanel({ sheetId }: { sheetId: string; user: string }) {
  const periods = usePeriods(sheetId);
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: async (body: { name: string; from: string; to: string }) => {
      if (body.name.trim() === '') throw new Error('Enter a period name');
      if (body.to.trim() === '') throw new Error('Enter a to-date');
      if ((body.from.trim() !== '' && !DATE.test(body.from.trim())) || !DATE.test(body.to.trim())) {
        throw new Error('Enter a date as YYYY-MM-DD');
      }
      // `by` is the signed-in user the change is recorded against.
      return postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, {
        name: body.name.trim(), from: body.from.trim(), to: body.to.trim(), by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
      setName('');
      setFrom('');
      setTo('');
    },
  });
  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    save.reset();
  };

  const gradeOf = new Map((people.data ?? []).map((p) => [p.id, p.grade]));
  const dueTotal = (p: Period): bigint => {
    let sum = 0n;
    for (const c of claims.data ?? []) {
      if (!counts(c) || c.date < p.from || c.date > p.to) continue;
      sum += claimDueCents(rates.data ?? [], gradeOf.get(c.personId), c) ?? 0n;
    }
    return sum;
  };

  const submit = useSubmitOnce(save);
  return (
    <section data-testid="periods-panel">
      <h2>Periods</h2>
      <form
        data-testid="period-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ name, from, to });
        }}
      >
        <input data-testid="period-form-name" placeholder="Name" value={name} onChange={field(setName)} />{' '}
        <label>
          From <input type="date" data-testid="period-form-from" value={from} onChange={field(setFrom)} />
        </label>{' '}
        <label>
          To <input type="date" data-testid="period-form-to" value={to} onChange={field(setTo)} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {save.isError && (
          <p data-testid="period-form-error" role="alert">
            {save.error.message}
          </p>
        )}
      </form>
      {periods.isError && (
        <p data-testid="periods-error" role="alert">
          {periods.error.message}
        </p>
      )}
      <table data-testid="period-list">
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{showHundredths(dueTotal(p))}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

// The period a claim belongs to: the one whose dates contain its date; blank when none does.
function ClaimPeriod({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: { date: string } }) {
  const periods = usePeriods(sheetId);
  return <span data-testid="claim-period">{periodOn(periods.data ?? [], claim.date)?.name ?? ''}</span>;
}

export const contributions = [
  SheetBody.fill({ order: 7, Component: PeriodsPanel }),
  ClaimCells.fill({ order: 30, Component: ClaimPeriod }),
];
