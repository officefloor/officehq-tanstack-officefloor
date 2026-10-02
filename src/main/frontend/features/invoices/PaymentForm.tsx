import { useState } from 'react';
import { useAddPayment } from './payments';

// Record-a-payment form. useState holds only what the user is currently typing (the amount and the
// date) and the validation error for the current attempt. A payment needs a positive amount and a
// date — otherwise a visible error is shown and no row is added (the server + DB enforce the same).
// On a valid submit it POSTs the payment and invalidates ['payments', invoiceId]; the list refreshes
// from the server — nothing is hand-maintained here.
export function PaymentForm({ invoiceId }: { invoiceId: number }) {
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const add = useAddPayment(invoiceId);

  return (
    <form
      data-testid="payment-form"
      onSubmit={(event) => {
        event.preventDefault();
        const amountValue = Number(amount);
        if (!amount.trim() || Number.isNaN(amountValue) || amountValue <= 0) {
          setError('Amount must be more than zero');
          return;
        }
        if (!date.trim()) {
          setError('A date is required');
          return;
        }
        setError('');
        add.mutate(
          { amount: amountValue, date: date.trim() },
          {
            onSuccess: () => {
              setAmount('');
              setDate('');
            },
          },
        );
      }}
    >
      <input
        data-testid="payment-form-amount"
        placeholder="Amount"
        value={amount}
        onChange={(event) => {
          setAmount(event.target.value);
          setError('');
        }}
      />
      <input
        data-testid="payment-form-date"
        type="date"
        placeholder="Date"
        value={date}
        onChange={(event) => {
          setDate(event.target.value);
          setError('');
        }}
      />
      <button data-testid="payment-form-submit" type="submit">
        Record payment
      </button>
      {error && (
        <p data-testid="payment-form-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}
