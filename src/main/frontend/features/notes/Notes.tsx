import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { addNote, listNotes, notesKey } from './api';

/** The sheet's notes, newest first, as `<sheet's today when added> <user>: <text>`, and a form to add
 *  one (trimmed; a blank one refused). */
export function Notes({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const notes = useQuery({ queryKey: notesKey(sheetId), queryFn: () => listNotes(sheetId) });
  const [text, setText] = useState('');

  const save = useMutation({
    mutationFn: (input: string) => addNote(sheetId, user, input),
    onSuccess: () => {
      setText('');
      return queryClient.invalidateQueries({ queryKey: notesKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();

  return (
    <section>
      <h3>Notes</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = text;
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(input);
          });
        }}
      >
        <input data-testid="note-text" placeholder="Note" value={text}
          onChange={(e) => {
            setText(e.target.value);
            save.reset();
          }} />{' '}
        <button type="submit" data-testid="note-add" disabled={save.isPending}>
          Add note
        </button>
        {save.isError && <span data-testid="note-error">The note was refused: it needs text.</span>}
      </form>
      <ul>
        {(notes.data ?? []).map((n) => (
          <li key={n.id} data-testid="note-line">{`${n.addedOn} ${n.by}: ${n.text}`}</li>
        ))}
      </ul>
    </section>
  );
}
