import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { type Note, notesKey, useNotes } from './useNotes';

// The sheet's notes, newest first, and the form that adds one (a blank note is refused). Each shows the
// sheet's today when it was added, who added it, and its text.
export function NotePanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const notes = useNotes(sheet);
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<Note[]>(`/api/sheets/${encodeURIComponent(sheet)}/notes`, { ...body, user }),
    onSuccess: (saved) => {
      setText('');
      queryClient.setQueryData(notesKey(sheet), saved);
    },
  });
  return (
    <>
      <ul data-testid="note-list">
        {notes.map((n, i) => (
          <li key={i} data-testid="note-line">{`${n.on} ${n.user}: ${n.text}`}</li>
        ))}
      </ul>
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
        <input
          data-testid="note-text"
          value={text}
          placeholder="Note"
          onChange={(e) => {
            setText(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="note-add">Add note</button>
        {add.isError && <span data-testid="note-error" role="alert"> Could not add the note: it needs some text.</span>}
      </form>
    </>
  );
}
