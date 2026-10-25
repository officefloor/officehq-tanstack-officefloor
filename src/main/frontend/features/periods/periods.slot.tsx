import { useState, type ChangeEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, useClaims, type ClaimStatus } from '../claims/useClaims';
import { gradeOn, useHeld, usePeople } from '../people/people.slot';
import { useUser } from '../session/useSession';
import { claimDueCents, showHundredths, useRates } from '../rates/useRates';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useFloorCents, useLate } from '../access/useAccess';

const DATE = /^\d{4}-\d{2}-\d{2}$/;
const NO_HOLDS = new Map();

/** A period of the sheet: its name and its from and to dates (yyyy-mm-dd), both inclusive; whether it is
 *  closed, the due-total fixed when it was closed (null while open), and each person's due-total for it fixed
 *  then, by person id (empty while open; a person added later has none); and whether it is soft-closed: its
 *  figures fixed as a close fixes them, but claims dated inside it can still change. */
export type Period = {
  id: number; name: string; from: string; to: string; closed: boolean; closedTotal: string | null;
  personTotals: Record<string, string>; soft: boolean;
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

/** A claim's status at the end of a day (yyyy-mm-dd) as its log records it: its last line logged on or before the
 *  day (the log is oldest first). None when it has no such line: it was added after the day. */
export function statusAsOf(log: readonly string[] | undefined, day: string): ClaimStatus | undefined {
  let status: ClaimStatus | undefined;
  for (const line of log ?? []) {
    const m = /^(\w+) by .* on (\d{4}-\d{2}-\d{2}) due=/.exec(line);
    if (m && m[2] <= day) status = m[1] as ClaimStatus;
  }
  return status;
}

/** A person's live due-total for a period in cents: the due figures (as shown now) of their claims whose due-by falls
 *  in it and that count by the status they had at the end of its last day, added up — a claim added after then does
 *  not count; a deferred or held one is left out as in the live totals. With no person given, every person's
 *  together: the period's own due-total. */
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
      if (c.dueBy < p.from || c.dueBy > p.to) continue;
      const then = statusAsOf(c.log, p.to);
      // Counted by its status then, but held (or deferred) as the live totals leave it out: by its status now.
      const live = c.status === 'OPEN' || c.status === 'PENDING';
      if (then === undefined || !counts({ ...c, status: then }, NO_HOLDS) || (live && !counts(c, held))) continue;
      sum += claimDueCents(rates.data ?? [], gradeOn(personOf.get(c.personId), c.date), c, floor, late) ?? 0n;
    }
    return sum;
  };
}

/** A period's due-total as shown: the one fixed when it was closed or soft-closed; while open, the live figure. */
export function showPeriodDueTotal(p: Period, live: bigint): string {
  return (p.closed || p.soft) && p.closedTotal != null ? p.closedTotal : showHundredths(live);
}

/** A person's due-total for a period as shown: the one fixed when it was closed or soft-closed (0.00 for a person
 *  added since); while open, the live figure. */
