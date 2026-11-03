import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/usePeople';

/** A paste box: lines of name,amount,date,note (the note may hold commas; a blank date is the sheet's
 *  today; a note ending in " #<reference>" gives the claim that external reference) each add a claim, in order, for the person on the sheet with that name, and open it as the open
 *  button would; a line matching one of the person's claims by date and note updates the newest such DRAFT
 *  instead and opens it. One refused line (or open) refuses the paste and changes nothing. Shows how many
 *  lines went through (gone while it runs). */
function PasteClaims({ sheetId }: { sheetId: string }) {
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const paste = useMutation({
    mutationFn: (t: string) => postJson<{ added: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/paste`, { text: t }),
    // The count shows as soon as the paste is done; the new rows follow once re-read (not awaited, so the
    // rows never show ahead of the count).
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  return (
    <form
      data-testid="paste"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          paste.reset();
          return paste.mutateAsync(text);
        });
      }}
    >
      <label>
        Paste claims (name,amount,date,note per line){' '}
        <textarea
          data-testid="paste-claims"
          rows={4}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            paste.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="paste-submit" disabled={paste.isPending}>
        Add and open claims
      </button>
      {paste.isSuccess && (
        <>
          {' '}
          Added <strong data-testid="paste-added">{paste.data.added}</strong>
        </>
      )}
      {paste.isError && <p data-testid="paste-error">{paste.error.message}</p>}
    </form>
  );
}

export const contribution = SheetBody.fill({
  order: 14,
  Component: ({ sheetId }) => <PasteClaims sheetId={sheetId} />,
});
