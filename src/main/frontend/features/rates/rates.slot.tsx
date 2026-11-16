import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { live, useClaims } from '../claims/claims';
import { useSheetDueTotal, useSheetSplit } from '../header/totals';
import { GRADES, usePeople, type Person } from '../people/people';
import { useSheetToday } from '../session/session';
import { useCap, useFloor, useLate } from '../settings/settings';
import { personDue, personExcess, personSplit, RATE_PATTERN, ratePreview, rateFor, ratesKey, useRates, type Rate } from './rates';

/** The sheet's rates: the form that sets one (from an optional from-date, through an optional to-date), each grade's rate in force on the
 *  sheet's today, and every rate set. */
function Rates({ sheetId, user }: { sheetId: string; user: string }) {
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId);
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: (v: { grade: string; rate: string; from: string; to: string }) =>
      putJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates/${encodeURIComponent(v.grade)}`, {
        rate: v.rate,
        from: v.from,
        to: v.to,
        by: user,
      }),
    onSuccess: () => {
      setPct('');
      setFrom('');
      setTo('');
      void queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <section data-testid="rates">
      <h3>Rates</h3>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          save.reset();
          save.mutate({ grade, rate: pct, from, to });
        }}
      >
        <label>
          Grade{' '}
          <select
            data-testid="rate-form-grade"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              save.reset();
            }}
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
            value={pct}
            onChange={(e) => {
              setPct(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          From{' '}
          <input
            type="date"
            data-testid="rate-form-from"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            type="date"
            data-testid="rate-form-to"
            value={to}
            onChange={(e) => {
              setTo(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && (
          <span data-testid="rate-form-error">The rate must be a percent 0.00 or more, with up to two decimals; the from- and to-dates must be dates, the to-date not before the from-date.</span>
        )}
      </form>
      {rates.isError && <p data-testid="rates-load-error">Could not load rates.</p>}
      <RatePreview sheetId={sheetId} rates={rates.data} />
      <table data-testid="rates-current">
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>{rateFor(rates.data, g, today) ?? 'n/a'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table data-testid="rates-table">
        <tbody>
          {(rates.data ?? []).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-pct">{r.rate}</td>
              <td data-testid="rate-from">{r.from ?? ''}</td>
              <td data-testid="rate-to">{r.to ?? ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

/** A preview of a candidate rate for a grade: how many OPEN and PENDING claims of people on the grade (on the
 *  claim's date) would show a different due, and the sum of the differences (see ratePreview). Nothing is saved. */
function RatePreview({ sheetId, rates }: { sheetId: string; rates: Rate[] | undefined }) {
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [pct, setPct] = useState('');
  const [result, setResult] = useState<{ count: number; diff: string } | null>(null);
  const [error, setError] = useState(false);
  const clear = () => {
    setResult(null);
    setError(false);
  };

  return (
    <form
      data-testid="rate-preview-form"
      onSubmit={(e) => {
        e.preventDefault();
        clear();
        const rate = pct.trim();
        if (!RATE_PATTERN.test(rate)) {
          setError(true);
          return;
        }
        const next = ratePreview(claims.data ?? [], people.data ?? [], rates, grade, rate, late, floor);
        setTimeout(() => setResult(next), 0);
      }}
    >
      <label>
        Preview grade{' '}
        <select
          data-testid="rate-preview-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            clear();
          }}
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
          data-testid="rate-preview-pct"
          inputMode="decimal"
          value={pct}
          onChange={(e) => {
            setPct(e.target.value);
            clear();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="rate-preview">
        Preview
      </button>
      {result !== null && (
        <span>
          {' '}Claims changed <span data-testid="rate-preview-count">{result.count}</span>
          {' '}Difference <span data-testid="rate-preview-diff">{result.diff}</span>
        </span>
      )}
      {error && <span data-testid="rate-preview-error">The rate must be a percent 0.00 or more, with up to two decimals.</span>}
    </form>
  );
}

function DueHead() {
  return (
    <>
      <th>Due total</th>
      <th>Excess</th>
      <th>Settled total</th>
      <th>Open total</th>
    </>
  );
}

/** A person's due-total: the sum of their claims' due figures, at most the sheet's cap (see personDue), and their
 *  excess over the cap (see personExcess). */
function PersonDueTotal({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const cap = useCap(sheetId);
  const mine = live(claims.data?.filter((c) => c.personId === person.id) ?? [], [person], late.today);
  const split = personSplit(mine, rates.data, person, late, floor);
  return (
    <>
      <td data-testid="person-due-total">{personDue(mine, rates.data, person, late, floor, cap)}</td>
      <td data-testid="person-excess">{personExcess(mine, rates.data, person, late, floor, cap)}</td>
      <td data-testid="person-settled-total">{split.settled}</td>
      <td data-testid="person-open-total">{split.open}</td>
    </>
  );
}

/** The sheet's due-total: everyone's due-total added up, skipping n/a; and its settled-total and open-total. */
function DueTotal({ sheetId }: { sheetId: string }) {
  const split = useSheetSplit(sheetId);
  return (
    <p>
      Due total <span data-testid="rollup-due-total">{useSheetDueTotal(sheetId)}</span>
      {' '}Settled total <span data-testid="rollup-settled-total">{split.settled}</span>
      {' '}Open total <span data-testid="rollup-open-total">{split.open}</span>
    </p>
  );
}

export const contributions = [
  SheetPage.fill({ order: 5, Component: Rates }),
  PeopleHead.fill({ order: 20, Component: DueHead }),
  PersonRow.fill({ order: 20, Component: PersonDueTotal }),
  SheetPage.fill({ order: 30, Component: DueTotal }),
];
