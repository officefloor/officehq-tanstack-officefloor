import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';

/** A note on the sheet: the sheet's today when it was added (yyyy-mm-dd), who added it and its text. */
export type Note = { date: string; by: string; text: string };

/** The key every feature reads the sheet's notes under. */
export const notesKey = (sheetId: string) => ['sheet', sheetId, 'notes'] as const;

/** The sheet's notes, newest first; as of a date, those added while the sheet's today was on or before it. */
export function useNotes(sheetId: string) {
  return useQuery({
    queryKey: notesKey(sheetId),
    queryFn: () => getJson<Note[]>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`),
  });
}

// The sheet's notes: a text entry adds a note, dated the sheet's today; a blank note is refused.
function NotesPanel({ sheetId }: { sheetId: string; user: string }) {
  const notes = useNotes(sheetId);
  const [text, setText] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (value: string) => {
      if (value.trim() === '') throw new Error('Enter a note');
      // `by` is the signed-in user the change is recorded against.
      return postJson<Note>(`/api/sheets/${encodeURIComponent(sheetId)}/notes`, { text: value.trim(), by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: notesKey(sheetId) });
      setText('');
    },
  });

  const submit = useSubmitOnce(add);
  return (
    <section data-testid="notes-panel">
      <h2>Notes</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit(text);
        }}
      >
        <input
          type="text"
          data-testid="note-text"
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="note-add" disabled={add.isPending}>
          Add note
        </button>
        {add.isError && (
          <span data-testid="note-error" role="alert">
            {' '}
            {add.error.message}
          </span>
        )}
      </form>
      <ul data-testid="note-list">
        {(notes.data ?? []).map((n, i) => (
          <li key={i} data-testid="note-line">
            {`${n.date} ${n.by}: ${n.text}`}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 8.8, Component: NotesPanel });
