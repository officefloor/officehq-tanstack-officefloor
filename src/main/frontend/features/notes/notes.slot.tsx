import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { notesKey, useNotes, type Note } from './useNotes';

/**
 * The sheet's notes, newest first, each as "<the sheet's today when added> <user>: <text>", and a
 * text entry to add one. A blank note is refused.
 */
function NotesPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: notes = [] } = useNotes(sheet);
  const [text, setText] = useState('');
  const [invalid, setInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (t: string) => postJson<Note>(`/api/sheets/${encodeURIComponent(sheet)}/notes`, { text: t, user }),
    onSuccess: async () => {
      setText('');
      await queryClient.invalidateQueries({ queryKey: notesKey(sheet) });
    },
  });

  // An error goes as soon as its form is changed or submitted again.
  const clearError = () => {
    add.reset();
    setInvalid(false);
  };

  return (
    <section data-testid="notes-panel">
      <h2>Notes</h2>
      <form
        data-testid="note-form"
        onSubmit={(e) => {
          e.preventDefault();
          const t = text.trim();
          if (t === '') {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            add.reset();
            setInvalid(true);
            return;
          }
          setInvalid(false);
          submitOnce(() => add.mutateAsync(t));
        }}
      >
        <label>
          Note{' '}
          <input
            data-testid="note-text"
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="note-add" disabled={add.isPending}>
          Add note
        </button>
        {(invalid || add.isError) && (
          <span data-testid="note-error" role="alert">
            {' '}
            {invalid ? 'Enter a note.' : 'Could not add this note.'}
          </span>
        )}
      </form>
      <ul data-testid="note-list">
        {notes.map((n, i) => (
          <li key={i} data-testid="note-line">
            {`${n.date} ${n.user}: ${n.text}`}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 9, Component: NotesPanel });
