import { useQuery } from '@tanstack/react-query';
import { ProjectDetail } from '../../slots/defs/projectDetail';
import { formatCurrency } from '../../ui/currency';
import { projectBudgetKey, fetchProjectBudget } from './queries';

// The budget panel on a project's detail page — one new *.slot.tsx file filling the ProjectDetail
// region (CLAUDE.md rule 3). Nothing existing is edited to add it. Queries for itself under
// ['projects', projectId, 'budget'] (rule 5); the budget, the invoiced total and what is left are
// derived server-side so the three figures always agree. Renders nothing until a budget is set.
function ProjectBudgetPanel({ projectId }: { projectId: number }) {
  const { data: budget } = useQuery({
    queryKey: projectBudgetKey(projectId),
    queryFn: () => fetchProjectBudget(projectId),
  });

  if (!budget || budget.budget === null || budget.remaining === null) {
    return null;
  }

  return (
    <section data-testid="project-budget-panel">
      <dl>
        <dt>Budget</dt>
        <dd data-testid="project-budget">{formatCurrency(budget.budget)}</dd>
        <dt>Invoiced</dt>
        <dd data-testid="project-invoiced">{formatCurrency(budget.invoiced)}</dd>
        <dt>Remaining</dt>
        <dd data-testid="project-remaining">{formatCurrency(budget.remaining)}</dd>
      </dl>
    </section>
  );
}

export const contribution = ProjectDetail.fill({ order: 5, Component: ProjectBudgetPanel });
