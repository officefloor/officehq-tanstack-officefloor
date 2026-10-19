import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, putJson } from '../../api/http';
import { useCurrentUser } from '../session/SignIn';
import { useSheetToday } from '../sheet/SheetToday';

// The sheet's rates for each grade: a percent with two decimals, set here, each in force from its
// from-date (null: from the beginning of time). A claim uses its grade's rate in force on its date;
// with none it shows n/a, and so does anything due that depends on it. Money and percents come from
// the server as strings with two decimals and are worked in whole cents and hundredths of a percent,
// never floats.
export type Rate = { grade: string; pct: string; from: string | null };

const GRADES = ['A', 'B', 'C'];
export const NA = 'n/a';

export const ratesKey = (sheet: string) => ['sheet', sheet, 'rates'];
const ratesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/rates`;

export function useRates(sheet: string): Rate[] {
  return (
    useQuery({
      queryKey: ratesKey(sheet),
      queryFn: () => getJson<Rate[]>(ratesPath(sheet)),
      enabled: !!sheet,
    }).data ?? []
  );
}

/** The grade's percent in force on the date (yyyy-MM-dd): the rate with the latest from-date on or
 *  before it. Undefined when no rate is in force then, or the date is not known yet. */
export function rateOn(rates: Rate[], grade: string, date: string | undefined): string | undefined {
  if (date === undefined) return undefined;
  let best: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (best === undefined || (best.from ?? '') < (r.from ?? '')) best = r;
  }
  return best?.pct;
}

/** A two-decimal string as a whole number of hundredths. */
export const toHundredths = (value: string): bigint => {
  const [whole, frac = ''] = value.split('.');
  return BigInt(whole) * 100n + BigInt(frac.padEnd(2, '0') || '0');
};

/** Hundredths as a two-decimal string. */
export const fromHundredths = (h: bigint): string =>
  `${h / 100n}.${String(h % 100n).padStart(2, '0')}`;

/** amount × pct / 100, rounded half up to two decimals; n/a without a rate. Amounts are never negative. */
export function due(amount: string, pct: string | undefined): string {
  if (pct === undefined) return NA;
  // cents × hundredths-of-a-percent gives cents × 10000.
  return fromHundredths((toHundredths(amount) * toHundredths(pct) + 5000n) / 10000n);
}

/** Two-decimal values added up, skipping n/a. */
export function total(values: string[]): string {
  return fromHundredths(values.filter((v) => v !== NA).reduce((sum, v) => sum + toHundredths(v), 0n));
}

export function Rates({ sheet }: { sheet: string }) {
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  return (
    <section>
      <h2>Rates</h2>
      <RateForm sheet={sheet} />
      <table>
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
              <td data-testid={`rate-current-${g}`}>{rateOn(rates, g, today) ?? NA}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>From</th>
            <th>Rate %</th>
          </tr>
        </thead>
        <tbody>
          {rates.map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td>{r.from ?? 'start'}</td>
              <td data-testid="rate-pct">{r.pct}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function RateForm({ sheet }: { sheet: string }) {
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState('A');
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const save = useMutation({
    mutationFn: (body: { grade: string; pct: string; from: string }) =>
      putJson<Rate>(`${ratesPath(sheet)}/${encodeURIComponent(body.grade)}${by}`, { pct: body.pct, from: body.from }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheet) });
      setPct('');
      setFrom('');
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save.reset();
        // A missing, out-of-range or over-precise percent, or a from-date that is not a date, is
        // refused by the server (400). A blank from-date sets the rate in force from the beginning.
        save.mutate({ grade, pct, from: from.trim() });
      }}
    >
      <label>
        Grade{' '}
        <select
          data-testid="rate-form-grade"
          value={grade}
          onChange={(e) => {
            save.reset();
            setGrade(e.target.value);
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
            save.reset();
            setPct(e.target.value);
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
            save.reset();
            setFrom(e.target.value);
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
        Set rate
      </button>
      {save.isError && (
        <span role="alert" data-testid="rate-form-error">
          {' '}
          {(save.error instanceof HttpError && save.error.status === 400 && save.error.reason)
            || 'The rate could not be saved.'}
        </span>
      )}
    </form>
  );
}
