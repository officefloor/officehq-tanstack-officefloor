import { formatMoney } from '../../ui/money';
import { useProjectBudget } from './budget';

// A project's budget standing: the agreed budget, how much has been invoiced against it, and what is
// left over (budget minus invoiced). Reads its own query key, scoped to the project; the figures are
// derived on the server. A project with no budget set shows nothing here.
export function ProjectBudget({ projectId }: { projectId: number }) {
  const { data } = useProjectBudget(projectId);

  if (!data || data.budget === null || data.remaining === null) {
    return null;
  }

  return (
    <section data-testid="project-budget-panel">
      <h2>Budget</h2>
      <dl>
        <dt>Budget</dt>
        <dd data-testid="project-budget">{formatMoney(data.budget)}</dd>
        <dt>Invoiced</dt>
        <dd data-testid="project-invoiced">{formatMoney(data.invoiced)}</dd>
        <dt>Remaining</dt>
        <dd data-testid="project-remaining">{formatMoney(data.remaining)}</dd>
      </dl>
    </section>
  );
}
