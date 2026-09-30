import { getJson } from '../../api/http';

// A project's budget picture, under a key nested beneath ['projects'] so invalidating projects (or a
// project's invoices) refreshes it too (CLAUDE.md rule 5). The budget, how much has been invoiced
// against it (the sum of the project's invoices) and what is left (budget - invoiced) are all
// derived server-side, so the three figures always agree. budget/remaining are null if none is set.
export type ProjectBudget = {
  budget: number | null;
  invoiced: number;
  remaining: number | null;
};

export const projectBudgetKey = (projectId: number) =>
  ['projects', projectId, 'budget'] as const;

export function fetchProjectBudget(projectId: number): Promise<ProjectBudget> {
  return getJson<ProjectBudget>(`/api/projects/${projectId}/budget`);
}
