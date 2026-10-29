import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { periodsKey } from './periodsQuery';

const YEAR = /^\d{4}$/;

/**
 * The year end rollover: a steward types a fiscal year (yyyy) that has periods. The server makes the next fiscal
 * year's twelve months as the fiscal year generator does and carries each person's open figure at the old year's end
 * into the new year's first period as an opening balance. Shows how many periods it made. The steward who made the
 * last rollover can undo it the same sheet-today.
 */
function Rollover({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [year, setYear] = useState('');
  const [error, setError] = useState('');
  const [made, setMade] = useState('');
  const once = useSubmitOnce();
  const rollover = useMutation({
    mutationFn: (body: { year: string }) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/rollover`, body, user),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
      setMade(String(r.made));
    },
    onError: (e) =>
      setError(e instanceof RefusedError ? e.message : 'The year could not be rolled over: only a steward rolls over a fiscal year (yyyy) that has periods.'),
  });

  // Undoing the last rollover: only its steward, on the sheet-today it was made, while no claim belongs to a period it made.
  const undo = useMutation({
    mutationFn: () =>
      postJson<{ fy: string; removed: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/rollover-undo`, {}, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries();
    },
    onError: (e) =>
      setError(
        e instanceof RefusedError
          ? e.message
          : 'The rollover could not be undone: only the steward who made the last rollover undoes it, on the same day, while no claim belongs to a period it made.',
      ),
  });

  const undoLast = () => {
    setError('');
    setMade('');
    once(() => undo.mutateAsync());
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setMade('');
    const y = year.trim();
    if (!YEAR.test(y)) {
      setError('The fiscal year must be a year (yyyy).');
      return;
    }
    once(() => rollover.mutateAsync({ year: y }));
  };

  return (
    <form data-testid="rollover-form" onSubmit={submit}>
      <label>
        Roll over fiscal year{' '}
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
      </button>{' '}
      Made: <span data-testid="rollover-made">{made}</span>{' '}
      <button type="button" data-testid="rollover-undo" onClick={undoLast} disabled={undo.isPending}>
        Undo last rollover
      </button>
      {error && <p data-testid="rollover-error">{error}</p>}
    </form>
  );
}

// Below the opening balances; keyed by sheet, so a year, a count or an error never carries over.
export const contribution = SheetPanel.fill({
  order: 7.2,
  Component: ({ sheetId }) => <Rollover key={sheetId} sheetId={sheetId} />,
});
