import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, useClaims } from '../claims/useClaims';
import { usePeople } from '../people/people.slot';
import { useSheetToday } from '../today/today.slot';
import { useUser } from '../session/useSession';
import { dueCents, rateOn, ratesKey, showHundredths, toHundredths, useRates, type Rate } from './useRates';

const GRADES = ['A', 'B', 'C'] as const;
const RATE = /^\d{1,3}(\.\d{1,2})?$/;
const NA = 'n/a';

/** The rate (in hundredths of a percent) in force on the sheet's today for each grade that has one. */
function useRateByGrade(sheetId: string): Map<string, bigint> {
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId).data?.today;
  const byGrade = new Map<string, bigint>();
  if (today === undefined) return byGrade;
  for (const g of GRADES) {
    const r = rateOn(rates.data ?? [], g, today);
    if (r) byGrade.set(g, toHundredths(r.rate));
  }
  return byGrade;
}

/** A person's due-total in cents (their counted claims times their grade's rate in force on the sheet's today), or
 *  null when the grade has no rate then. */
function useDueTotals(sheetId: string): Map<number, bigint | null> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRateByGrade(sheetId);
  const totals = new Map<number, bigint | null>();
  for (const p of people.data ?? []) {
    const rate = rates.get(p.grade);
    if (rate === undefined) {
      totals.set(p.id, null);
      continue;
    }
    const claimed = (claims.data ?? [])
      .filter((c) => c.personId === p.id && counts(c))
      .reduce((n, c) => n + toHundredths(c.amount), 0n);
    totals.set(p.id, dueCents(claimed, rate));
  }
  return totals;
}

// The rate form: sets a grade's rate from a date (blank: from the beginning of time), replacing one
// already set for that grade and date. Every rate is listed under it, with each grade's rate in
// force on the sheet's today.
function RatesPanel({ sheetId }: { sheetId: string; user: string }) {
  const rates = useRates(sheetId);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const today = useSheetToday(sheetId).data?.today;
  const [user] = useUser();
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: async (body: { grade: string; rate: string; from: string }) => {
      if (body.rate.trim() === '') throw new Error('Enter a rate');
      if (!RATE.test(body.rate.trim()) || Number(body.rate) > 100) {
        throw new Error('A rate is a percent, 0.00 to 100.00, with up to two decimals');
      }
      // `by` is the signed-in user the change is recorded against.
      return putJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`, { ...body, from: body.from.trim(), by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
      setPct('');
      setFrom('');
    },
  });

  return (
    <section data-testid="rates-panel">
      <h2>Rates</h2>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          save.mutate({ grade, rate: pct, from });
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
            placeholder="0.00"
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
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && (
          <p data-testid="rate-form-error" role="alert">
            {save.error.message}
          </p>
        )}
      </form>
      {rates.isError && (
        <p data-testid="rates-error" role="alert">
          {rates.error.message}
        </p>
      )}
      <table data-testid="rate-current-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate % today</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const r = today === undefined ? undefined : rateOn(rates.data ?? [], g, today);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`rate-current-${g}`}>{r ? showHundredths(toHundredths(r.rate)) : NA}</td>
              </tr>
            );
          })}
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
          {(rates.data ?? []).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from ?? ''}</td>
              <td data-testid="rate-pct">{showHundredths(toHundredths(r.rate))}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function DueHead() {
  return <th>Due total</th>;
}

// A person's due-total: their claimed times their grade's rate; n/a when the grade has no rate.
function DueTotalCell({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const total = useDueTotals(sheetId).get(personId);
  return <td data-testid="person-due-total">{total == null ? NA : showHundredths(total)}</td>;
}

// A claim's due: fixed when it was settled; otherwise its amount times its person's grade's rate in
// force on the claim's date, n/a when no rate of the grade applies then.
function ClaimDue({ sheetId, personId, claim }: { sheetId: string; user: string; personId: number;
  claim: { amount: string; date: string; settledDue: string | null } }) {
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const grade = people.data?.find((p) => p.id === personId)?.grade;
  const found = grade === undefined ? undefined : rateOn(rates.data ?? [], grade, claim.date);
  const rate = found === undefined ? undefined : toHundredths(found.rate);
  if (claim.settledDue != null) {
    return <span data-testid="claim-due">{showHundredths(toHundredths(claim.settledDue))}</span>;
  }
  return (
    <span data-testid="claim-due">{rate === undefined ? NA : showHundredths(dueCents(toHundredths(claim.amount), rate))}</span>
  );
}

// The sheet's due-total: everyone's due-total added up, skipping those that are n/a.
function DueTotal({ sheetId }: { sheetId: string; user: string }) {
  const totals = useDueTotals(sheetId);
  let sum = 0n;
  for (const t of totals.values()) if (t != null) sum += t;
  return (
    <section data-testid="due-rollup">
      <p>
        Due total: <strong data-testid="rollup-due-total">{showHundredths(sum)}</strong>
      </p>
    </section>
  );
}

export const contributions = [
  SheetBody.fill({ order: 5, Component: RatesPanel }),
  PersonHeads.fill({ order: 20, Component: DueHead }),
  PersonCells.fill({ order: 20, Component: DueTotalCell }),
  ClaimCells.fill({ order: 10, Component: ClaimDue }),
  SheetBody.fill({ order: 25, Component: DueTotal }),
];
