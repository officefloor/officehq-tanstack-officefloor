import { useQuery } from '@tanstack/react-query';
import { ProjectRow } from '../../slots/defs/projectRow';
import { projectsKey, fetchProjects } from './queries';

// Shows a project's lifecycle status on its row — its own file (CLAUDE.md rule 3), so the projects
// table never lists what each row carries. Queries the shared ['projects'] key for itself (rule 5)
// and reads the status off the matching project. Carries data-testid="project-status" (the test
// contract); ordered first so the status reads before the row's actions.
function ProjectStatus({ projectId }: { projectId: number }) {
  const { data: projects } = useQuery({ queryKey: projectsKey, queryFn: fetchProjects });
  const project = projects?.find((p) => p.id === projectId);
  if (!project) {
    return null;
  }
  return <span data-testid="project-status">{project.status}</span>;
}

export const contribution = ProjectRow.fill({ order: 1, Component: ProjectStatus });
