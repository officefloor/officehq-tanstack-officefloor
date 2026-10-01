import { getJson, postJson } from '../../api/http';

// A project's budget as the API exposes it: the planned spend set against it, how much has been
// invoiced against it (the sum of the project's issued invoices), and what is left (budget minus
// invoiced). The derivation lives on the server. The query key is scoped to the project and shares
// the ['projects'] prefix — ['projects', projectId, 'budget'] — so each project's budget reads only
// its own figures while a project write still refreshes it by prefix.
export type ProjectBudget = {
  projectId: number;
  budget: number;
  invoiced: number;
  remaining: number;
};

export type SetBudget = { id: number; budget: number };

export const projectBudgetKey = (projectId: number) =>
  ['projects', projectId, 'budget'] as const;

export const getProjectBudget = (projectId: number): Promise<ProjectBudget> =>
  getJson<ProjectBudget>(`/api/projects/budget?projectId=${projectId}`);

export const setProjectBudget = (body: SetBudget): Promise<ProjectBudget> =>
  postJson<ProjectBudget>('/api/projects/budget', body);
