import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ProjectDetail } from '../../slots/defs/projectDetail';
import { createNote, listNotes, notesKey, type Note } from './notesApi';

// Notes the user writes on a project — one panel filling the project.detail region: the list of
// notes (newest first) above a form to add one. Reads server data under ['notes', 'project', id]
// (never copied into state); the server returns the rows already newest-first, so the list renders
// them in order. What the user is currently typing lives in useState (uncommitted input); on a
// successful write we invalidate the same key so the list refetches with the new note on top — no
// hand-maintained list, no import between list and form.
function ProjectNotes({ projectId }: { projectId: number }) {
  const targetType = 'project';
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const { data: notes } = useQuery({
    queryKey: notesKey(targetType, projectId),
    queryFn: () => listNotes(targetType, projectId),
  });
  const mutation = useMutation({
    mutationFn: createNote,
    onSuccess: () => {
      setText('');
      void queryClient.invalidateQueries({ queryKey: notesKey(targetType, projectId) });
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (text.trim() === '') {
      return;
    }
    mutation.mutate({ targetType, targetId: projectId, text: text.trim() });
  };

  return (
    <section data-testid="project-notes">
      <ul data-testid="project-notes-list">
        {(notes ?? []).map((note: Note) => (
          <li key={note.id} data-testid={`note-row-${note.id}`}>
            <span data-testid="note-text">{note.text}</span>
          </li>
        ))}
      </ul>
      <form onSubmit={submit}>
        <input
          data-testid="note-form-text"
          placeholder="Write a note"
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <button data-testid="note-form-submit" type="submit" disabled={mutation.isPending}>
          Add note
        </button>
      </form>
    </section>
  );
}

export const contribution = ProjectDetail.fill({ order: 50, Component: ProjectNotes });
