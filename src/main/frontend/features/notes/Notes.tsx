import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { notesKey, useNotes } from './notes';
import type { Note } from './notes';
import { useOneAtATime } from '../../query/useOneAtATime';

// The sheet's notes, newest first, and the entry a note is added with; a blank note is refused.
export function Notes() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const notes = useNotes().data ?? [];
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (input: { text: string }) =>
      postJson<Note[]>(`/api/sheets/${encodeURIComponent(sheet)}/notes?user=${encodeURIComponent(user)}`, input),
    onSuccess: async (saved) => {
      setText('');
      setError('');
      queryClient.setQueryData(notesKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: notesKey(sheet) });
    },
    onError: () => setError('That note was refused.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const note = text.trim();
    if (!note) {
      setError('Type the note.');
      return;
    }
    once(() => add.mutateAsync({ text: note }));
  };

  return (
    <section data-testid="notes">
      <ul data-testid="note-list">
        {notes.map((n, i) => (
          <li key={i} data-testid="note-line">
            {`${n.date} ${n.user}: ${n.text}`}
          </li>
        ))}
      </ul>
      <form onSubmit={submit} data-testid="note-form">
        <input
          data-testid="note-text"
          placeholder="Note"
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            setError('');
          }}
        />
        <button type="submit" data-testid="note-add" disabled={add.isPending}>
          Add note
        </button>
        {error && <span data-testid="note-error">{error}</span>}
      </form>
    </section>
  );
}
