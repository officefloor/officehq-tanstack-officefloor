import { useState } from 'react';
import { formatMoney } from '../../ui/money';
import { useClientStatement } from './statement';
import { useSplitPayment } from './splitPayment';

// Record a lump-sum payment and split it across the client's open invoices. The client's open
// invoices come straight from its statement (the shared ['clients', id, 'statement'] query, so no
// parent hands this form data) — each payable invoice (SENT or part paid) gets its own allocation
// field. useState holds only what the user is currently typing: the lump amount, the date, and the
// per-invoice shares. On submit it posts the split and the shared query keys refresh every balance
// and status; nothing is hand-maintained here.
export function ClientPaymentForm({ clientId }: { clientId: number }) {
  const { data } = useClientStatement(clientId);
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [shares, setShares] = useState<Record<number, string>>({});
  const [error, setError] = useState('');
  const split = useSplitPayment();

  if (!data) {
    return null;
  }

  // The invoices a payment can go against: sent and not yet fully paid.
  const open = data.invoices.filter(
    (invoice) => invoice.status === 'SENT' || invoice.status === 'PARTIAL',
  );

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
        // Each filled share becomes an allocation against its invoice; blank fields are skipped.
        const allocations = open
          .map((invoice) => ({ invoiceId: invoice.id, amount: Number(shares[invoice.id]) }))
          .filter((a) => shares[a.invoiceId]?.trim() && !Number.isNaN(a.amount) && a.amount > 0);
        if (allocations.length === 0) {
          setError('Allocate the payment to at least one invoice');
          return;
        }
        const allocated = allocations.reduce((sum, a) => sum + a.amount, 0);
        if (allocated !== amountValue) {
          setError('The allocations must add up to the payment amount');
          return;
        }
        setError('');
        split.mutate(
          { amount: amountValue, date: date.trim(), allocations },
          {
            onSuccess: () => {
              setAmount('');
              setDate('');
              setShares({});
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
      <table data-testid="payment-allocations">
        <tbody>
          {open.map((invoice) => (
            <tr key={invoice.id} data-testid={`payment-alloc-row-${invoice.id}`}>
              <td>Invoice {invoice.id}</td>
              <td data-testid="payment-alloc-due">{formatMoney(invoice.due)}</td>
              <td>
                <input
                  data-testid={`payment-alloc-${invoice.id}`}
                  placeholder="0.00"
                  value={shares[invoice.id] ?? ''}
                  onChange={(event) => {
                    const value = event.target.value;
                    setShares((prev) => ({ ...prev, [invoice.id]: value }));
                    setError('');
                  }}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <button data-testid="payment-form-submit" type="submit" disabled={split.isPending}>
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
