import { Fragment, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheet';
import { useClaims } from '../claims/claims';
import { GRADES, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import { useFloor, useLate } from '../settings/settings';
import { personDueTotal, rateOn, ratesKey, sheetDueTotal, useRates } from './rates';
import type { Rate } from './rates';

const PCT = /^\d{1,3}(\.\d{1,2})?$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** The sheet's rates — per grade the one in force on the sheet's today, then each rate with its
 *  from-date and to-date — and the form that sets one (for a grade and from-date, replacing any it had). */
function Rates({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (body: { grade: string; rate: string; from: string; to: string }) =>
      putJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates/${body.grade}`, {
        user,
        rate: body.rate,
        from: body.from || null,
        to: body.to || null,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
      setPct('');
      setFrom('');
      setTo('');
    },
    onError: () =>
      setError('Could not set this rate: it must be a percent, 0.00 to 999.99, with up to two decimals, from a date yyyy-mm-dd or none, to a date not before it or none.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!PCT.test(pct.trim())) {
      setError('The rate must be a percent, 0.00 to 999.99, with up to two decimals.');
      return;
    }
    if (from.trim() !== '' && !DATE.test(from.trim())) {
      setError('The from-date must be a date, yyyy-mm-dd, or left empty.');
      return;
    }
    if (to.trim() !== '' && !DATE.test(to.trim())) {
      setError('The to-date must be a date, yyyy-mm-dd, or left empty.');
      return;
    }
    if (to.trim() !== '' && from.trim() !== '' && to.trim() < from.trim()) {
      setError('The to-date must not be before the from-date.');
      return;
    }
    if (!once.start()) return;
    save.mutate({ grade, rate: pct.trim(), from: from.trim(), to: to.trim() });
  };

  const change = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    set(e.target.value);
    setError('');
  };

  return (
    <section data-testid="rates">
      <h2>Rates</h2>
      <form data-testid="rate-form" onSubmit={submit}>
        <label>
          Grade{' '}
          <select data-testid="rate-form-grade" value={grade} onChange={change(setGrade)}>
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          Rate %{' '}
          <input data-testid="rate-form-pct" placeholder="0.00" value={pct} onChange={change(setPct)} />
        </label>{' '}
        <label>
          From{' '}
          <input data-testid="rate-form-from" placeholder="yyyy-mm-dd (optional)" value={from} onChange={change(setFrom)} />
        </label>{' '}
        <label>
          To{' '}
          <input data-testid="rate-form-to" placeholder="yyyy-mm-dd (optional)" value={to} onChange={change(setTo)} />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {error && (
          <p data-testid="rate-form-error" role="alert">
            {error}
          </p>
        )}
      </form>
      <table>
        <tbody>
          {GRADES.map((g) => {
            const undated = rates.find((r) => r.grade === g && r.from === null);
            const dated = rates.filter((r) => r.grade === g && r.from !== null);
            return (
              <Fragment key={g}>
                <tr>
                  <th>{g}</th>
                  <td>
                    In force today <span data-testid={`rate-current-${g}`}>{rateOn(rates, g, today) ?? 'n/a'}</span>
                  </td>
                </tr>
                <tr data-testid={`rate-row-${g}-start`}>
                  <td data-testid="rate-from">from the start</td>
                  <td data-testid="rate-pct">{undated?.rate ?? 'n/a'}</td>
                  <td data-testid="rate-to">{undated?.to ?? ''}</td>
                </tr>
                {dated.map((r) => (
                  <tr key={r.from} data-testid={`rate-row-${g}-${r.from}`}>
                    <td data-testid="rate-from">{r.from}</td>
                    <td data-testid="rate-pct">{r.rate}</td>
                    <td data-testid="rate-to">{r.to ?? ''}</td>
                  </tr>
                ))}
              </Fragment>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

/** A person's due-total: the figures of their claims that count added up (see personDueTotal). */
function PersonDueTotal({ sheetId, person }: { sheetId: string; user: string; person: Person }) {
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  return <td data-testid="person-due-total">{personDueTotal(person, claims, rates, today, floor, late)}</td>;
}

/** The sheet's due-total: every person's due-total added up, skipping n/a. */
function DueTotal({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  return (
    <p>
      Due total <strong data-testid="rollup-due-total">{sheetDueTotal(people, claims, rates, today, floor, late)}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 5, Component: Rates }),
  SheetBody.fill({ order: 21, Component: DueTotal }),
  PersonRow.fill({ order: 20, Component: PersonDueTotal }),
];
