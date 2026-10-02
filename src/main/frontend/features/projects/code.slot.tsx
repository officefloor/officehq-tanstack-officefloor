import { ProjectRow } from '../../slots/defs/projectRow';
import type { Project } from './projects';

// "What is this job's reference code" — a self-contained cell filling the project-row region,
// showing the short code set when the job was created (Flyway V29). The projects table is not
// edited to add it (CLAUDE.md rule 3); it fills the slot the table renders per row, reading the
// code served alongside each project. Ordered first so the code reads before the status and actions.
function ProjectCode({ project }: { project: Project }) {
  return <span data-testid="project-code">{project.code ?? ''}</span>;
}

export const contribution = ProjectRow.fill({ order: 1, Component: ProjectCode });
