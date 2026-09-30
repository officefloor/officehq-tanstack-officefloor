import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ProjectRow } from '../../slots/defs/projectRow';
import { projectsKey, deleteProject } from './queries';

// The "delete this project" action on every project row — its own file (CLAUDE.md rule 3), so the
// projects table never lists what goes at the end of a row. The write is a mutation that
// invalidates ['projects'] (rule 5): every view showing projects re-queries itself, so the deleted
// row disappears from the list for free. The server records the audited PROJECT_DELETED entry.
// Carries data-testid="project-delete-<id>" (the test contract).
function ProjectDelete({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();

  const remove = useMutation({
    mutationFn: () => deleteProject(projectId),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: projectsKey }),
  });

  return (
    <button
      type="button"
      data-testid={`project-delete-${projectId}`}
      onClick={() => remove.mutate()}
    >
      Delete
    </button>
  );
}

export const contribution = ProjectRow.fill({ order: 10, Component: ProjectDelete });
