import { useState } from 'react';
import type { FormEvent } from 'react';
import { notifyManager, useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { peopleKey, peopleQuery } from '../people/people';
import { claimsKey, claimsQuery } from '../claims/claims';

type Result = { added: number };

// The paste box: lines of name,amount,date,note (the date may be blank for the sheet's entry date while set, else its today, the
// note may be absent and may hold commas) each add a claim, in order, for the named person on the
// sheet and open it as the open button would; a line matching one of their DRAFTs by date and note
// updates that DRAFT to its amount and opens it instead. The server checks every line first: one bad
// line or refused open refuses the whole paste and changes nothing. The box shows how many lines went
// through; the count is cleared while it runs.
function PasteClaims({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const paste = useMutation({
    // A re-read already under way could answer after the paste is stored and list its claims
    // before the count shows: it is cancelled first, and the lists are re-read once the paste is done.
    mutationFn: async (body: { text: string }) => {
      await Promise.all([
        queryClient.cancelQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.cancelQueries({ queryKey: claimsKey(sheetId) }),
      ]);
      return postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/paste`, body, user);
    },
    // The count shows once the lists are re-read, so the pasted claims are listed when it appears:
    // the people and the claims are read again together and stored, with the count, in one batch,
    // so the rows and the count show in the same render. What else hangs off the people and the
    // claims is re-read after.
    onSuccess: async (r) => {
      setText('');
      const [people, claims] = await Promise.all([
        peopleQuery(sheetId).queryFn(),
        claimsQuery(sheetId).queryFn(),
      ]);
      notifyManager.batch(() => {
        queryClient.setQueryData(peopleKey(sheetId), people);
        queryClient.setQueryData(claimsKey(sheetId), claims);
        notifyManager.schedule(() => setResult(r));
      });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId), predicate: (q) => q.queryKey.length > 3 }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId), predicate: (q) => q.queryKey.length > 3 }),
      ]);
    },
    onError: () => setError('Those claims could not be added and opened: check every line.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setResult(null);
    setError('');
    if (text.trim() === '') {
      setError('Paste at least one line of name,amount,date,note.');
      return;
    }
    once((done) => paste.mutate({ text }, { onSettled: done }));
  };

  return (
    <section data-testid="paste-box">
      <form onSubmit={submit}>
        <label>
          Paste claims (name,amount,date,note){' '}
          <textarea
            data-testid="paste-claims"
            rows={4}
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="paste-submit" disabled={paste.isPending}>
          Add and open pasted claims
        </button>
        {result !== null && (
          <>
            {' '}Went through <span data-testid="paste-added">{result.added}</span>
          </>
        )}
        {error && (
          <p role="alert" data-testid="paste-error">
            {error}
          </p>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 1.5, Component: PasteClaims });
