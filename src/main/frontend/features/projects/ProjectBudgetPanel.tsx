import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A project's budget summary as the server returns it: the budget set on it, how much has been
// invoiced against it (the sum of its invoices), and what is left (budget minus invoiced).
type Budget = {
  budget: number;
  invoiced: number;
  remaining: number;
};

// Money rendered with thousands separators, like $1,000.00 — the budget/invoiced/remaining figures
// can run large, so they read with grouping (the test contract for this panel).
const dollars = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

// A project's budget, rendered in the project-detail context: what is planned, what has been
// invoiced against it, and what is left — plus a form to set the budget. Server data is read with
// useQuery under a key that starts with ['invoices'] (the invoiced figure aggregates the project's
// invoices, like the client statement does), so an invoice write that invalidates ['invoices']
// refreshes this too — never copied into state, never hand-maintained (rule 5). Setting the budget
// is a useMutation that invalidates ['invoices'] so the panel refetches with the new figures. The
// only useState is the amount the user is currently typing (rule 4).
export function ProjectBudgetPanel({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: ['invoices', 'projectBudget', projectId],
    queryFn: () => getJson<Budget>(`/api/projects/budget?projectId=${projectId}`),
  });

  const [budget, setBudget] = useState('');
  const [error, setError] = useState('');

  const save = useMutation({
    mutationFn: () =>
      postJson<Budget>('/api/projects/budget', { projectId, budget: Number(budget) }),
    onSuccess: () => {
      setBudget('');
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
  });

  const data = query.data;

  return (
    <section data-testid="project-budget-panel">
      <form
        data-testid="project-budget-form"
        onSubmit={(e) => {
          e.preventDefault();
          const value = Number(budget);
          if (!budget.trim() || Number.isNaN(value) || value < 0) {
            setError('Budget must be zero or more.');
            return;
          }
          setError('');
          save.mutate();
        }}
      >
        <input
          data-testid="project-budget-input"
          type="number"
          step="0.01"
          placeholder="Budget"
          value={budget}
          onChange={(e) => {
            setBudget(e.target.value);
            setError('');
          }}
        />
        <button data-testid="project-budget-submit" type="submit">
          Set budget
        </button>
        {error && (
          <p data-testid="project-budget-error" role="alert">
            {error}
          </p>
        )}
      </form>

      <dl>
        <dt>Budget</dt>
        <dd data-testid="project-budget">{data ? dollars.format(Number(data.budget)) : ''}</dd>
        <dt>Invoiced</dt>
        <dd data-testid="project-invoiced">
          {data ? dollars.format(Number(data.invoiced)) : ''}
        </dd>
        <dt>Remaining</dt>
        <dd data-testid="project-remaining">
          {data ? dollars.format(Number(data.remaining)) : ''}
        </dd>
      </dl>
    </section>
  );
}
