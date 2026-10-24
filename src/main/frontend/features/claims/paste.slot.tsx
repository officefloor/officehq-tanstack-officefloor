import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peopleQueryKey } from '../people/people';
import { claimsQueryKey } from './claims';

// The paste box: each line is name,amount,date,note (the note may hold commas; the date may be blank for the
// sheet's entry date, else its today; the note may be absent) and adds a DRAFT claim, in order, for the named person on the sheet.
// The server checks every line by the claim form's rules first, so one refused line adds nothing. How many were
// added shows once done, and is gone while it runs.
function PasteClaims() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const paste = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<{ added: number }>(`/api/sheets/${encodeURIComponent(sheet ?? '')}/paste-claims`, body, user),
    // Not awaited: the count shows before the new rows do.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
      void queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(paste);
  if (!user || !sheet) return null;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submit({ text });
      }}
    >
      <h2>Paste claims</h2>
      <textarea
        data-testid="paste-claims"
        placeholder="name,amount,date,note — one claim per line"
        rows={4}
        value={text}
        onChange={(e) => { setText(e.target.value); paste.reset(); }}
      />
      <button type="submit" data-testid="paste-submit" disabled={paste.isPending}>
        Add draft claims
      </button>
      {paste.isSuccess && (
        <>
          {' '}Added <span data-testid="paste-added">{paste.data.added}</span>
        </>
      )}
      {paste.isError && (
        <span data-testid="paste-error">
          Nothing was added: every line needs a name on the sheet and an amount, 0.00 or more, with up to two
          decimals; a date must be a calendar date, due by a date in no closed period and not after the person's retirement.
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 16, Component: PasteClaims });
