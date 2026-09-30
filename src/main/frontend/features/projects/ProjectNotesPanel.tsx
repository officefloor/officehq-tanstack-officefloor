import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A note as the server returns it: free text kept against a target (a project here), plus the ISO
// instant it was written. The server returns notes newest first.
export type Note = {
  id: number;
  targetType: string;
  targetId: number;
  text: string;
  at: string;
};

// A project's notes, rendered in the project-detail context: the list of that project's notes,
// newest first, plus a form to add one. Server data is read with useQuery under the ['notes'] key
// and filtered to this project (never copied into state, never hand-maintained — rule 5). Adding is a
// useMutation that invalidates ['notes'], so the list refetches itself with the new note on top. The
// only useState here is the text the user is currently typing (rule 4).
export function ProjectNotesPanel({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const notes = useQuery({
    queryKey: ['notes'],
    queryFn: () => getJson<Note[]>('/api/notes'),
  });

  const [text, setText] = useState('');

  const create = useMutation({
    mutationFn: () =>
      postJson<Note>('/api/notes', { targetType: 'project', targetId: projectId, text }),
    onSuccess: () => {
      setText('');
      void queryClient.invalidateQueries({ queryKey: ['notes'] });
    },
  });

  // Server returns notes newest first; keep that order and narrow to this project.
  const rows = (notes.data ?? []).filter(
    (n) => n.targetType === 'project' && n.targetId === projectId,
  );

  return (
    <section data-testid="project-notes">
      <form
        data-testid="note-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!text.trim()) {
            return;
          }
          create.mutate();
        }}
      >
        <input
          data-testid="note-form-text"
          placeholder="Write a note"
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <button data-testid="note-form-submit" type="submit">
          Add note
        </button>
      </form>

      {rows.length === 0 ? (
        <p data-testid="project-notes-empty">No notes yet.</p>
      ) : (
        <ul data-testid="project-notes-list">
          {rows.map((n) => (
            <li key={n.id} data-testid={`note-row-${n.id}`}>
              <span data-testid="note-text">{n.text}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
