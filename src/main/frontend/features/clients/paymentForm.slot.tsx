import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { clientStatementKey, getClientStatement, type StatementInvoice } from './statementApi';
import { createClientPayment } from './clientPaymentApi';
import { formatMoney } from '../../ui/money';

// Split one lump payment across several of the client's open invoices — one panel filling the
// client.detail region, shown only when the `payment` URL flag is on (owned by the record-payment
// control), so the two stay in step through the shared search param. The open invoices come from the
// client's statement under the SHARED key ['clients', clientId, 'statement'] (the server derives
// each balance); what the user is currently typing (the total, the date, each allocation) lives in
// useState (uncommitted input). On success we invalidate the shared keys so the statement, the
// project invoices and each invoice's balance refetch — we never hand-maintain any of them.
function ClientPaymentForm({ clientId }: { clientId: number }) {
  const [open] = useSearchParam('payment', asFlag);
  const queryClient = useQueryClient();
  const { data: statement } = useQuery({
    queryKey: clientStatementKey(clientId),
    queryFn: () => getClientStatement(clientId),
    enabled: open,
  });

  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [allocations, setAllocations] = useState<Record<number, string>>({});
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: createClientPayment,
    onSuccess: () => {
      setAmount('');
      setDate('');
      setAllocations({});
      setError('');
      // Everything showing these figures shares a key, so one invalidate per key refreshes them all
      // with no import between panels: the client statement, each project's invoices and totals, and
      // each invoice's own balance and payments, plus the home dashboard figures.
      void queryClient.invalidateQueries({ queryKey: ['clients'] });
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
      void queryClient.invalidateQueries({ queryKey: ['invoice-due'] });
      void queryClient.invalidateQueries({ queryKey: ['invoice-payments'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });

  if (!open) {
    return null;
  }

  // Only invoices that still owe something can take a share — a settled or voided invoice is not
  // part of a split. Shown in the statement's id order so each row's allocation input is stable.
  const openInvoices = (statement?.invoices ?? []).filter(
    (invoice: StatementInvoice) => invoice.status !== 'VOID' && Number(invoice.amountDue) > 0,
  );

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
    // The slices the user actually filled in — an invoice left blank takes no share.
    const slices = openInvoices
      .map((invoice: StatementInvoice) => ({
        invoiceId: invoice.id,
        raw: (allocations[invoice.id] ?? '').trim(),
      }))
      .filter((slice) => slice.raw !== '')
      .map((slice) => ({ invoiceId: slice.invoiceId, amount: Number(slice.raw) }));
    if (slices.length === 0 || slices.some((s) => Number.isNaN(s.amount) || s.amount <= 0)) {
      setError('Allocate the payment across at least one invoice');
      return;
    }
    // The slices must add up to the lump sum, so the whole payment is accounted for (money scale).
    const allocated = slices.reduce((sum, slice) => sum + slice.amount, 0);
    if (Math.round(allocated * 100) !== Math.round(amountValue * 100)) {
      setError('Allocations must add up to the payment amount');
      return;
    }
    setError('');
    mutation.mutate({ clientId, date: date.trim(), amount: amountValue, allocations: slices });
  };

  return (
    <form data-testid="client-payment-form" onSubmit={submit}>
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
      {openInvoices.map((invoice: StatementInvoice) => (
        <label key={invoice.id}>
          Invoice {invoice.id} ({formatMoney(invoice.amountDue)} due)
          <input
            data-testid={`payment-alloc-${invoice.id}`}
            placeholder="Allocation"
            value={allocations[invoice.id] ?? ''}
            onChange={(e) =>
              setAllocations((prev) => ({ ...prev, [invoice.id]: e.target.value }))
            }
          />
        </label>
      ))}
      <button data-testid="payment-form-submit" type="submit">
        Record payment
      </button>
      {error && <p data-testid="payment-form-error">{error}</p>}
    </form>
  );
}

export const contribution = ClientDetail.fill({ order: 35, Component: ClientPaymentForm });
