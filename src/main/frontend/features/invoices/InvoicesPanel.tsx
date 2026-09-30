import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// An invoice as the server returns it: which project it belongs to and its amount.
export type Invoice = { id: number; projectId: number; amount: number };

// Amounts always render with 2 decimals (the test contract). Kept in one place so the rows and the
// derived total format identically.
function money(amount: number): string {
  return amount.toFixed(2);
}

// A project's invoices: the list scoped to this project, their derived total, and the form to add a
// new one. Server data is read with useQuery under the ['invoices'] key and changed with
// useMutation + invalidateQueries — never copied into state, never hand-maintained. The only
// useState is the amount the user is currently typing (rule 4).
export function InvoicesPanel({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const invoices = useQuery({
    queryKey: ['invoices'],
    queryFn: () => getJson<Invoice[]>('/api/invoices'),
  });

  const [amount, setAmount] = useState('');

  const create = useMutation({
    mutationFn: () =>
      postJson<Invoice>('/api/invoices', { projectId, amount: Number(amount) }),
    onSuccess: () => {
      setAmount('');
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
  });

  const rows = (invoices.data ?? []).filter((i) => i.projectId === projectId);
  const total = rows.reduce((sum, i) => sum + Number(i.amount), 0);

  return (
    <section data-testid="project-invoices">
      <form
        data-testid="invoice-form"
        onSubmit={(e) => {
          e.preventDefault();
          const value = Number(amount);
          if (!amount.trim() || Number.isNaN(value) || value <= 0) {
            return;
          }
          create.mutate();
        }}
      >
        <input
          data-testid="invoice-form-amount"
          type="number"
          step="0.01"
          placeholder="Amount"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
        />
        <button data-testid="invoice-form-submit" type="submit">
          Add invoice
        </button>
      </form>

      <table data-testid="project-invoices-table">
        <thead>
          <tr>
            <th>Amount</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((i) => (
            <tr key={i.id} data-testid={`invoice-row-${i.id}`}>
              <td data-testid="invoice-amount">{money(Number(i.amount))}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td data-testid="project-invoices-total">{money(total)}</td>
          </tr>
        </tfoot>
      </table>
    </section>
  );
}
