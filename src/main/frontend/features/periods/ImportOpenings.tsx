import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { importOpenings, periodsKey } from './api';

/** A box that adds each pasted line of name,period,amount to the person's opening balance for the period,
 *  and how many lines went through. The count is cleared while it runs; a refused paste writes nothing
 *  and shows an error. */
export function ImportOpenings({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const submitOnce = useSubmitOnce();

  const run = useMutation({
    mutationFn: (input: string) => importOpenings(sheetId, user, input),
    // Not awaited: the count shows as soon as the paste is done.
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }); },
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
        Opening balances (name,period,amount per line){' '}
        <textarea data-testid="opening-import" rows={3} value={text}
          onChange={(e) => {
            setText(e.target.value);
            if (run.isError) run.reset();
          }} />
      </label>{' '}
      <button type="submit" data-testid="opening-import-submit" disabled={run.isPending}>
        Add openings
      </button>
      {run.isSuccess && (
        <>
          {' '}Added <span data-testid="opening-import-done">{run.data.imported}</span>
        </>
      )}
      {run.isError && (
        <span data-testid="opening-error">Every line needs a person on the sheet (by name or alias), a period on the sheet and an amount of 0.00 or more with two decimals: nothing was added.</span>
      )}
    </form>
  );
}
