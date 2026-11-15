import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey, pasteClaims } from './api';

/** A box that adds a DRAFT claim per pasted line of name,amount,date,note, and how many it added.
 *  The count is cleared while it runs; a refused paste adds nothing and shows an error. */
export function PasteClaims({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const submitOnce = useSubmitOnce();

  const paste = useMutation({
    mutationFn: (input: string) => pasteClaims(sheetId, user, input),
    // Not awaited: the count shows as soon as the paste is done, never after its new rows.
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }); },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const input = text;
        submitOnce(() => {
          paste.reset();
          return paste.mutateAsync(input);
        });
      }}
    >
      <label>
        Paste claims (name,amount,date,note per line; end the note with " #ref" to give a reference){' '}
        <textarea data-testid="paste-claims" rows={3} value={text}
          onChange={(e) => {
            setText(e.target.value);
            if (paste.isError) paste.reset();
          }} />
      </label>{' '}
      <button type="submit" data-testid="paste-submit" disabled={paste.isPending}>
        Add pasted claims
      </button>
      {paste.isSuccess && (
        <>
          {' '}Added <span data-testid="paste-added">{paste.data.added}</span>
        </>
      )}
      {paste.isError && (
        <span data-testid="paste-error">Every line needs a name on the sheet and an amount of 0.00 or more with up to two decimals, dated on or before the person's retirement and outside a closed period: nothing was added.</span>
      )}
    </form>
  );
}
