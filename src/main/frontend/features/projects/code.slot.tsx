import { useQuery } from '@tanstack/react-query';
import { ProjectRow } from '../../slots/defs/projectRow';
import { projectsKey, listProjects, type Project } from './api';

// A project's reference code, shown on its row — one new file filling the per-project-row slot. Like
// the status cell, it reads the same ['projects'] query the list does (never a second fetch, never a
// prop passed down): it finds its row in that shared cache by id and renders the code. Ordered ahead
// of the row's actions. Its testid is project-code (the test contract).
function ProjectCodeCell({ projectId }: { projectId: number }) {
  const { data: projects } = useQuery({ queryKey: projectsKey, queryFn: listProjects });
  const project = projects?.find((p: Project) => p.id === projectId);

  if (!project) {
    return null;
  }

  return <span data-testid="project-code">{project.code}</span>;
}

export const contribution = ProjectRow.fill({ order: -5, Component: ProjectCodeCell });
