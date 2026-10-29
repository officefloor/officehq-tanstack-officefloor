import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { notesKey, notesQuery } from './notesQuery';
import type { Note } from './notesQuery';

/** The sheet's notes, newest first, and the entry a note is added through. A blank note is refused. */
function Notes({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const notes = useQuery(notesQuery(sheetId));
  const [text, setText] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<Note>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: notesKey(sheetId) });
      setText('');
    },
    onError: (e: Error) => setError(e.message || 'The note could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => save.mutateAsync({ text: text.trim() }));
  };

  return (
    <section data-testid="notes">
      <h3>Notes</h3>
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
        <button type="submit" data-testid="note-add" disabled={save.isPending}>
          Add note
        </button>
        {error && <p data-testid="note-error">{error}</p>}
      </form>
      {notes.isError && <p data-testid="notes-load-error">Could not load the notes.</p>}
      <ul data-testid="notes-list">
        {(notes.data ?? []).map((n) => (
          <li key={n.id} data-testid="note-line">
            {`${n.on} ${n.by}: ${n.text}`}
          </li>
        ))}
      </ul>
    </section>
  );
}

// Keyed by sheet, so a typed value or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 6.65,
  Component: ({ sheetId }) => <Notes key={`notes-${sheetId}`} sheetId={sheetId} />,
});
