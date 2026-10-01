import { useState } from 'react';
import { useCreateInvoice } from './invoices';

// Raise-an-invoice form. useState holds only what the user is currently typing (the amount) and the
// validation error for the current attempt. An invoice must bill a positive amount — a zero or
// negative value is rejected with a visible error and no row is added (the server + DB enforce the
// same rule). On a valid submit it POSTs {projectId, amount} and invalidates ['invoices', projectId];
// the list and total refresh from the server — nothing is hand-maintained here.
export function InvoiceForm({ projectId }: { projectId: number }) {
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const create = useCreateInvoice(projectId);

  return (
    <form
      data-testid="invoice-form"
      onSubmit={(event) => {
        event.preventDefault();
        const value = Number(amount);
        if (!amount.trim() || Number.isNaN(value) || value <= 0) {
          setError('Amount must be more than zero');
          return;
        }
        setError('');
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
        onChange={(event) => {
          setAmount(event.target.value);
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
  );
}
