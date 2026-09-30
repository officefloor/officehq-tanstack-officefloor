import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { invoicesKey, createInvoice } from './queries';

// Add an invoice for a project. useState holds only what the user is currently entering (CLAUDE.md
// rule 4); the write is a mutation that invalidates ['invoices', projectId] so the list and its
// total refresh themselves.
export function InvoiceForm({ projectId }: { projectId: number }) {
  const [amount, setAmount] = useState('');
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: createInvoice,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) });
      setAmount('');
    },
  });

  return (
    <form
      data-testid="invoice-form"
      onSubmit={(event) => {
        event.preventDefault();
        const value = Number(amount);
        if (amount.trim() === '' || Number.isNaN(value)) {
          return;
        }
        mutation.mutate({ projectId, amount: value });
      }}
    >
      <input
        data-testid="invoice-form-amount"
        placeholder="Amount"
        value={amount}
        onChange={(event) => setAmount(event.target.value)}
      />
      <button data-testid="invoice-form-submit" type="submit">
        Add invoice
      </button>
    </form>
  );
}
