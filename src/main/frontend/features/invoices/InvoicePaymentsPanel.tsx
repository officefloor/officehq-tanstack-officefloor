import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { money } from '../../ui/money';

// A payment the server returns: how much (amount) and on what day (date, an ISO YYYY-MM-DD string),
// plus the id of the invoice it was made against so a panel can show just its own.
export type Payment = {
  id: number;
  invoiceId: number;
  amount: number;
  date: string;
};

// The payments a client has recorded against an invoice, rendered in the invoice-detail context: the
// list of that invoice's payments (amount and date) and a form to record another. Server data is read
// with useQuery under the ['payments'] key and filtered to the invoice this panel is handed — never
// copied into state, never hand-maintained (rule 5). Recording a payment is a useMutation that
// invalidates ['payments'] so this list refetches. The only useState is the add form (rule 4).
export function InvoicePaymentsPanel({ invoiceId }: { invoiceId: number }) {
  const queryClient = useQueryClient();
  const payments = useQuery({
    queryKey: ['payments'],
    queryFn: () => getJson<Payment[]>('/api/payments'),
  });

  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');

  const create = useMutation({
    mutationFn: () =>
      postJson<Payment>('/api/payments', {
        invoiceId,
        amount: Number(amount),
        date,
      }),
    onSuccess: () => {
      setAmount('');
      setDate('');
      void queryClient.invalidateQueries({ queryKey: ['payments'] });
    },
  });

  const rows = (payments.data ?? []).filter((p) => p.invoiceId === invoiceId);

  return (
    <section data-testid="invoice-payments">
      <form
        data-testid="payment-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!amount.trim() || !date.trim()) {
            return;
          }
          create.mutate();
        }}
      >
        <input
          data-testid="payment-form-amount"
          type="number"
          step="0.01"
          placeholder="Amount"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
        />
        <input
          data-testid="payment-form-date"
          type="date"
          placeholder="Date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
        />
        <button data-testid="payment-form-submit" type="submit">
          Record payment
        </button>
      </form>

      <table data-testid="invoice-payments-table">
        <thead>
          <tr>
            <th>Amount</th>
            <th>Date</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.id} data-testid={`payment-row-${p.id}`}>
              <td data-testid="payment-amount">{money(Number(p.amount))}</td>
              <td data-testid="payment-date">{p.date}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
