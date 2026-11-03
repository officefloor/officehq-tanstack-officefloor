import { Fragment, useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { usePeriodDueTotals, usePersonPeriodCarried, usePersonPeriodDueTotals } from './usePeriodDueTotals';
import { useSettings } from '../settings/useSettings';
import { fiscalYearOf, periodsKey, usePeriods, type Period } from './usePeriods';
import { useFigure } from '../locale/useLocale';

/** The sheet's periods: a form that adds one (a name unique on the sheet, a from- and to-date,
 *  inclusive, overlapping no other period, and an optional cap on each person's open claims in it), and each period with its due-total: the figures of the
 *  claims that count whose due-by falls in it (a settled claim's frozen due, else the figure its last
 *  statement bound it to, else its live due, with its late charge while overdue), skipping those with none — fixed once it is closed —
 *  and whether it is closed (soft for a soft-closed one), with buttons that close and soft-close it. */
function Periods({ sheetId }: { sheetId: string }) {
  const periods = usePeriods(sheetId);
  const dueTotals = usePeriodDueTotals(sheetId);
  const fiscalStartMonth = useSettings(sheetId).data?.fiscalStartMonth;
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [cap, setCap] = useState('');

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { name: string; from: string; to: string; cap: string }) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, f),
    onSuccess: async () => {
      setName('');
      setFrom('');
      setTo('');
      setCap('');
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });

  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    save.reset();
  };

  return (
    <section data-testid="periods">
      <form
        data-testid="period-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ name, from, to, cap });
          });
        }}
      >
        <label>
          Period <input data-testid="period-form-name" value={name} onChange={field(setName)} />
        </label>{' '}
        <label>
          From{' '}
          <input type="text" placeholder="yyyy-mm-dd" data-testid="period-form-from" value={from} onChange={field(setFrom)} />
        </label>{' '}
        <label>
          To <input type="text" placeholder="yyyy-mm-dd" data-testid="period-form-to" value={to} onChange={field(setTo)} />
        </label>{' '}
        <label>
          Cap <input data-testid="period-form-cap" value={cap} onChange={field(setCap)} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {save.isError && <p data-testid="period-form-error">{save.error.message}</p>}
      </form>
      <GeneratePeriods sheetId={sheetId} />
      <table data-testid="periods-list">
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due</th>
            <th>Closed</th>
            <th>Fiscal year</th>
            <th>Cap</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <PeriodRow
              key={p.id}
              sheetId={sheetId}
              period={p}
              dueTotal={dueTotals.get(p.name) ?? ''}
              fiscalYear={fiscalStartMonth === undefined ? '' : fiscalYearOf(p.from, fiscalStartMonth)}
            />
          ))}
        </tbody>
      </table>
      {periods.isError && <p data-testid="periods-error">{periods.error.message}</p>}
    </section>
  );
}

/** Generate monthly periods: one per calendar month from the from-date's month to the to-date's month
 *  (a blank from-date is the sheet's today), each named yyyy-mm, leaving out a month already named or
 *  overlapping a period. Shows how many were made (gone while it runs); a to-date before the
 *  from-date is refused with an error. A fiscal year (yyyy) and its button generate that fiscal year's twelve monthly
 *  periods the same way, from the sheet's fiscal year start month; the count and error are shared. */
