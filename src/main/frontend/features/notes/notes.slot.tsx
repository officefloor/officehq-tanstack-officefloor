import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { notesKey, useNotes } from './notes';
import type { Note } from './notes';

// The sheet's notes: a text entry and an add button, the notes listed below newest first as
// '<sheet's today when added> <user>: <text>'. A blank note is refused.
function Notes({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const notes = useNotes(sheetId);
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<Note>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`, body, user),
    onSuccess: () => {
      setText('');
      return queryClient.invalidateQueries({ queryKey: notesKey(sheetId) });
    },
    onError: () => setError('That note could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const value = text.trim();
    if (value === '') {
      setError('A note needs text.');
      return;
    }
    once((done) => add.mutate({ text: value }, { onSettled: done }));
  };

  return (
    <section data-testid="notes">
      <h2>Notes</h2>
      <form onSubmit={submit}>
        <label>
          Note{' '}
          <input
            data-testid="note-text"
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="note-add" disabled={add.isPending}>
          Add note
        </button>
        {error && (
          <p role="alert" data-testid="note-error">
            {error}
          </p>
        )}
      </form>
      <ul data-testid="note-list">
        {(notes.data ?? []).map((n, i) => (
          <li key={i} data-testid="note-line">
            {`${n.addedOn} ${n.addedBy}: ${n.text}`}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -5.4, Component: Notes });
