import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Note, notesKey, useNotes } from './notes';

// The sheet's notes, newest first, and the entry a note is added with. A blank note is refused.
function Notes() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const notes = useNotes(sheet);
  const [text, setText] = useState('');
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationFn: (t: string) => postJson<Note>(`/api/sheets/${encodeURIComponent(sheet)}/notes`, { text: t, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: notesKey(sheet) });
      setText('');
    },
  });
  const submit = useSubmitOnce(add);

  return (
    <section data-testid="notes">
      <h2>Notes</h2>
      <ul>
        {(notes.data ?? []).map((n) => (
          <li key={n.id} data-testid="note-line">
            {`${n.date} ${n.user}: ${n.text}`}
          </li>
        ))}
      </ul>
      <form
        data-testid="note-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(text);
        }}
      >
        <input
          data-testid="note-text"
          placeholder="Note"
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            add.reset();
          }}
        />
        <button type="submit" data-testid="note-add" disabled={add.isPending}>
          Add note
        </button>
        {add.isError && (
          <span data-testid="note-error" role="alert">
            {' '}
            {add.error instanceof HttpError && add.error.reason ? `${add.error.reason}.` : 'The note was refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 47.95, Component: Notes });
