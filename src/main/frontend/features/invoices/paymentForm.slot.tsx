import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { createPayment, paymentsKey } from './paymentsApi';

// Record a payment a client has made against this invoice — one panel filling the invoice.detail
// region. What the user is currently typing lives in useState (uncommitted input); the saved payments
// live on the server. On success we invalidate ['invoice-payments', invoiceId] so the list refetches —
// we never hand-maintain it.
function PaymentForm({ invoiceId }: { invoiceId: number }) {
  const queryClient = useQueryClient();
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: createPayment,
    onSuccess: () => {
      setAmount('');
      setDate('');
      void queryClient.invalidateQueries({ queryKey: paymentsKey(invoiceId) });
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const amountValue = Number(amount);
    // A payment has to be for a positive amount on a day it was paid.
    if (amount.trim() === '' || Number.isNaN(amountValue) || amountValue <= 0) {
      setError('Amount must be greater than zero');
      return;
    }
    if (date.trim() === '') {
      setError('A payment date is required');
      return;
    }
    setError('');
    mutation.mutate({ invoiceId, amount: amountValue, date: date.trim() });
  };

  return (
    <form onSubmit={submit}>
      <input
        data-testid="payment-form-amount"
        placeholder="Amount"
        value={amount}
        onChange={(e) => setAmount(e.target.value)}
      />
      <input
        data-testid="payment-form-date"
        placeholder="Date"
        value={date}
        onChange={(e) => setDate(e.target.value)}
      />
      <button data-testid="payment-form-submit" type="submit">
        Record payment
      </button>
      {error && <p data-testid="payment-form-error">{error}</p>}
    </form>
  );
}

export const contribution = InvoiceDetail.fill({ order: 40, Component: PaymentForm });
