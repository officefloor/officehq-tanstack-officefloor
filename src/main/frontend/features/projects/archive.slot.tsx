import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ProjectRow } from '../../slots/defs/projectRow';
import { archiveProject, projectsKey } from './api';

// Archive a project — one new file filling the per-project-row action slot. Rather than delete, the
// click tucks the project away: a useMutation (never a hand-rolled list edit in state) that, on
// success, invalidates the shared ['projects'] key so the list refetches and the archived row drops
// off. The server flips the flag and appends the audit record, so "note it when I do" is handled
// where the side-effect lives. Its testid is project-archive-<id> (the test contract; CLAUDE.md).
function ArchiveProject({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => archiveProject(projectId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: projectsKey });
    },
  });

  return (
    <button
      data-testid={`project-archive-${projectId}`}
      type="button"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      Archive
    </button>
  );
}

export const contribution = ProjectRow.fill({ order: 20, Component: ArchiveProject });
