import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { periodsKey } from './periods';

type RolloverResult = { made: number };

/** The year-end rollover (a steward only): the old fiscal year (yyyy, FY<yyyy>) and the button that rolls it over.
 *  The server generates the next fiscal year's twelve periods as the fiscal year generator does and carries each
 *  person's open figure at the old year's end into the new year's first period; the form shows how many periods it
 *  made. A refusal (bad year, not a steward, a year with no periods) shows why. The undo button takes back the last
 *  rollover (only by whoever made it, on the sheet-day it was made, while no claim belongs to a period it generated):
 *  its periods are removed and its opening balances taken back; a refusal shows why in the same error. */
function Rollover({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [year, setYear] = useState('');
  const [made, setMade] = useState<number | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const rollover = useMutation({
    mutationFn: (body: { user: string; year: string }) =>
      postJson<RolloverResult>(`/api/sheets/${encodeURIComponent(sheetId)}/rollover`, body),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
      setMade(r.made);
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : 'Could not roll the year over.'),
    onSettled: once.done,
  });

  const undo = useMutation({
    mutationFn: (body: { user: string }) =>
      postJson<unknown>(`/api/sheets/${encodeURIComponent(sheetId)}/rollover-undo`, body),
    onSuccess: async () => {
      setMade(null);
      // The periods and every figure that reads them (carried figures, period dues) change.
      await queryClient.invalidateQueries();
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : 'Could not undo the rollover.'),
    onSettled: once.done,
  });

  const undoLast = () => {
    setError('');
    if (!once.start()) return;
    undo.mutate({ user });
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setMade(null);
    if (!/^\d{4}$/.test(year.trim())) {
      setError('The fiscal year must be a year, yyyy.');
      return;
    }
    if (!once.start()) return;
    rollover.mutate({ user, year: year.trim() });
  };

  return (
    <form data-testid="rollover-form" onSubmit={submit}>
      <label>
        Roll over FY{' '}
        <input
          data-testid="rollover-year"
          placeholder="yyyy"
          value={year}
          onChange={(e) => {
            setYear(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="rollover" disabled={rollover.isPending}>
        Roll year over
      </button>
      {' '}
      <button type="button" data-testid="rollover-undo" disabled={undo.isPending} onClick={undoLast}>
        Undo last rollover
      </button>
      {made !== null && (
        <>
          {' '}Periods made <span data-testid="rollover-made">{made}</span>
        </>
      )}
      {error && (
        <p data-testid="rollover-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: 6.2, Component: Rollover });
