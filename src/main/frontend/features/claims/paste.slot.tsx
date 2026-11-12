import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { peopleKey } from '../people/people';
import { claimsKey } from './claims';

type PasteResult = { added: number };

// The paste box: lines of name,amount,date,note each add a DRAFT claim for the named person. The
// server checks every line with the claim form's rules first and refuses the whole paste if any
// fails; the count added shows once it answers (gone while it runs).
function PasteClaims() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const queryClient = useQueryClient();

  const paste = useMutation({
    mutationFn: (t: string) =>
      postJson<PasteResult>(`/api/sheets/${encodeURIComponent(sheet)}/paste-claims`, { text: t, user }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]);
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
        Add claims
      </button>
      {paste.isSuccess && (
        <>
          {' '}
          Added <strong data-testid="paste-added">{paste.data.added}</strong>
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
