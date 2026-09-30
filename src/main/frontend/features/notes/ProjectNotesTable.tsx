import { useQuery } from '@tanstack/react-query';
import { projectNotesKey, fetchProjectNotes, type Note } from './queries';

// The notes written on one project. Queries for itself under ['projects', projectId, 'notes'] —
// never handed its data by a parent (CLAUDE.md rule 5). The server returns them newest first, so
// the rows render in that order untouched. Carries the stable data-testid contract: a row per note
// and the text cell the test reads.
export function ProjectNotesTable({ projectId }: { projectId: number }) {
  const { data: notes } = useQuery({
    queryKey: projectNotesKey(projectId),
    queryFn: () => fetchProjectNotes(projectId),
  });

  if (!notes) {
    return null;
  }

  return (
    <ul data-testid="project-notes-list">
      {notes.map((note: Note) => (
        <li key={note.id} data-testid={`note-row-${note.id}`}>
          <span data-testid="note-text">{note.text}</span>
          <time data-testid="note-at" dateTime={note.at}>
            {note.at}
          </time>
        </li>
      ))}
    </ul>
  );
}