export function showPersonPeriodDueTotal(p: Period, personId: number, live: bigint): string {
  return p.closed || p.soft ? (p.personTotals?.[String(personId)] ?? '0.00') : showHundredths(live);
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
      <GeneratePeriods sheetId={sheetId} />
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

// The generate form: a period per calendar month from the from-date's month (blank: the sheet's today) to the
// to-date's, first to last day, named yyyy-mm. A month whose name is in use or whose days overlap a period is left
// out. Shows how many were made; the count is gone while it runs. A to-date before the from-date is refused.
function GeneratePeriods({ sheetId }: { sheetId: string }) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [made, setMade] = useState<number | null>(null);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const generate = useMutation({
    mutationFn: async (body: { from: string; to: string }) => {
      setMade(null);
      if (body.to.trim() === '') throw new Error('Enter a to-date');
      if ((body.from.trim() !== '' && !DATE.test(body.from.trim())) || !DATE.test(body.to.trim())) {
        throw new Error('Enter a date as YYYY-MM-DD');
      }
      // `by` is the signed-in user the change is recorded against.
      const result = await postJson<{ made: number }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/generate`,
        { from: body.from.trim(), to: body.to.trim(), by: user ?? '' });
      setMade(result.made);
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
      return result;
    },
  });
  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    generate.reset();
  };
  const submit = useSubmitOnce(generate);
  return (
    <form
      data-testid="period-generate-form"
      onSubmit={(e) => {
        e.preventDefault();
        submit({ from, to });
      }}
    >
      <label>
        Generate monthly from <input type="date" data-testid="period-generate-from" value={from} onChange={field(setFrom)} />
      </label>{' '}
      <label>
        To <input type="date" data-testid="period-generate-to" value={to} onChange={field(setTo)} />
      </label>{' '}
      <button type="submit" data-testid="period-generate" disabled={generate.isPending}>
        Generate periods
      </button>{' '}
      {made !== null && !generate.isError && (
        <span>
          Made <strong data-testid="period-generated">{made}</strong>
        </span>
      )}
      {generate.isError && (
        <p data-testid="period-generate-error" role="alert">
          {generate.error.message}
        </p>
      )}
    </form>
  );
}

// A period's row. A closed period shows the due-total fixed when it was closed; an open one, the live
// figure. Closing is refused with the row's error when someone else closed it first. Reopening (a
// steward only) makes the due-total live again; anyone else, or a period not closed, is refused with
// the row's error. Soft-closing (as closing, by the close permission) fixes the figures but leaves claims inside it
// open to change; a soft-closed period can be closed (fixing them afresh) or reopened. Each action clears the
// error the others left.
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
  const softClose = useMutation({
    mutationFn: () => postJson<Period>(
      `/api/sheets/${encodeURIComponent(sheetId)}/periods/${p.id}/soft-close`, { by: user ?? '' }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
  });
  const submitClose = useSubmitOnce(close);
  const submitSoftClose = useSubmitOnce(softClose);
  const submitReopen = useSubmitOnce(reopen);
  const error = close.error ?? softClose.error ?? reopen.error;
  return (
    <tr data-testid={`period-row-${p.name}`}>
      <td data-testid="period-name">{p.name}</td>
      <td data-testid="period-from">{p.from}</td>
      <td data-testid="period-to">{p.to}</td>
      <td data-testid="period-due-total">
        {showPeriodDueTotal(p, dueTotal)}
      </td>
      <td data-testid="period-closed">{p.closed ? 'yes' : p.soft ? 'soft' : 'no'}</td>
      <td>
        {!p.closed && (
          <button
            type="button"
            data-testid="period-close"
            disabled={close.isPending}
            onClick={() => {
              softClose.reset();
              reopen.reset();
              submitClose(undefined);
            }}
          >
            Close
          </button>
        )}{' '}
        {!p.closed && (
          <button
            type="button"
            data-testid="period-soft-close"
            disabled={softClose.isPending}
            onClick={() => {
              close.reset();
              reopen.reset();
              submitSoftClose(undefined);
            }}
          >
            Soft close
          </button>
        )}{' '}
        <button
          type="button"
          data-testid="period-reopen"
          disabled={reopen.isPending}
          onClick={() => {
            close.reset();
            softClose.reset();
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

// The period a claim belongs to: the one whose dates contain its due-by (as fixed when it was added); blank when none does.
function ClaimPeriod({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: { dueBy: string } }) {
  const periods = usePeriods(sheetId);
  return <span data-testid="claim-period">{periodOn(periods.data ?? [], claim.dueBy)?.name ?? ''}</span>;
}

// Whether the claim was added while its period (by its due-by) was soft-closed: a late entry, for good.
function ClaimLateEntry({ claim }: { sheetId: string; user: string; personId: number; claim: { lateEntry?: boolean } }) {
  return <span data-testid="claim-late-entry">{claim.lateEntry ? 'yes' : 'no'}</span>;
}

export const contributions = [
  SheetBody.fill({ order: 7, Component: PeriodsPanel }),
  ClaimCells.fill({ order: 30, Component: ClaimPeriod }),
  ClaimCells.fill({ order: 31, Component: ClaimLateEntry }),
  PersonHeads.fill({ order: 26, Component: PersonPeriodHeads }),
  PersonCells.fill({ order: 26, Component: PersonPeriodCells }),
];
