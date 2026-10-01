import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ProjectDetail } from '../../slots/defs/projectDetail';
import {
  getProjectBudget,
  projectBudgetKey,
  setProjectBudget,
} from './budgetApi';

// Money shown with thousands grouping, like "$1,000.00". The shared formatMoney gives two places but
// no grouping; a budget figure is large enough to want the commas, so this panel formats its own.
function formatBudget(amount: number): string {
  return `$${Number(amount).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

// A project's budget — one panel filling the project.detail region: the budget set against the
// project, how much has been invoiced against it, and what is left, above a form to set (or change)
// the budget. Server data is read under ['projects', projectId, 'budget'] (never copied into state):
// the panel queries for itself and the server derives the invoiced / remaining figures. What the
// user is typing into the set-budget field lives in useState (uncommitted input); on a successful
// write we invalidate the same key so the figures refetch — no hand-maintained totals.
function ProjectBudgetPanel({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const [amount, setAmount] = useState('');
  const { data: budget } = useQuery({
    queryKey: projectBudgetKey(projectId),
    queryFn: () => getProjectBudget(projectId),
  });
  const mutation = useMutation({
    mutationFn: setProjectBudget,
    onSuccess: () => {
      setAmount('');
      void queryClient.invalidateQueries({ queryKey: projectBudgetKey(projectId) });
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const value = Number(amount);
    if (amount.trim() === '' || Number.isNaN(value) || value < 0) {
      return;
    }
    mutation.mutate({ id: projectId, budget: value });
  };

  if (!budget) {
    return null;
  }

  return (
    <section data-testid="project-budget-panel">
      <dl>
        <dt>Budget</dt>
        <dd data-testid="project-budget">{formatBudget(budget.budget)}</dd>
        <dt>Invoiced</dt>
        <dd data-testid="project-invoiced">{formatBudget(budget.invoiced)}</dd>
        <dt>Remaining</dt>
        <dd data-testid="project-remaining">{formatBudget(budget.remaining)}</dd>
      </dl>
      <form onSubmit={submit}>
        <input
          data-testid="project-budget-input"
          type="number"
          min="0"
          step="0.01"
          placeholder="Set budget"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
        />
        <button
          data-testid="project-budget-submit"
          type="submit"
          disabled={mutation.isPending}
        >
          Set budget
        </button>
      </form>
    </section>
  );
}

export const contribution = ProjectDetail.fill({ order: 15, Component: ProjectBudgetPanel });
