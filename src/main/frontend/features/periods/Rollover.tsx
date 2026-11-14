import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';

// The year-end rollover, by a steward: the next fiscal year's periods, made as the fiscal year form makes them, with
// each person's open figure at the old year's end carried into the new year's first period. The steward who made the
// last rollover may undo it on the same sheet-today, while no claim belongs to a period it made.
export function Rollover() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [year, setYear] = useState('');
  const [error, setError] = useState('');
  const [made, setMade] = useState<number | undefined>(undefined);
  const once = useOneAtATime();
  const queryClient = useQueryClient();

  const rollover = useMutation({
    mutationFn: (input: { year: string }) =>
      postJson<{ made: number }>(
        `/api/sheets/${encodeURIComponent(sheet)}/rollover?user=${encodeURIComponent(user)}`,
        input,
      ),
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
      setMade(result.made);
    },
    onError: () =>
      setError('The year was not rolled over: type a fiscal year with periods, as yyyy; only a steward may roll it over.'),
  });

  const undo = useMutation({
    mutationFn: () =>
      postJson<{ fy: string; removed: number }>(
        `/api/sheets/${encodeURIComponent(sheet)}/rollover/undo?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    },
    onError: () =>
      setError(
        'The rollover was not undone: only the steward who made the last rollover may undo it, on the same day, ' +
          'while no claim belongs to a period it made.',
      ),
  });

  if (!user || !sheet) return null;

  const undoLast = () => {
    setError('');
    setMade(undefined);
    once(() => undo.mutateAsync());
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setMade(undefined);
    const typed = year.trim();
    if (!/^\d{4}$/.test(typed)) {
      setError('Type the fiscal year as yyyy.');
      return;
    }
    once(() => rollover.mutateAsync({ year: typed }));
  };

  return (
    <form onSubmit={submit} data-testid="rollover-form">
      <input
        data-testid="rollover-year"
        placeholder="Fiscal year to roll over, yyyy"
        value={year}
        onChange={(e) => {
          setYear(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="rollover" disabled={rollover.isPending}>
        Roll the year over
      </button>
      <button type="button" data-testid="rollover-undo" disabled={undo.isPending} onClick={undoLast}>
        Undo the last rollover
      </button>
      {made !== undefined && (
        <>
          {' '}
          Made <span data-testid="rollover-made">{made}</span>
        </>
      )}
      {error && <span data-testid="rollover-error">{error}</span>}
    </form>
  );
}
