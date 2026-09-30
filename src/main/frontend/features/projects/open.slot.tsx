import { Link } from '@tanstack/react-router';
import { ProjectRowActions } from '../../slots/defs/projectRowActions';

// The link that opens a project's detail page — its own file, filling the project-row-actions slot.
// Drilling in is a child route (/projects/$projectId), reached by this Link; the list is not edited
// to know about it. Carries data-testid="project-open-<id>" (the test contract).
export const contribution = ProjectRowActions.fill({
  order: 10,
  Component: ({ projectId }) => (
    <Link
      to="/projects/$projectId"
      params={{ projectId: String(projectId) }}
      data-testid={`project-open-${projectId}`}
    >
      Open
    </Link>
  ),
});
