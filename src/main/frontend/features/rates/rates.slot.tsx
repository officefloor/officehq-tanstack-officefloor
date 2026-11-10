import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { useClaims } from '../claims/claims';
import { GRADES, liveHolds, usePeople } from '../people/people';
import type { Grade, Person } from '../people/people';
import { useUser } from '../session/session';
import { useSheetToday } from '../sheet/sheet';
import { useCap, useFloor, useLate } from '../access/access';
import { NA, PCT, personDueTotal, personExcess, rateOn, ratesKey, sumDue, trimmedSumDue, useRates } from './rates';
import type { Rate } from './rates';

type Fields = { grade: Grade; pct: string; from: string; to: string };
const EMPTY: Fields = { grade: 'A', pct: '', from: '', to: '' };

// The sheet's rates: a form that sets a grade's rate from a date (or from the beginning of time)
// through a date (or with no end),
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
      setFields((f) => ({ ...f, pct: '', from: '', to: '' }));
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
    const from = fields.from.trim();
    const to = fields.to.trim();
    if (to !== '' && from !== '' && to < from) {
      setError('A to-date is on or after the from-date.');
      return;
    }
    once((done) =>
      save.mutate({ grade: fields.grade, pct, from, to }, { onSettled: done }),
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
        <label>
          To{' '}
          <input
            type="date"
            data-testid="rate-form-to"
            value={fields.to}
            onChange={(e) => change({ to: e.target.value })}
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
            <th>To</th>
            <th>Rate %</th>
          </tr>
        </thead>
        <tbody>
          {all.map((r) => (
            <tr key={`${r.grade}-${r.from}`} data-testid={`rate-row-${r.grade}-${r.from || 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from || 'start'}</td>
              <td data-testid="rate-to">{r.to}</td>
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

// A person's due-total: the due figures of their claims that count, added up, held to the sheet's cap.
function PersonDueTotal({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  return <td data-testid="person-due-total">{personDueTotal(rates, person.grades, today, claims, liveHolds(person, today), floor, late, cap)}</td>;
}

function ExcessHead() {
  return <th>Excess</th>;
}

// A person's excess: how far the due figures of their claims that count go above the sheet's cap.
function PersonExcess({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  return <td data-testid="person-excess">{personExcess(rates, person.grades, today, claims, liveHolds(person, today), floor, late, cap)}</td>;
}

// The sheet's due-total: everyone's due-total added up, skipping those with no rate, trimmed of the highest and lowest.
function DueTotal({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const totals = people.map((p) =>
    personDueTotal(rates, p.grades, today, claims.filter((c) => c.personId === p.id), liveHolds(p, today), floor, late, cap),
  );
  return (
    <p>
      Due in all: <strong data-testid="rollup-due-total">{trimmedSumDue(totals)}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: -10, Component: Rates }),
  SheetBody.fill({ order: 20, Component: DueTotal }),
  PersonHead.fill({ order: 20, Component: DueHead }),
  PersonRow.fill({ order: 20, Component: PersonDueTotal }),
  PersonHead.fill({ order: 21, Component: ExcessHead }),
  PersonRow.fill({ order: 21, Component: PersonExcess }),
];
