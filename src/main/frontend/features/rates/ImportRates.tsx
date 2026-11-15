import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { importRates, ratesKey } from './api';

/** A box that sets a rate per pasted line of grade,rate,from,to, and how many it set.
 *  The count is cleared while it runs; a refused import sets nothing and shows an error. */
export function ImportRates({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const submitOnce = useSubmitOnce();

  const run = useMutation({
    mutationFn: (input: string) => importRates(sheetId, user, input),
    // Not awaited: the count shows as soon as the import is done.
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) }); },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const input = text;
        submitOnce(() => {
          run.reset();
          return run.mutateAsync(input);
        });
      }}
    >
      <label>
        Import rates (grade,rate,from,to per line; from and to may be left out){' '}
        <textarea data-testid="import-rates" rows={3} value={text}
          onChange={(e) => {
            setText(e.target.value);
            if (run.isError) run.reset();
          }} />
      </label>{' '}
      <button type="submit" data-testid="import-rates-submit" disabled={run.isPending}>
        Import rates
      </button>
      {run.isSuccess && (
        <>
          {' '}Set <span data-testid="import-rates-set">{run.data.set}</span>
        </>
      )}
      {run.isError && (
        <span data-testid="import-rates-error">Every line needs a grade of A, B or C, a rate of 0.00 or more with up to two decimals, and a from-date and to-date that are dates or blank, the to-date not before the from-date: nothing was set.</span>
      )}
    </form>
  );
}
