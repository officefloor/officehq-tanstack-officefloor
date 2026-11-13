import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { claimsKey } from '../claims/claims';
import { peopleKey } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';

type PasteResult = { added: number };

// The paste box: lines of name,amount,date,note (the note may hold commas, a blank date is the sheet's
// today, the note may be left off), each a DRAFT claim for the named person on the sheet. The server
// checks every line with the claim form's rules first, so one refused line adds nothing.
export function PasteClaims() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [added, setAdded] = useState<number | undefined>(undefined);
  const once = useOneAtATime();
  const queryClient = useQueryClient();

  const paste = useMutation({
    mutationFn: (pasted: string) =>
      postJson<PasteResult>(
        `/api/sheets/${encodeURIComponent(sheet)}/paste-claims?user=${encodeURIComponent(user)}`,
        { text: pasted },
      ),
    onSuccess: async (result) => {
      setText('');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]);
      setAdded(result.added);
    },
    onError: () => setError('Nothing was added: every line must be a claim the claim form takes, for a name on the sheet.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setAdded(undefined);
    if (!text.trim()) {
      setError('Paste at least one line of name,amount,date,note.');
      return;
    }
    once(() => paste.mutateAsync(text));
  };

  return (
    <form onSubmit={submit} data-testid="paste-form">
      <textarea
        data-testid="paste-claims"
        placeholder="name,amount,date,note — one claim per line"
        rows={4}
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="paste-submit" disabled={paste.isPending}>
        Paste claims
      </button>
      {added !== undefined && (
        <>
          {' '}
          Added <span data-testid="paste-added">{added}</span>
        </>
      )}
      {error && <span data-testid="paste-error">{error}</span>}
    </form>
  );
}
