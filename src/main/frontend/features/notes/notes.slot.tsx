import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { notesPath, notesQueryKey, useNotes, type Note } from './notes';

// The sheet's notes, newest first, and the entry that adds one. A blank note (or one a viewer, an archived sheet or
// the as-of view refuses) shows the error beside the entry, which goes as soon as the entry changes.
function Notes() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const notes = useNotes();
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { text: string }) => postJson<Note>(notesPath(sheet ?? ''), body, user),
    onSuccess: async (_added, body) => {
      // Cleared before the list reloads, and only if it still holds the text added, so a text typed meanwhile stays.
      setText((t) => (t === body.text ? '' : t));
      await queryClient.invalidateQueries({ queryKey: notesQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(add);

  if (!user || !sheet) return null;
  return (
    <section data-testid="notes">
      <h2>Notes</h2>
      <ul>
        {(notes.data ?? []).map((n) => (
          <li key={n.id} data-testid="note-line">{`${n.date} ${n.user}: ${n.text}`}</li>
        ))}
      </ul>
      <form
        data-testid="note-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ text });
        }}
      >
        <input
          data-testid="note-text"
          placeholder="Note"
          value={text}
          onChange={(e) => { setText(e.target.value); add.reset(); }}
        />
        <button type="submit" data-testid="note-add" disabled={add.isPending}>
          Add note
        </button>
        {add.isError && <span data-testid="note-error">The note could not be added: it must have a text.</span>}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 6.9, Component: Notes });
