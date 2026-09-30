import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { ProjectRowActions } from '../../slots/defs/projectRowActions';
import type { Project } from './ProjectsPage';

// The button that archives a project — tucks it away instead of deleting it — its own file, filling
// the project-row-actions slot. Archiving is a useMutation that POSTs the id to
// /api/projects/archive; on success it invalidates ['projects'] so the list (and the client's
// projects panel, which shares the key) refetches and the row drops out, while the row is kept on
// the server. Carries data-testid="project-archive-<id>" (the test contract).
export const contribution = ProjectRowActions.fill({
  order: 15,
  Component: ({ projectId }) => {
    const queryClient = useQueryClient();
    const archive = useMutation({
      mutationFn: () => postJson<Project>('/api/projects/archive', { id: projectId }),
      onSuccess: () => {
        void queryClient.invalidateQueries({ queryKey: ['projects'] });
      },
    });
    return (
      <button
        type="button"
        data-testid={`project-archive-${projectId}`}
        disabled={archive.isPending}
        onClick={() => archive.mutate()}
      >
        Archive
      </button>
    );
  },
});