function GeneratePeriods({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [fyYear, setFyYear] = useState('');
  const submitOnce = useSingleFlight();
  const generate = useMutation({
    mutationFn: (f: { from: string; to: string } | { year: string }) =>
      postJson<{ made: number }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/${'year' in f ? 'period-generate-fy' : 'period-generate'}`,
        f,
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });

  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    generate.reset();
  };

  return (
    <form
      data-testid="period-generate-form"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          generate.reset();
          return generate.mutateAsync({ from, to });
        });
      }}
    >
      <label>
        Generate from{' '}
        <input type="text" placeholder="yyyy-mm-dd" data-testid="period-generate-from" value={from} onChange={field(setFrom)} />
      </label>{' '}
      <label>
        To <input type="text" placeholder="yyyy-mm-dd" data-testid="period-generate-to" value={to} onChange={field(setTo)} />
      </label>{' '}
      <button type="submit" data-testid="period-generate" disabled={generate.isPending}>
        Generate monthly periods
      </button>{' '}
      <label>
        Fiscal year{' '}
        <input placeholder="yyyy" data-testid="period-generate-fy-year" value={fyYear} onChange={field(setFyYear)} />
      </label>{' '}
      <button
        type="button"
        data-testid="period-generate-fy"
        disabled={generate.isPending}
        onClick={() =>
          submitOnce(() => {
            generate.reset();
            return generate.mutateAsync({ year: fyYear });
          })
        }
      >
        Generate fiscal year
      </button>
      {generate.isSuccess && (
        <>
          {' '}
          Made <strong data-testid="period-generated">{generate.data.made}</strong>
        </>
      )}
      {generate.isError && <p data-testid="period-generate-error">{generate.error.message}</p>}
    </form>
  );
}

/** One period's row; closing it fixes its due-total, soft-closing it fixes its figures while its claims can still be
 *  changed, reopening it (a steward only) makes it live again.
 *  A refused close or reopen shows its error, cleared when the next action on the row is taken. */
function PeriodRow({ sheetId, period: p, dueTotal, fiscalYear }: { sheetId: string; period: Period; dueTotal: string; fiscalYear: string }) {
  const fig = useFigure();
  const queryClient = useQueryClient();
  const closeOnce = useSingleFlight();
  const close = useMutation({
    mutationFn: () =>
      postJson<Period>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(p.name)}/close`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });
  const reopen = useMutation({
    mutationFn: () =>
      postJson<Period>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(p.name)}/reopen`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });
  const softClose = useMutation({
    mutationFn: () =>
      postJson<Period>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(p.name)}/soft-close`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });
  const error = close.error ?? softClose.error ?? reopen.error;
  const act = (m: { mutateAsync: () => Promise<Period> }) =>
    closeOnce(() => {
      close.reset();
      softClose.reset();
      reopen.reset();
      return m.mutateAsync();
    });

  return (
    <tr data-testid={`period-row-${p.name}`}>
      <td data-testid="period-name">{p.name}</td>
      <td data-testid="period-from">{p.from}</td>
      <td data-testid="period-to">{p.to}</td>
      <td data-testid="period-due-total">{fig(dueTotal)}</td>
      <td data-testid="period-closed">{p.closed ? 'yes' : p.softClosed ? 'soft' : 'no'}</td>
      <td data-testid="period-fy">{fiscalYear}</td>
      <td data-testid="period-cap">{fig(p.cap ?? '')}</td>
      <td>
        <button
          type="button"
          data-testid="period-close"
          disabled={close.isPending}
          onClick={() => act(close)}
        >
          Close
        </button>{' '}
        <button
          type="button"
          data-testid="period-soft-close"
          disabled={softClose.isPending}
          onClick={() => act(softClose)}
        >
          Soft close
        </button>{' '}
        <button
          type="button"
          data-testid="period-reopen"
          disabled={reopen.isPending}
          onClick={() => act(reopen)}
        >
          Reopen
        </button>
        {error && <span data-testid="period-error">{error.message}</span>}
      </td>
    </tr>
  );
}

/** A column per period on the people list. */
function PeriodColumnHeads({ sheetId }: { sheetId: string }) {
  const periods = usePeriods(sheetId);
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <Fragment key={p.id}>
          <th>Due {p.name}</th>
          <th>Carried {p.name}</th>
        </Fragment>
      ))}
    </>
  );
}

/** A person's due-total for each period on the sheet (fixed while the period is closed), and what is carried into it. */
function PersonPeriodDueTotals({ sheetId, personId }: { sheetId: string; personId: number }) {
  const fig = useFigure();
  const periods = usePeriods(sheetId);
  const totals = usePersonPeriodDueTotals(sheetId).get(personId);
  const carried = usePersonPeriodCarried(sheetId).get(personId);
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <Fragment key={p.id}>
          <td data-testid={`person-period-${p.name}-due-total`}>{fig(totals?.get(p.name) ?? '')}</td>
          <td data-testid={`person-period-${p.name}-carried`}>{fig(carried?.get(p.name) ?? '')}</td>
        </Fragment>
      ))}
    </>
  );
}

export const contributions = [
  SheetBody.fill({ order: 6, Component: ({ sheetId }) => <Periods sheetId={sheetId} /> }),
  PersonColumnHead.fill({ order: 22, Component: PeriodColumnHeads }),
  PersonCells.fill({ order: 22, Component: PersonPeriodDueTotals }),
];
