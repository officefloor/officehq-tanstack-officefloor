import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { claimsKey } from './claims';

type PasteResult = { added: number };

/** The paste box: one name,amount,date,note line per claim (a blank date is the sheet's entry date while it is set, else its today, the note may be
 *  absent and may hold commas, and a note ending " #<reference>" gives the claim that reference instead). The server checks every line by the claim form's rules first; one failing line
 *  refuses the whole paste and shows why on the box, otherwise each line is added in order as a DRAFT claim for
 *  the named person and the box shows how many were added. */
function PasteClaims({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [added, setAdded] = useState<number | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const paste = useMutation({
    mutationFn: (body: { user: string; text: string }) =>
      postJson<PasteResult>(`/api/sheets/${encodeURIComponent(sheetId)}/paste-claims`, body),
    onSuccess: async (r) => {
      setAdded(r.added);
      setText('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(
        e instanceof HttpError && e.body
          ? e.body
          : 'Could not paste: each line is name,amount,date,note for a person on the sheet.',
      ),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setAdded(null);
    if (text.trim() === '') {
      setError('Paste at least one line: name,amount,date,note.');
      return;
    }
    if (!once.start()) return;
    paste.mutate({ user, text });
  };

  return (
    <form data-testid="paste-form" onSubmit={submit}>
      <label>
        Paste claims (name,amount,date,note per line){' '}
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
        Add pasted claims
      </button>
      {added !== null && (
        <>
          {' '}Added <span data-testid="paste-added">{added}</span>
        </>
      )}
      {error && (
        <p data-testid="paste-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: 14, Component: PasteClaims });
