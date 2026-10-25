import { useState, type ChangeEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, useClaims } from '../claims/useClaims';
import { gradeOn, useHeld, usePeople } from '../people/people.slot';
import { useUser } from '../session/useSession';
import { claimDueCents, showHundredths, useRates } from '../rates/useRates';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useFloorCents, useLate } from '../access/useAccess';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** A period of the sheet: its name and its from and to dates (yyyy-mm-dd), both inclusive; whether it is
 *  closed, the due-total fixed when it was closed (null while open), and each person's due-total for it fixed
 *  then, by person id (empty while open; a person added later has none). */
export type Period = {
  id: number; name: string; from: string; to: string; closed: boolean; closedTotal: string | null;
  personTotals: Record<string, string>;
};

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

/** A person's live due-total for a period in cents: the due figures of their counted claims whose date falls in it,
 *  added up. With no person given, every person's together: the period's own due-total. */
export function usePeriodDueTotal(sheetId: string): (p: Period, personId?: number) => bigint {
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const held = useHeld(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloorCents(sheetId);
  const late = useLate(sheetId);
  const personOf = new Map((people.data ?? []).map((p) => [p.id, p]));
  return (p: Period, personId?: number): bigint => {
    let sum = 0n;
    for (const c of claims.data ?? []) {
      if (personId !== undefined && c.personId !== personId) continue;
      if (!counts(c, held) || c.date < p.from || c.date > p.to) continue;
      sum += claimDueCents(rates.data ?? [], gradeOn(personOf.get(c.personId), c.date), c, floor, late) ?? 0n;
    }
    return sum;
  };
}

/** A period's due-total as shown: the one fixed when it was closed; while open, the live figure. */
export function showPeriodDueTotal(p: Period, live: bigint): string {
  return p.closed && p.closedTotal != null ? p.closedTotal : showHundredths(live);
}

/** A person's due-total for a period as shown: the one fixed when it was closed (0.00 for a person added since);
 *  while open, the live figure. */
export function showPersonPeriodDueTotal(p: Period, personId: number, live: bigint): string {
  return p.closed ? (p.personTotals?.[String(personId)] ?? '0.00') : showHundredths(live);
}

// The period form: adds a period with a name unique on the sheet and a from and to date, not
// overlapping another period. Every period is listed under it with its due-total: the due figures of
// the counted claims whose date falls in it, added up.
function PeriodsPanel({ sheetId }: { sheetId: string; user: string }) {
  const periods = usePeriods(sheetId);
  const dueTotal = usePeriodDueTotal(sheetId);
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
            <th>Closed</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <PeriodRow key={p.id} sheetId={sheetId} period={p} dueTotal={dueTotal(p)} />
          ))}
        </tbody>
      </table>
    </section>
  );
}

// A period's row. A closed period shows the due-total fixed when it was closed; an open one, the live
// figure. Closing is refused with the row's error when someone else closed it first. Reopening (a
// steward only) makes the due-total live again; anyone else, or a period not closed, is refused with
// the row's error. Each action clears the error the other left.
function PeriodRow({ sheetId, period: p, dueTotal }: { sheetId: string; period: Period; dueTotal: bigint }) {
  const [user] = useUser();
  const queryClient = useQueryClient();
  const close = useMutation({
    mutationFn: () => postJson<Period>(
      `/api/sheets/${encodeURIComponent(sheetId)}/periods/${p.id}/close`, { by: user ?? '' }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
  });
  const reopen = useMutation({
    mutationFn: () => postJson<Period>(
      `/api/sheets/${encodeURIComponent(sheetId)}/periods/${p.id}/reopen`, { by: user ?? '' }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
  });
  const submitClose = useSubmitOnce(close);
  const submitReopen = useSubmitOnce(reopen);
  const error = close.error ?? reopen.error;
  return (
    <tr data-testid={`period-row-${p.name}`}>
      <td data-testid="period-name">{p.name}</td>
      <td data-testid="period-from">{p.from}</td>
      <td data-testid="period-to">{p.to}</td>
      <td data-testid="period-due-total">
        {showPeriodDueTotal(p, dueTotal)}
      </td>
      <td data-testid="period-closed">{p.closed ? 'yes' : 'no'}</td>
      <td>
        {!p.closed && (
          <button
            type="button"
            data-testid="period-close"
            disabled={close.isPending}
            onClick={() => {
              reopen.reset();
              submitClose(undefined);
            }}
          >
            Close
          </button>
        )}{' '}
        <button
          type="button"
          data-testid="period-reopen"
          disabled={reopen.isPending}
          onClick={() => {
            close.reset();
            submitReopen(undefined);
          }}
        >
          Reopen
        </button>
        {error && (
          <p data-testid="period-error" role="alert">
            {error.message}
          </p>
        )}
      </td>
    </tr>
  );
}

// One column per period on the people table: the person's due-total for it.
function PersonPeriodHeads({ sheetId }: { sheetId: string; user: string }) {
  const periods = usePeriods(sheetId);
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <th key={p.id}>{p.name} due</th>
      ))}
    </>
  );
}

function PersonPeriodCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const periods = usePeriods(sheetId);
  const dueTotal = usePeriodDueTotal(sheetId);
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <td key={p.id} data-testid={`person-period-${p.name}-due-total`}>
          {showPersonPeriodDueTotal(p, personId, dueTotal(p, personId))}
        </td>
      ))}
    </>
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
  PersonHeads.fill({ order: 26, Component: PersonPeriodHeads }),
  PersonCells.fill({ order: 26, Component: PersonPeriodCells }),
];
