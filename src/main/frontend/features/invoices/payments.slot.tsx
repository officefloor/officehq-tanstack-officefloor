import { useQuery } from '@tanstack/react-query';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { paymentsKey, listPayments, type Payment } from './paymentsApi';
import { formatMoney } from '../../ui/money';

// What a client has paid against this invoice — one panel filling the invoice.detail region. Reads
// server data under ['invoice-payments', invoiceId] (never copied into state); the record form shares
// the key, so a successful record refreshes this with no import between them. Amounts render with two
// decimals; the date is shown as the ISO day it was paid.
function InvoicePayments({ invoiceId }: { invoiceId: number }) {
  const { data: payments } = useQuery({
    queryKey: paymentsKey(invoiceId),
    queryFn: () => listPayments(invoiceId),
  });

  if (!payments) {
    return null;
  }

  return (
    <table data-testid="invoice-payments-table">
      <thead>
        <tr>
          <th>Amount</th>
          <th>Date</th>
        </tr>
      </thead>
      <tbody>
        {payments.map((payment: Payment) => (
          <tr key={payment.id} data-testid={`payment-row-${payment.id}`}>
            <td data-testid="payment-amount">{formatMoney(payment.amount)}</td>
            <td data-testid="payment-date">{payment.date}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export const contribution = InvoiceDetail.fill({ order: 30, Component: InvoicePayments });
