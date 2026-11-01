import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { GRADES } from '../people/people';
import type { Grade } from '../people/people';
import { rateOf, ratesKey, ratesPath, useRates } from './rates';
import type { Rate } from './rates';

type Draft = { grade: Grade; pct: string };
const EMPTY: Draft = { grade: 'A', pct: '' };

// The sheet's rate for each grade: shown per grade (n/a when unset), with a form that sets one.
function Rates({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: rates = [] } = useRates(sheet);
  const [draft, setDraft] = useState<Draft>(EMPTY);

  const save = useMutation({
    mutationFn: (body: Draft) => postJson<Rate[]>(byUser(ratesPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(ratesKey(sheet), updated);
      setDraft((d) => ({ ...d, pct: '' }));
    },
  });

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.reset();
    // The server refuses a percent that is not 0.00 or more with up to two decimals.
    save.mutate(draft);
  };

  return (
    <section>
      <h2>Rates</h2>
      <form onSubmit={submit}>
        <label>
          Grade{' '}
          <select
            data-testid="rate-form-grade"
            value={draft.grade}
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
          <input data-testid="rate-form-pct" value={draft.pct} onChange={(e) => change({ pct: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && (
          <span data-testid="rate-form-error" role="alert">
            {' '}A rate needs a grade of A, B or C and a percent of 0.00 or more with up to two decimals.
          </span>
        )}
      </form>
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
                <td data-testid={`rate-current-${g}`}>
                  {pct === undefined ? 'n/a' : <span data-testid="rate-pct">{pct}</span>}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 5, Component: Rates });
