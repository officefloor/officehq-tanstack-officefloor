import { ProjectDetail } from '../../slots/defs/projectDetail';
import { ProjectNotesTable } from './ProjectNotesTable';
import { NoteForm } from './NoteForm';

// The notes panel on a project's detail page — one new *.slot.tsx file filling the ProjectDetail
// region (CLAUDE.md rule 3). Nothing existing is edited to add it. Shows the project's notes newest
// first and the form to add one; each queries/mutates for itself under the shared notes key.
function ProjectNotesPanel({ projectId }: { projectId: number }) {
  return (
    <section data-testid="project-notes">
      <NoteForm projectId={projectId} />
      <ProjectNotesTable projectId={projectId} />
    </section>
  );
}

export const contribution = ProjectDetail.fill({ order: 20, Component: ProjectNotesPanel });
