import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { ProjectRow } from '../../slots/defs/projectRow';
import { projectsKey } from './api';

// Delete a project — one new file filling the per-project-row action slot. The click is a
// useMutation (never a hand-rolled list edit in state); on success we invalidate the shared
// ['projects'] key so the list refetches and the row disappears. The server performs the delete
// and appends the audit record, so "keep a record" is handled where the side-effect lives.
// Its testid is project-delete-<id> (the test contract; CLAUDE.md).
function DeleteProject({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => postJson<unknown>('/api/projects/delete', { id: projectId }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: projectsKey });
    },
  });

  return (
    <button
      data-testid={`project-delete-${projectId}`}
      type="button"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      Delete
    </button>
  );
}

export const contribution = ProjectRow.fill({ order: 10, Component: DeleteProject });
