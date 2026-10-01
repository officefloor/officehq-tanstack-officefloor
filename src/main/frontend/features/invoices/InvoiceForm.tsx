import { useState } from 'react';
import { useCreateInvoice } from './invoices';

// Raise-an-invoice form. useState holds only what the user is currently typing (the amount). On
// submit it POSTs {projectId, amount} and invalidates ['invoices', projectId]; the list and total
// refresh from the server — nothing is hand-maintained here.
export function InvoiceForm({ projectId }: { projectId: number }) {
  const [amount, setAmount] = useState('');
  const create = useCreateInvoice(projectId);

  return (
    <form
      data-testid="invoice-form"
      onSubmit={(event) => {
        event.preventDefault();
        const value = Number(amount);
        if (!amount.trim() || Number.isNaN(value) || value <= 0) {
          return;
        }
        create.mutate(
          { amount: value },
          {
            onSuccess: () => setAmount(''),
          },
        );
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
