import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { peopleKey } from '../people/people';
import { claimsKey } from './claims';

type PasteResult = { added: number };

// The paste box: lines of name,amount,date,note each add a claim for the named person (or update
// the DRAFT of theirs with that date and note) and open it. The server checks every line first and
// refuses the whole paste if any line, or any open, fails; the count of lines that went through
// shows once it answers (gone while it runs).
function PasteClaims() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const queryClient = useQueryClient();

  const paste = useMutation({
    mutationFn: (t: string) =>
      postJson<PasteResult>(`/api/sheets/${encodeURIComponent(sheet)}/paste-claims`, { text: t, user }),
    // Not awaited: the count added shows as soon as the server answers, no later than the rows it added.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });
  const submit = useSubmitOnce(paste);

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submit(text);
      }}
    >
      <h2>Paste claims</h2>
      <textarea
        data-testid="paste-claims"
        placeholder="name,amount,date,note — one claim per line"
        rows={4}
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          paste.reset();
        }}
      />
      <button type="submit" data-testid="paste-submit" disabled={paste.isPending}>
        Add and open claims
      </button>
      {paste.isSuccess && (
        <>
          {' '}
          Went through <strong data-testid="paste-added">{paste.data.added}</strong>
        </>
      )}
      {paste.isError && (
        <span data-testid="paste-error" role="alert">
          {' '}
          {paste.error instanceof HttpError && paste.error.reason ? `${paste.error.reason}.` : 'The paste was refused.'}
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 16, Component: PasteClaims });
