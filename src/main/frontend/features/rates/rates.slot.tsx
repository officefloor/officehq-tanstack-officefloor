import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { useClaims } from '../claims/claims';
import { GRADES, usePeople } from '../people/people';
import type { Grade, Person } from '../people/people';
import { useUser } from '../session/session';
import { useSheetToday } from '../sheet/sheet';
import { useFloor, useLate } from '../access/access';
import { NA, PCT, personDueTotal, rateOn, ratesKey, sumDue, useRates } from './rates';
import type { Rate } from './rates';

type Fields = { grade: Grade; pct: string; from: string };
const EMPTY: Fields = { grade: 'A', pct: '', from: '' };

// The sheet's rates: a form that sets a grade's rate from a date (or from the beginning of time),
// every rate set, and each grade's rate in force on the sheet's today.
function Rates({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const save = useMutation({
    mutationFn: (body: Fields) =>
      postJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`, body, user),
    onSuccess: () => {
      setFields((f) => ({ ...f, pct: '', from: '' }));
      void queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
    onError: () => setError('That rate could not be set.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const pct = fields.pct.trim();
    if (pct === '') {
      setError('A rate is required.');
      return;
    }
    if (!PCT.test(pct)) {
      setError('A rate is a percent of 0.00 or more, with up to two decimals.');
      return;
    }
    once((done) =>
      save.mutate({ grade: fields.grade, pct, from: fields.from.trim() }, { onSettled: done }),
    );
  };

  const change = (next: Partial<Fields>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
  };

  const all = rates.data ?? [];

  return (
    <section data-testid="rates">
      <h2>Rates</h2>
      <form onSubmit={submit} data-testid="rate-form">
        <label>
          Grade{' '}
          <select
            data-testid="rate-form-grade"
            value={fields.grade}
            onChange={(e) => change({ grade: e.target.value as Grade })}
          >
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          Rate %{' '}
          <input
            data-testid="rate-form-pct"
            inputMode="decimal"
            value={fields.pct}
            onChange={(e) => change({ pct: e.target.value })}
          />
        </label>{' '}
        <label>
          From{' '}
          <input
            type="date"
            data-testid="rate-form-from"
            value={fields.from}
            onChange={(e) => change({ from: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {error && (
          <p role="alert" data-testid="rate-form-error">
            {error}
          </p>
        )}
      </form>
      {rates.isError && (
        <p role="alert" data-testid="rates-error">
          The rates could not be read.
        </p>
      )}
      <table data-testid="rate-today">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate % today</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>{rateOn(all, g, today) ?? NA}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table data-testid="rate-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>From</th>
            <th>Rate %</th>
          </tr>
        </thead>
        <tbody>
          {all.map((r) => (
            <tr key={`${r.grade}-${r.from}`} data-testid={`rate-row-${r.grade}-${r.from || 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from || 'start'}</td>
              <td data-testid="rate-pct">{r.pct}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function DueHead() {
  return <th>Due</th>;
}

// A person's due-total: the due figures of their claims that count, added up.
function PersonDueTotal({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  return <td data-testid="person-due-total">{personDueTotal(rates, person.grade, today, claims, person.held, floor, late)}</td>;
}

// The sheet's due-total: everyone's due-total added up, skipping those with no rate.
function DueTotal({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const totals = people.map((p) =>
    personDueTotal(rates, p.grade, today, claims.filter((c) => c.personId === p.id), p.held, floor, late),
  );
  return (
    <p>
      Due in all: <strong data-testid="rollup-due-total">{sumDue(totals)}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: -10, Component: Rates }),
  SheetBody.fill({ order: 20, Component: DueTotal }),
  PersonHead.fill({ order: 20, Component: DueHead }),
  PersonRow.fill({ order: 20, Component: PersonDueTotal }),
];
