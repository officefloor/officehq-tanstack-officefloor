import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsKey, claimsQuery } from '../claims/claimsQuery';
import { peopleKey, peopleQuery } from '../people/peopleQuery';
import { useUser } from '../session/useUser';

/**
 * The paste box: lines of name,amount,date,note each add a claim, in order, for the person of that name or alias
 * on the sheet (ignoring case), and open it as its open button would; a line matching a DRAFT of that person (same
 * date and note) updates and opens that DRAFT instead. The server checks every line first, and stores nothing when any
 * line, or any open, is refused. Shows how many lines went through.
 */
function PasteClaims({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [added, setAdded] = useState('');
  const once = useSubmitOnce();
  const paste = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<{ added: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/paste`, body, user),
    onSuccess: async (res) => {
      // Both lists are re-read first and shown together with the count, so the new rows never show before it.
      const [people, claims] = await Promise.all([peopleQuery(sheetId).queryFn(), claimsQuery(sheetId).queryFn()]);
      queryClient.setQueryData(peopleKey(sheetId), people);
      queryClient.setQueryData(claimsKey(sheetId), claims);
      setText('');
      setAdded(String(res.added));
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
    onError: (e) =>
      setError(e instanceof RefusedError ? e.message : 'The lines could not be pasted: check every name, amount and date.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setAdded('');
    if (text.trim() === '') {
      setError('Paste at least one line of name,amount,date,note.');
      return;
    }
    once(() => paste.mutateAsync({ text }));
  };

  return (
    <form data-testid="paste-box" onSubmit={submit}>
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
        Paste
      </button>{' '}
      Added: <span data-testid="paste-added">{added}</span>
      {error && <p data-testid="paste-error">{error}</p>}
    </form>
  );
}

// Below the quick-add bar; keyed by sheet, so pasted text, a count or an error never carries over.
export const contribution = SheetPanel.fill({
  order: 10,
  Component: ({ sheetId }) => <PasteClaims key={sheetId} sheetId={sheetId} />,
});
