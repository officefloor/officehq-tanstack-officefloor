import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ProjectRow } from '../../slots/defs/projectRow';
import { projectsKey, archiveProject } from './queries';

// The "archive this project" action on every project row — its own file (CLAUDE.md rule 3), so the
// projects table never lists what goes at the end of a row. Archiving tucks the project away instead
// of deleting it: the write is a mutation that invalidates ['projects'] (the main list) and
// ['clients'] (each client's own project list) (rule 5), so the row drops off both for free while
// the project is retained. The server records the audited PROJECT_ARCHIVED entry. Carries
// data-testid="project-archive-<id>" (the test contract).
function ProjectArchive({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();

  const archive = useMutation({
    mutationFn: () => archiveProject(projectId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: projectsKey });
      void queryClient.invalidateQueries({ queryKey: ['clients'] });
    },
  });

  return (
    <button
      type="button"
      data-testid={`project-archive-${projectId}`}
      onClick={() => archive.mutate()}
    >
      Archive
    </button>
  );
}

export const contribution = ProjectRow.fill({ order: 5, Component: ProjectArchive });
