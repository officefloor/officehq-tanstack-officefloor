import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { periodsKey, rollover, undoRollover } from './api';

/** A year entry and a button that rolls that fiscal year over (a steward only), and how many periods it
 *  made. The count is cleared while it runs; a refused rollover writes nothing and shows an error. A second
 *  button undoes the last rollover (its steward, on the same sheet-today); a refused undo shows the same error. */
export function Rollover({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [year, setYear] = useState('');
  const submitOnce = useSubmitOnce();

  const run = useMutation({
    mutationFn: (input: string) => rollover(sheetId, user, input),
    // Not awaited: the count shows as soon as the rollover is done.
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }); },
  });
  const undo = useMutation({
    mutationFn: () => undoRollover(sheetId, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const input = year;
        submitOnce(() => {
          run.reset();
          undo.reset();
          return run.mutateAsync(input);
        });
      }}
    >
      <label>
        Roll over fiscal year{' '}
        <input data-testid="rollover-year" value={year} placeholder="yyyy"
          onChange={(e) => {
            setYear(e.target.value);
            run.reset();
            undo.reset();
          }} />
      </label>{' '}
      <button type="submit" data-testid="rollover" disabled={run.isPending}>
        Roll over
      </button>
      <button type="button" data-testid="rollover-undo" disabled={undo.isPending}
        onClick={() => {
          run.reset();
          undo.reset();
          undo.mutate();
        }}>
        Undo last rollover
      </button>
      {run.isSuccess && (
        <>
          {' '}Made <span data-testid="rollover-made">{run.data.made}</span>
        </>
      )}
      {run.isError && (
        <span data-testid="rollover-error">Only a steward may roll over a fiscal year, given as yyyy, that has periods: nothing was changed.</span>
      )}
      {undo.isError && (
        <span data-testid="rollover-error">Only the steward who made the last rollover may undo it, on the same day, while no claim
          belongs to a period it made: nothing was changed.</span>
      )}
    </form>
  );
}
