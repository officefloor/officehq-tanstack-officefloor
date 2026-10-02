import { ProjectRow } from '../../slots/defs/projectRow';
import type { Project } from './projects';

// "What state is this project in" — a self-contained cell filling the project-row region, showing
// whether the project is ACTIVE, ON_HOLD or FINISHED. The projects table is not edited to add it
// (CLAUDE.md rule 3); it fills the slot the table renders per row, reading the status served
// alongside each project. Ordered first so the status reads before the row's actions.
function ProjectStatus({ project }: { project: Project }) {
  return <span data-testid="project-status">{project.status}</span>;
}

export const contribution = ProjectRow.fill({ order: 5, Component: ProjectStatus });
