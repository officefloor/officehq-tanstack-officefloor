import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { notesKey, notesPath, useNotes } from './notes';
import type { Note } from './notes';

// The sheet's notes, newest first, each shown with the sheet's today when it was added and who added it, and a form
// that adds one. The server refuses a blank note.
function Notes({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: notes = [] } = useNotes(sheet);
  const [text, setText] = useState('');

  const add = useMutation({
    mutationFn: (body: { text: string }) => postJson<Note[]>(byUser(notesPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(notesKey(sheet), updated);
      setText('');
    },
  });
  const send = useSubmitOnce(add);

  const change = (next: string) => {
    setText(next);
    add.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send({ text });
  };

  return (
    <section>
      <h2>Notes</h2>
      <form onSubmit={submit}>
        <label>
          Note <input data-testid="note-text" value={text} onChange={(e) => change(e.target.value)} />
        </label>{' '}
        <button type="submit" data-testid="note-add" disabled={add.isPending}>
          Add note
        </button>
        {add.isError && (
          <span data-testid="note-error" role="alert">
            {' '}A note needs some text, and a sheet you may change.
          </span>
        )}
      </form>
      <ul>
        {notes.map((n) => (
          <li key={n.id} data-testid="note-line">
            {n.on} {n.by}: {n.text}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 9.6, Component: Notes });
