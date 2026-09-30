import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { money } from '../../ui/money';

// An invoice as the server returns it: which project it belongs to, its amount, and its payment
// status (UNPAID until marked paid).
export type Invoice = { id: number; projectId: number; amount: number; status: string };

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
  const [error, setError] = useState('');

  const create = useMutation({
    mutationFn: () =>
      postJson<Invoice>('/api/invoices', { projectId, amount: Number(amount) }),
    onSuccess: () => {
      setAmount('');
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
  });

  // Marking an invoice paid is a write: POST then invalidate ['invoices'] so the row re-renders with
  // its new status — never hand-maintained. The server also records the payment to the audit file.
  const pay = useMutation({
    mutationFn: (id: number) => postJson<Invoice>('/api/invoices/pay', { id }),
    onSuccess: () => {
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
            setError('Amount must be more than zero.');
            return;
          }
          setError('');
          create.mutate();
        }}
      >
        <input
          data-testid="invoice-form-amount"
          type="number"
          step="0.01"
          placeholder="Amount"
          value={amount}
          onChange={(e) => {
            setAmount(e.target.value);
            setError('');
          }}
        />
        <button data-testid="invoice-form-submit" type="submit">
          Add invoice
        </button>
        {error && (
          <p data-testid="invoice-form-amount-error" role="alert">
            {error}
          </p>
        )}
      </form>

      <table data-testid="project-invoices-table">
        <thead>
          <tr>
            <th>Amount</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {rows.map((i) => (
            <tr key={i.id} data-testid={`invoice-row-${i.id}`}>
              <td data-testid="invoice-amount">{money(Number(i.amount))}</td>
              <td data-testid="invoice-status">{i.status}</td>
              <td>
                {i.status !== 'PAID' && (
                  <button
                    type="button"
                    data-testid={`invoice-pay-${i.id}`}
                    onClick={() => pay.mutate(i.id)}
                  >
                    Mark paid
                  </button>
                )}
              </td>
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
