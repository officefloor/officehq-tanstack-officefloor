import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { ProjectRowActions } from '../../slots/defs/projectRowActions';
import type { Project } from './ProjectsPage';

// The button that deletes a project the user no longer needs — its own file, filling the
// project-row-actions slot. Deleting is a useMutation that POSTs the id to /api/projects/remove;
// on success it invalidates ['projects'] so the list (and anything else showing projects) refetches
// and the row drops out — the list is never edited to know about this action. Carries
// data-testid="project-delete-<id>" (the test contract).
export const contribution = ProjectRowActions.fill({
  order: 20,
  Component: ({ projectId }) => {
    const queryClient = useQueryClient();
    const remove = useMutation({
      mutationFn: () => postJson<Project>('/api/projects/remove', { id: projectId }),
      onSuccess: () => {
        void queryClient.invalidateQueries({ queryKey: ['projects'] });
      },
    });
    return (
      <button
        type="button"
        data-testid={`project-delete-${projectId}`}
        disabled={remove.isPending}
        onClick={() => remove.mutate()}
      >
        Delete
      </button>
    );
  },
});
