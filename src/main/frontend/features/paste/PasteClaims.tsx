import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { claimsKey } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';

type PasteResult = { added: number };

// The paste box: lines of name,amount,date,note (the note may hold commas, a blank date is the sheet's entry date,
// else its today, the note may be left off), each a claim for the named person on the sheet, opened as the open
// button would. A line matching a DRAFT of theirs (same date and note) updates it to its amount and opens it instead.
// The server refuses the whole paste when any line is refused, so one refused line changes nothing.
// A note ending " #<reference>" gives the claim that external reference instead.
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
      // Re-read people and claims first, then show them with the count in one render: the new rows never
      // show before the count does.
      const [people, claims] = await Promise.all([
        getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
        getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims`),
      ]);
      queryClient.setQueryData(peopleKey(sheet), people);
      queryClient.setQueryData(claimsKey(sheet), claims);
      setAdded(result.added);
      // Opening moves the figures and numbers shown elsewhere on the screen.
      void queryClient.invalidateQueries();
    },
    onError: () => setError('Nothing was pasted: every line must be a claim the claim form takes and the open button opens, for a name on the sheet.'),
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
          Pasted <span data-testid="paste-added">{added}</span>
        </>
      )}
      {error && <span data-testid="paste-error">{error}</span>}
    </form>
  );
}
