import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { notesKey, useNotes, type Note } from './notes';

/** The sheet's notes: the text entry and button that add one, and each note, newest first, as
 *  `<the sheet's today when added> <user>: <text>`. A blank note is refused. */
function Notes({ sheetId, user }: { sheetId: string; user: string }) {
  const notes = useNotes(sheetId);
  const queryClient = useQueryClient();
  const [text, setText] = useState('');

  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (t: string) => postJson<Note>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`, { text: t, by: user }),
    onSuccess: async () => {
      setText('');
      await queryClient.invalidateQueries({ queryKey: notesKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <section data-testid="notes">
      <h3>Notes</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          add.reset();
          add.mutate(text);
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
        />{' '}
        <button type="submit" data-testid="note-add" disabled={add.isPending}>
          Add note
        </button>
        {add.isError && <span data-testid="note-error"> The note could not be added.</span>}
      </form>
      {notes.isError && <p data-testid="notes-load-error">Could not load notes.</p>}
      <ul>
        {(notes.data ?? []).map((n, i) => (
          <li key={i} data-testid="note-line">
            {`${n.on} ${n.by}: ${n.text}`}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 9.7, Component: Notes });
