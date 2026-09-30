import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { createClientPayment, type PaymentAllocation } from './queries';
import { formatMoney } from '../../ui/money';

// The client's open invoices, read under the SAME key the client statement uses (CLAUDE.md rule 5 —
// features share a KEY by value, not by importing each other). An invoice still owing money (due > 0)
// is one the lump payment can be split across.
type StatementInvoice = { id: number; amount: number; status: string; due: number };
type ClientStatement = { invoices: StatementInvoice[]; totalOwed: number };

// Record ONE lump payment from a client and split it across several of their open invoices. The user
// enters the total, the date, and how much of it goes to each invoice; the write posts all of that
// and invalidates the shared keys so every view of these invoices refreshes itself (rule 5). useState
// holds only what the user is currently typing (rule 4). Carries the stable data-testid contract.
export function ClientPaymentForm({ clientId }: { clientId: number }) {
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [allocations, setAllocations] = useState<Record<number, string>>({});
  const [error, setError] = useState('');
  const queryClient = useQueryClient();

  const { data: statement } = useQuery({
    queryKey: ['clients', clientId, 'statement'],
    queryFn: () => getJson<ClientStatement>(`/api/clients/${clientId}/statement`),
  });

  const mutation = useMutation({
    mutationFn: (input: { amount: number; date: string; allocations: PaymentAllocation[] }) =>
      createClientPayment(clientId, input),
    onSuccess: () => {
      // Refresh everything that shows these invoices or the client's balances: the statement and
      // outstanding total (under ['clients']), each project's invoice list (['invoices', …]) and any
      // invoice's payments (['payments', …]).
      void queryClient.invalidateQueries({ queryKey: ['clients'] });
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
      void queryClient.invalidateQueries({ queryKey: ['payments'] });
      setAmount('');
      setDate('');
      setAllocations({});
    },
  });

  if (!statement) {
    return null;
  }

  const openInvoices = statement.invoices.filter((invoice) => invoice.due > 0);

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
        const split: PaymentAllocation[] = [];
        for (const invoice of openInvoices) {
          const share = Number(allocations[invoice.id]);
          if (!Number.isNaN(share) && share > 0) {
            split.push({ invoiceId: invoice.id, amount: share });
          }
        }
        if (split.length === 0) {
          setError('Allocate the payment to at least one invoice.');
          return;
        }
        setError('');
        mutation.mutate({ amount: paid, date: date.trim(), allocations: split });
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
      <table data-testid="payment-alloc-table">
        <tbody>
          {openInvoices.map((invoice) => (
            <tr key={invoice.id} data-testid={`payment-alloc-row-${invoice.id}`}>
              <td data-testid="payment-alloc-invoice">{`Invoice ${invoice.id}`}</td>
              <td data-testid="payment-alloc-due">{formatMoney(invoice.due)}</td>
              <td>
                <input
                  data-testid={`payment-alloc-${invoice.id}`}
                  placeholder="Amount to apply"
                  value={allocations[invoice.id] ?? ''}
                  onChange={(event) =>
                    setAllocations((prev) => ({ ...prev, [invoice.id]: event.target.value }))
                  }
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <button data-testid="payment-form-submit" type="submit">
        Record payment
      </button>
      {error !== '' && <p data-testid="payment-form-error">{error}</p>}
    </form>
  );
}
