import { useQuery } from '@tanstack/react-query';
import { ProjectRow } from '../../slots/defs/projectRow';
import { projectsKey, listProjects, type Project } from './api';

// A project's lifecycle status, shown on its row — one new file filling the per-project-row slot.
// It reads the same ['projects'] query the list does (never a second fetch of its own data, never a
// prop passed down): it finds its row in that shared cache by id and renders the status. Ordered
// first so it reads before the row's actions. Its testid is project-status (the test contract).
function ProjectStatusCell({ projectId }: { projectId: number }) {
  const { data: projects } = useQuery({ queryKey: projectsKey, queryFn: listProjects });
  const project = projects?.find((p: Project) => p.id === projectId);

  if (!project) {
    return null;
  }

  return <span data-testid="project-status">{project.status}</span>;
}

export const contribution = ProjectRow.fill({ order: -10, Component: ProjectStatusCell });
