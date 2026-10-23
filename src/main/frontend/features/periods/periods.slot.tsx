import { useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheet';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { fiscalYearOf, useFiscalStartMonth, useFloor, useLate } from '../settings/settings';
import { useRates } from '../rates/rates';
import { periodDueTotal, periodsKey, personPeriodCarried, personPeriodDueTotal, usePeriods } from './periods';
import type { Period } from './periods';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** One period with its due-total (see periodDueTotal), whether it is closed (yes, soft or no), the fiscal year its
 *  from-date falls in (see fiscalYearOf), its cap on each person's open claims (blank: none), and the buttons that close it, soft-close it and (a steward only) reopen it. */
function PeriodRow({ sheetId, user, period, dueTotal, fiscalYear }: { sheetId: string; user: string; period: Period;
    dueTotal: string; fiscalYear: string }) {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');

  const close = useMutation({
    mutationFn: () =>
      putJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(period.name)}/close`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not close this period.'),
  });

  const softClose = useMutation({
    mutationFn: () =>
      putJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(period.name)}/soft-close`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not soft-close this period.'),
  });

  const reopen = useMutation({
    mutationFn: () =>
      putJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(period.name)}/reopen`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not reopen this period.'),
  });

  return (
    <tr data-testid={`period-row-${period.name}`}>
      <td data-testid="period-name">{period.name}</td>
      <td data-testid="period-from">{period.from}</td>
      <td data-testid="period-to">{period.to}</td>
      <td>
        Due total <span data-testid="period-due-total">{dueTotal}</span>
      </td>
      <td>
        Closed <span data-testid="period-closed">{period.closed ? 'yes' : period.soft ? 'soft' : 'no'}</span>
      </td>
      <td>
        Fiscal year <span data-testid="period-fy">{fiscalYear}</span>
      </td>
      <td>
        Cap <span data-testid="period-cap">{period.cap ?? ''}</span>
      </td>
      <td>
        <button
          type="button"
          data-testid="period-close"
          disabled={close.isPending}
          onClick={() => {
            setError('');
            close.mutate();
          }}
        >
          Close
        </button>{' '}
        <button
          type="button"
          data-testid="period-soft-close"
          disabled={softClose.isPending}
          onClick={() => {
            setError('');
            softClose.mutate();
          }}
        >
          Soft close
        </button>{' '}
        <button
          type="button"
          data-testid="period-reopen"
          disabled={reopen.isPending}
          onClick={() => {
            setError('');
            reopen.mutate();
          }}
        >
          Reopen
        </button>
        {error && (
          <p data-testid="period-error" role="alert">
            {error}
          </p>
        )}
      </td>
    </tr>
  );
}

/** The form that generates one yyyy-mm period per calendar month from the from-date's month to the to-date's month
 *  (months already taken are left out), or the twelve months of a fiscal year (FY<yyyy>, from the sheet's fiscal year
 *  start month in yyyy) the same way, and how many it made. */
function GeneratePeriods({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [fyYear, setFyYear] = useState('');
  const [generated, setGenerated] = useState<number | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const generate = useMutation({
    mutationFn: (body: { from: string; to: string } | { year: string }) =>
      postJson<{ generated: number }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/${'year' in body ? 'generate-fy' : 'generate'}`,
        { user, ...body },
      ),
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
      setGenerated(result.generated);
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : 'Could not generate periods.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setGenerated(null);
    if (!DATE.test(from.trim()) || !DATE.test(to.trim())) {
      setError('The from and to dates must be dates, yyyy-mm-dd.');
      return;
    }
    if (to.trim() < from.trim()) {
      setError('The to-date must not be before the from-date.');
      return;
    }
    if (!once.start()) return;
    generate.mutate({ from: from.trim(), to: to.trim() });
  };

  const generateFy = () => {
    setError('');
    setGenerated(null);
    if (!/^\d{4}$/.test(fyYear.trim())) {
      setError('The fiscal year must be a year, yyyy.');
      return;
    }
    if (!once.start()) return;
    generate.mutate({ year: fyYear.trim() });
  };

  const change = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    setError('');
    setGenerated(null);
  };

  return (
    <form data-testid="period-generate-form" onSubmit={submit}>
      <label>
        From <input data-testid="period-generate-from" placeholder="yyyy-mm-dd" value={from} onChange={change(setFrom)} />
      </label>{' '}
      <label>
        To <input data-testid="period-generate-to" placeholder="yyyy-mm-dd" value={to} onChange={change(setTo)} />
      </label>{' '}
      <button type="submit" data-testid="period-generate" disabled={generate.isPending}>
        Generate monthly periods
      </button>{' '}
      <label>
        Fiscal year FY <input data-testid="period-generate-fy-year" placeholder="yyyy" value={fyYear} onChange={change(setFyYear)} />
      </label>{' '}
      <button type="button" data-testid="period-generate-fy" disabled={generate.isPending} onClick={generateFy}>
        Generate fiscal year
      </button>
      {generated !== null && (
        <span>
          {' '}Generated <span data-testid="period-generated">{generated}</span>
        </span>
      )}
      {error && (
        <p data-testid="period-generate-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

/** The sheet's periods, each with its due-total, the form that adds one and the form that generates monthly ones. */
function Periods({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const periods = usePeriods(sheetId).data ?? [];
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const fiscalStartMonth = useFiscalStartMonth(sheetId);
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [cap, setCap] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (body: { name: string; from: string; to: string; cap: string }) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, { user, ...body }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
      setName('');
      setFrom('');
      setTo('');
      setCap('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : 'Could not add this period.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('The period needs a name.');
      return;
    }
    if (!DATE.test(from.trim()) || !DATE.test(to.trim())) {
      setError('The from and to dates must be dates, yyyy-mm-dd.');
      return;
    }
    if (from.trim() > to.trim()) {
      setError('The from-date must be on or before the to-date.');
      return;
    }
    if (cap.trim() !== '' && (!/^\d+$/.test(cap.trim()) || Number(cap.trim()) < 1)) {
      setError('The cap must be a whole number of at least 1, or blank for none.');
      return;
    }
    if (!once.start()) return;
    add.mutate({ name: name.trim(), from: from.trim(), to: to.trim(), cap: cap.trim() });
  };

  const change = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    setError('');
  };

  return (
    <section data-testid="periods">
      <h2>Periods</h2>
      <form data-testid="period-form" onSubmit={submit}>
        <label>
          Name <input data-testid="period-form-name" value={name} onChange={change(setName)} />
        </label>{' '}
        <label>
          From <input data-testid="period-form-from" placeholder="yyyy-mm-dd" value={from} onChange={change(setFrom)} />
        </label>{' '}
        <label>
          To <input data-testid="period-form-to" placeholder="yyyy-mm-dd" value={to} onChange={change(setTo)} />
        </label>{' '}
        <label>
          Cap <input data-testid="period-form-cap" placeholder="open claims per person" value={cap} onChange={change(setCap)} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={add.isPending}>
          Add period
        </button>
        {error && (
          <p data-testid="period-form-error" role="alert">
            {error}
          </p>
        )}
      </form>
      <GeneratePeriods sheetId={sheetId} user={user} />
      <table>
        <tbody>
          {periods.map((p) => (
            <PeriodRow key={p.name} sheetId={sheetId} user={user} period={p} dueTotal={periodDueTotal(p, claims, people, rates, floor, late)}
              fiscalYear={fiscalYearOf(p.from, fiscalStartMonth)} />
          ))}
        </tbody>
      </table>
    </section>
  );
}

/** A person's due-total for each period on the sheet (see personPeriodDueTotal). */
function PersonPeriodDueTotals({ sheetId, person }: { sheetId: string; user: string; person: Person }) {
  const periods = usePeriods(sheetId).data ?? [];
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  return (
    <>
      {periods.map((p) => (
        <td key={p.name}>
          {p.name} <span data-testid={`person-period-${p.name}-due-total`}>
            {personPeriodDueTotal(p, person, claims, people, rates, floor, late)}
          </span>{' '}
          carried <span data-testid={`person-period-${p.name}-carried`}>
            {personPeriodCarried(p, periods, person, claims, people, rates, floor, late)}
          </span>
        </td>
      ))}
    </>
  );
}

export const contributions = [
  SheetBody.fill({ order: 6, Component: Periods }),
  PersonRow.fill({ order: 22, Component: PersonPeriodDueTotals }),
];
