import { Link } from '@tanstack/react-router';
import { ProjectRow } from '../../slots/defs/projectRow';

// The link that opens a project's detail page — one new file filling the projects-row action slot.
// Drilling in is a child route (routes/projects.$projectId.tsx), never a flag, so this is a plain
// navigation to that route. Its testid is project-open-<id> (the test contract; CLAUDE.md).
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
