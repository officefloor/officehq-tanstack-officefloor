import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { periodsKey, rollover } from './api';

/** A year entry and a button that rolls that fiscal year over (a steward only), and how many periods it
 *  made. The count is cleared while it runs; a refused rollover writes nothing and shows an error. */
export function Rollover({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [year, setYear] = useState('');
  const submitOnce = useSubmitOnce();

  const run = useMutation({
    mutationFn: (input: string) => rollover(sheetId, user, input),
    // Not awaited: the count shows as soon as the rollover is done.
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }); },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const input = year;
        submitOnce(() => {
          run.reset();
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
          }} />
      </label>{' '}
      <button type="submit" data-testid="rollover" disabled={run.isPending}>
        Roll over
      </button>
      {run.isSuccess && (
        <>
          {' '}Made <span data-testid="rollover-made">{run.data.made}</span>
        </>
      )}
      {run.isError && (
        <span data-testid="rollover-error">Only a steward may roll over a fiscal year, given as yyyy, that has periods: nothing was changed.</span>
      )}
    </form>
  );
}
