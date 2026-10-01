import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ProjectDetail } from '../../slots/defs/projectDetail';
import { createInvoice, invoicesKey } from './api';

// Add an invoice for an amount to this project — one panel filling the project.detail region. The
// amount the user is typing lives in useState (uncommitted input); the saved data lives on the
// server. On success we invalidate ['invoices', projectId] so the list + total refetch — we never
// hand-maintain the list.
function InvoiceForm({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const [amount, setAmount] = useState('');

  const mutation = useMutation({
    mutationFn: createInvoice,
    onSuccess: () => {
      setAmount('');
      void queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) });
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const value = Number(amount);
    if (amount.trim() === '' || Number.isNaN(value)) {
      return;
    }
    mutation.mutate({ projectId, amount: value });
  };

  return (
    <form onSubmit={submit}>
      <input
        data-testid="invoice-form-amount"
        placeholder="Amount"
        value={amount}
        onChange={(e) => setAmount(e.target.value)}
      />
      <button data-testid="invoice-form-submit" type="submit">
        Add invoice
      </button>
    </form>
  );
}

export const contribution = ProjectDetail.fill({ order: 10, Component: InvoiceForm });
