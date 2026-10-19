import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, putJson } from '../../api/http';
import { useCurrentUser } from '../session/SignIn';

// The sheet's rate for each grade: a percent with two decimals, set here. A grade without a rate
// shows n/a, and so does anything due that depends on it. Money and percents come from the server as
// strings with two decimals and are worked in whole cents and hundredths of a percent, never floats.
export type Rate = { grade: string; pct: string };

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

/** The grade's percent, or undefined when the grade has no rate. */
export function rateOf(rates: Rate[], grade: string): string | undefined {
  return rates.find((r) => r.grade === grade)?.pct;
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
  return (
    <section>
      <h2>Rates</h2>
      <RateForm sheet={sheet} />
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate %</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const pct = rateOf(rates, g);
            return (
              <tr key={g} data-testid={pct === undefined ? undefined : `rate-row-${g}-start`}>
                <td>{g}</td>
                <td>
                  <span data-testid={`rate-current-${g}`}>
                    {pct === undefined ? NA : <span data-testid="rate-pct">{pct}</span>}
                  </span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

function RateForm({ sheet }: { sheet: string }) {
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState('A');
  const [pct, setPct] = useState('');
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const save = useMutation({
    mutationFn: (body: { grade: string; pct: string }) =>
      putJson<Rate>(`${ratesPath(sheet)}/${encodeURIComponent(body.grade)}${by}`, { pct: body.pct }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheet) });
      setPct('');
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save.reset();
        // A missing, out-of-range or over-precise percent is refused by the server (400).
        save.mutate({ grade, pct });
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
