import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { notesKey, useNotes, type Note } from './useNotes';

/** The sheet's notes: a text and an add button (a blank note is refused), and the notes newest first, each
 *  shown as <the sheet's today when added> <user>: <text>. */
function Notes({ sheetId }: { sheetId: string }) {
  const notes = useNotes(sheetId);
  const queryClient = useQueryClient();
  const [text, setText] = useState('');

  const submitOnce = useSingleFlight();
  const add = useMutation({
    mutationFn: (f: { text: string }) =>
      postJson<{ notes: Note[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`, f),
    onSuccess: async () => {
      setText('');
      await queryClient.invalidateQueries({ queryKey: notesKey(sheetId) });
    },
  });

  return (
    <section data-testid="notes">
      <form
        data-testid="note-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            add.reset();
            return add.mutateAsync({ text });
          });
        }}
      >
        <label>
          Note{' '}
          <input
            data-testid="note-text"
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              add.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="note-add" disabled={add.isPending}>
          Add note
        </button>
        {add.isError && <p data-testid="note-error">{add.error.message}</p>}
      </form>
      <ul data-testid="notes-list">
        {(notes.data ?? []).map((n) => (
          <li key={n.id} data-testid="note-line">{`${n.addedOn} ${n.addedBy}: ${n.text}`}</li>
        ))}
      </ul>
      {notes.isError && <p data-testid="notes-error">{notes.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 8, Component: ({ sheetId }) => <Notes sheetId={sheetId} /> });
