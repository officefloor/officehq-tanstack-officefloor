import { ProjectDetail } from '../../slots/defs/projectDetail';
import { ProjectTasksTable } from './ProjectTasksTable';

// The task-checklist panel on a project's detail page — one new *.slot.tsx file filling the
// ProjectDetail region (CLAUDE.md rule 3). Nothing existing is edited to add it. Shows the project's
// tasks and lets each be ticked off; the table queries/mutates for itself under the shared key.
function ProjectTasksPanel({ projectId }: { projectId: number }) {
  return (
    <section data-testid="project-tasks">
      <ProjectTasksTable projectId={projectId} />
    </section>
  );
}

export const contribution = ProjectDetail.fill({ order: 10, Component: ProjectTasksPanel });
