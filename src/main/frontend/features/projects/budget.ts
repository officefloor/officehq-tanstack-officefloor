import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// A project's budget standing, read from GET /api/projects/budget: the agreed budget, how much has
// been invoiced against it, and what is left (budget minus invoiced). Derived on the server so the
// money math lives in one place. Its own query key, scoped to the one project; budget and remaining
// are null when the project has no budget set.
export type ProjectBudget = {
  projectId: number;
  budget: number | null;
  invoiced: number;
  remaining: number | null;
};

export function useProjectBudget(projectId: number) {
  return useQuery({
    queryKey: ['projects', projectId, 'budget'] as const,
    queryFn: () => getJson<ProjectBudget>(`/api/projects/budget?projectId=${projectId}`),
  });
}
