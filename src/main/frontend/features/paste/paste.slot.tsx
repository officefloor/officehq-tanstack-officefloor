import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { claimsKey } from '../claims/claims';
import { peopleKey } from '../people/people';

type Result = { added: number };

/** The paste box: lines of name,amount,date,note (the note may hold commas and may be absent; a blank date is the
 *  sheet's today) each add a DRAFT claim, in order, for the person with the name. The server checks every line by the
 *  claim form's rules first: one failing line refuses the paste and adds nothing. Shows how many were added; the
 *  count is cleared while a new press runs. */
function PasteClaims({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const submit = useSubmitOnce();
  const paste = useMutation({
    mutationFn: (t: string) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/paste-claims`, { text: t, by: user }),
    onSuccess: () => {
      setText('');
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!submit.begin()) return;
        paste.reset();
        paste.mutate(text);
      }}
    >
      <label>
        Paste claims (name,amount,date,note per line)
        <br />
        <textarea
          data-testid="paste-claims"
          rows={4}
          cols={50}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            paste.reset();
          }}
        />
      </label>
      <br />
      <button type="submit" data-testid="paste-submit" disabled={paste.isPending}>
        Add draft claims
      </button>
      {paste.isSuccess && (
        <>
          {' '}
          Added <span data-testid="paste-added">{paste.data.added}</span>
        </>
      )}
      {paste.isError && (
        <span data-testid="paste-error">
          {' '}
          Nothing was added: every line must name a person on the sheet and keep the claim form's rules.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPage.fill({ order: 24, Component: PasteClaims });
