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
  const [error, setError] = useState('');

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
    // An invoice must be for something: the amount has to be more than zero.
    if (value <= 0) {
      setError('Amount must be greater than zero');
      return;
    }
    setError('');
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
      {error && <p data-testid="invoice-form-amount-error">{error}</p>}
    </form>
  );
}

export const contribution = ProjectDetail.fill({ order: 10, Component: InvoiceForm });
