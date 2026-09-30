import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { paymentsKey, createPayment } from './queries';

// Record a payment on an invoice: how much the client paid and when. useState holds only what the
// user is currently entering (CLAUDE.md rule 4); the write is a mutation that invalidates
// ['payments', invoiceId] so the list refreshes itself (rule 5). Carries the stable data-testid
// contract the test reads.
export function PaymentForm({ invoiceId }: { invoiceId: number }) {
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (input: { amount: number; date: string }) => createPayment(invoiceId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: paymentsKey(invoiceId) });
      setAmount('');
      setDate('');
    },
  });

  return (
    <form
      data-testid="payment-form"
      onSubmit={(event) => {
        event.preventDefault();
        const paid = Number(amount);
        if (Number.isNaN(paid) || paid <= 0 || date.trim() === '') {
          setError('Enter an amount above zero and a date.');
          return;
        }
        setError('');
        mutation.mutate({ amount: paid, date: date.trim() });
      }}
    >
      <input
        data-testid="payment-form-amount"
        placeholder="Amount"
        value={amount}
        onChange={(event) => setAmount(event.target.value)}
      />
      <input
        data-testid="payment-form-date"
        placeholder="Date"
        value={date}
        onChange={(event) => setDate(event.target.value)}
      />
      <button data-testid="payment-form-submit" type="submit">
        Record payment
      </button>
      {error !== '' && <p data-testid="payment-form-error">{error}</p>}
    </form>
  );
}
