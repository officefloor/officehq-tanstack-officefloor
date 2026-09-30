import { Link } from '@tanstack/react-router';
import { ProjectRow } from '../../slots/defs/projectRow';

// The "open this project" action on every project row — its own file (CLAUDE.md rule 3). Drilling
// in is a child route (rule 2): the link navigates to /projects/$projectId, where the project's
// invoices live. Carries data-testid="project-open-<id>" (the test contract).
export const contribution = ProjectRow.fill({
  order: 0,
  Component: ({ projectId }: { projectId: number }) => (
    <Link
      to="/projects/$projectId"
      params={{ projectId: String(projectId) }}
      data-testid={`project-open-${projectId}`}
    >
      Open
    </Link>
  ),
});
