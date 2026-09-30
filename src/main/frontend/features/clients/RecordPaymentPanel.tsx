import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { money } from '../../ui/money';

// A client's invoices as the statement endpoint returns them: each with its derived amountDue (its
// amount minus every payment recorded against it), the same shape the statement panel reads.
type StatementInvoice = {
  id: number;
  projectId: number;
  amount: number;
  status: string;
  issuedDate: string;
  dueDate: string;
  amountDue: number;
};
type Statement = { invoices: StatementInvoice[]; outstanding: number; currency: string };

// Record one lump payment split across several of a client's open invoices, rendered in the
// client-detail context. It only shows once the form is opened — the `payment` URL key, owned by
// features/clients/recordPayment.slot.tsx, which this reads (rule 4). The client's open invoices are
// read with useQuery under a key that starts with ['invoices'] (the client statement, scoped to the
// client) so it stays in step with any invoice/payment write; the user's typing (the total, the date
// and each invoice's share) is the only useState (rule 4). Submitting POSTs the split to
// /api/payments/allocate and invalidates ['invoices'] (and ['payments']) so every view of these
// invoices — the project list, the statement, the invoice detail — refreshes from the derived balances
// (rule 5), never hand-maintained.
export function RecordPaymentPanel({ clientId }: { clientId: number }) {
  const [payment] = useSearchParam('payment', asString);
  const open = payment === 'open';
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['invoices', 'statement', clientId],
    queryFn: () => getJson<Statement>(`/api/clients/statement?clientId=${clientId}`),
    enabled: open,
  });

  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  // The share the user is typing against each invoice, keyed by invoice id (uncommitted input, so
  // useState — rule 4).
  const [alloc, setAlloc] = useState<Record<number, string>>({});

  const create = useMutation({
    mutationFn: () =>
      postJson('/api/payments/allocate', {
        amount: Number(amount),
        date,
        allocations: Object.entries(alloc)
          .map(([invoiceId, share]) => ({ invoiceId: Number(invoiceId), amount: Number(share) }))
          .filter((a) => a.amount > 0),
      }),
    onSuccess: () => {
      setAmount('');
      setDate('');
      setAlloc({});
      // The split changes what each invoice still owes, so its derived status and amount-due (read
      // from ['invoices']) and the payments lists (['payments']) must refresh — share the keys, no
      // import between features.
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
      void queryClient.invalidateQueries({ queryKey: ['payments'] });
    },
  });

  if (!open) {
    return null;
  }

  // Only invoices with a balance left can take a share of the payment.
  const invoices = (query.data?.invoices ?? []).filter((i) => Number(i.amountDue) > 0);
  // The whole client is billed in one currency, so the amounts due show in it.
  const currency = query.data?.currency ?? 'USD';

  return (
    <section data-testid="client-record-payment-form">
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
          value={date}
          onChange={(e) => setDate(e.target.value)}
        />

        <table data-testid="payment-alloc-table">
          <thead>
            <tr>
              <th>Invoice</th>
              <th>Amount due</th>
              <th>Allocate</th>
            </tr>
          </thead>
          <tbody>
            {invoices.map((i) => (
              <tr key={i.id} data-testid={`payment-alloc-row-${i.id}`}>
                <td data-testid="payment-alloc-invoice">{i.id}</td>
                <td data-testid="payment-alloc-due">{money(Number(i.amountDue), currency)}</td>
                <td>
                  <input
                    data-testid={`payment-alloc-${i.id}`}
                    type="number"
                    step="0.01"
                    placeholder="0"
                    value={alloc[i.id] ?? ''}
                    onChange={(e) =>
                      setAlloc((prev) => ({ ...prev, [i.id]: e.target.value }))
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
      </form>
    </section>
  );
}
