import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from './usePeople';

// The paste box: one claim per line as name,amount,date,note (the note may hold commas; a blank date
// is the sheet's entry date while set, else its today; the note may be absent). Each line adds a DRAFT claim, in order, for the person
// of that name already on the sheet (case and outer spaces ignored). Every line is checked first by the
// claim form's rules; one refused line adds nothing. Then shows how many were added; pressing again
// removes the old count until the new one is in. A note ending " #<reference>" gives the claim that
// reference (not part of the note); a reference used on the sheet, or twice in the paste, refuses it all.
export function PasteClaims() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();

  const paste = useMutation({
    mutationFn: (pasted: string) =>
      postJson<{ added: number }>(`/api/sheets/${encodeURIComponent(sheet)}/paste-claims`, { text: pasted, user }),
    onSuccess: () => {
      setText('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]);
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          paste.reset();
          return paste.mutateAsync(text);
        });
      }}
    >
      <textarea
        data-testid="paste-claims"
        value={text}
        rows={3}
        placeholder="name,amount,date,note — one claim per line"
        onChange={(e) => {
          setText(e.target.value);
          paste.reset();
        }}
      />{' '}
      <button type="submit" data-testid="paste-submit">Paste claims</button>
      {paste.isSuccess && (
        <>
          {' '}
          Added <span data-testid="paste-added">{paste.data.added}</span>
        </>
      )}
      {paste.isError && (
        <span data-testid="paste-error" role="alert">
          {' '}
          Nothing was added: every line needs a name on the sheet and an amount of 0.00 or more with at most two
          decimals, and no claim whose due-by falls inside a closed period can be added or after the person's retirement date, and no reference may be used already on the sheet or twice in the paste.
        </span>
      )}
    </form>
  );
}
