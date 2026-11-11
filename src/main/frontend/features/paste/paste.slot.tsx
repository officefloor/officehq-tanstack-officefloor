import { useState } from 'react';
import type { FormEvent } from 'react';
import { useIsFetching, useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { peopleKey } from '../people/people';
import { claimsKey } from '../claims/claims';

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
  const rereading =
    useIsFetching({ queryKey: peopleKey(sheetId) }) + useIsFetching({ queryKey: claimsKey(sheetId) }) > 0;

  const once = useOnce();
  const paste = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/paste`, body, user),
    // The count shows once the lists are re-read, so the pasted claims are listed when it appears:
    // it is kept at once, while the re-read is under way, and held back until that is done, so the
    // rows and the count show in the same render.
    onSuccess: async (r) => {
      setText('');
      const reread = Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
      setResult(r);
      await reread;
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
        {result !== null && !rereading && (
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
