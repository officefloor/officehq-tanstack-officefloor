import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { notesKey, useNotes } from './notes';
import type { Note } from './notes';

function NoteList({ sheetId }: { sheetId: string }) {
  const notes = useNotes(sheetId).data ?? [];
  return (
    <ul>
      {notes.map((n) => (
        <li key={n.id} data-testid="note-line">
          {`${n.on} ${n.by}: ${n.text}`}
        </li>
      ))}
    </ul>
  );
}

function NoteForm({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (t: string) => postJson<Note>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`, { user, text: t }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: notesKey(sheetId) });
      setText('');
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not add the note.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (text.trim() === '') {
      setError("Give the note's text.");
      return;
    }
    if (!once.start()) return;
    save.mutate(text.trim());
  };

  return (
    <form data-testid="note-form" onSubmit={submit}>
      <input
        data-testid="note-text"
        placeholder="Note"
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          setError('');
        }}
      />{' '}
      <button type="submit" data-testid="note-add" disabled={save.isPending}>
        Add note
      </button>
      {error && (
        <p data-testid="note-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

/** The sheet's notes, newest first, each as "<the sheet's today when added> <user>: <text>", and the entry a note is
 *  added with. */
function Notes({ sheetId, user }: { sheetId: string; user: string }) {
  return (
    <section data-testid="notes">
      <h2>Notes</h2>
      <NoteForm sheetId={sheetId} user={user} />
      <NoteList sheetId={sheetId} />
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7.65, Component: Notes });
