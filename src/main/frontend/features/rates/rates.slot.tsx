import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, useClaims } from '../claims/useClaims';
import { gradeOn, useHeld, usePeople } from '../people/people.slot';
import { useSheetToday } from '../today/today.slot';
import { useUser } from '../session/useSession';
import { claimDueCents, isBound, rateOn, ratesKey, showHundredths, toHundredths, useRates, type Rate } from './useRates';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useFloorCents, useLate } from '../access/useAccess';

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

/** A person's due-total in cents: their counted claims' already-rounded figures added up (0 with none). Null when
 *  their grade has no rate on the sheet's today, unless a counted settled or stated claim keeps a figure, or a claim has
 *  one at a grade other than today's (from its own date): then the figures there are, added up. */
export function useDueTotals(sheetId: string): Map<number, bigint | null> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const today = useRateByGrade(sheetId);
  const held = useHeld(sheetId);
  const floor = useFloorCents(sheetId);
  const late = useLate(sheetId);
  const totals = new Map<number, bigint | null>();
  for (const p of people.data ?? []) {
    const counted = (claims.data ?? []).filter((c) => c.personId === p.id && counts(c, held));
    let sum = 0n;
    let kept = false;
    for (const c of counted) {
      const grade = gradeOn(p, c.date);
      const due = claimDueCents(rates.data ?? [], grade, c, floor, late);
      kept ||= c.settledDue != null || (isBound(c) && c.statedDue != null) || (due != null && grade !== p.grade);
      sum += due ?? 0n;
    }
    totals.set(p.id, today.has(p.grade) || kept ? sum : null);
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

  const submit = useSubmitOnce(save);
  return (
    <section data-testid="rates-panel">
      <h2>Rates</h2>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ grade, rate: pct, from });
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

// A person's due-total: their counted claims' due figures added up; n/a when the grade has no rate today and
// no settled or stated claim keeps a figure.
function DueTotalCell({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const total = useDueTotals(sheetId).get(personId);
  return <td data-testid="person-due-total">{total == null ? NA : showHundredths(total)}</td>;
}

// A claim's due: fixed when it was settled; the bound figure while a statement binds it; otherwise its amount times the rate in force on the claim's
// date of its person's grade on that date (at least the sheet's floor); with its late charge on top while it is overdue; n/a when no rate of the grade applies then.
function ClaimDue({ sheetId, personId, claim }: { sheetId: string; user: string; personId: number;
  claim: { amount: string; date: string; status: string; settledDue: string | null; stated: boolean; statedDue: string | null;
    dueBy: string; corrects?: string | null } }) {
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloorCents(sheetId);
  const late = useLate(sheetId);
  const grade = gradeOn(people.data?.find((p) => p.id === personId), claim.date);
  const due = claimDueCents(rates.data ?? [], grade, claim, floor, late);
  return <span data-testid="claim-due">{due == null ? NA : showHundredths(due)}</span>;
}

/** The sheet's due-total: everyone's due-total added up, skipping those that are n/a. */
export function useSheetDueTotal(sheetId: string): bigint {
  const totals = useDueTotals(sheetId);
  let sum = 0n;
  for (const t of totals.values()) if (t != null) sum += t;
  return sum;
}

// The sheet's due-total, as the rollup shows it.
function DueTotal({ sheetId }: { sheetId: string; user: string }) {
  const sum = useSheetDueTotal(sheetId);
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
